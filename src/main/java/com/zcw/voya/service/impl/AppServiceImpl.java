package com.zcw.voya.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.RandomUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.zcw.voya.ai.model.enums.CodeGenTypeEnum;
import com.zcw.voya.constant.AppConstant;
import com.zcw.voya.constant.UserConstant;
import com.zcw.voya.core.AiCodeGeneratorFacade;
import com.zcw.voya.core.handler.StreamHandlerExecutor;
import com.zcw.voya.exception.BusinessException;
import com.zcw.voya.exception.ErrorCode;
import com.zcw.voya.exception.ThrowUtils;
import com.zcw.voya.model.dto.app.AppAddRequest;
import com.zcw.voya.model.dto.app.AppQueryRequest;
import com.zcw.voya.model.dto.app.AppUpdateRequest;
import com.zcw.voya.model.entity.App;
import com.zcw.voya.model.entity.User;
import com.zcw.voya.model.enums.ChatHistoryMessageTypeEnum;
import com.zcw.voya.model.vo.AppVO;
import com.zcw.voya.mapper.AppMapper;
import com.zcw.voya.model.vo.UserVO;
import com.zcw.voya.service.AppService;
import com.zcw.voya.service.ChatHistoryService;
import com.zcw.voya.service.UserService;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现
 *
 * @author zcw
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    private UserService userService;
    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;
    @Resource
    private ChatHistoryService chatHistoryService;
    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    @Override
    public long createApp(AppAddRequest appAddRequest, HttpServletRequest request) {
        // 校验参数
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        String initPrompt = appAddRequest.getInitPrompt();
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 创建应用
        App app = App.builder()
                // 应用名称是提示词前12位
                .appName(initPrompt.substring(0, Math.min(initPrompt.length(), 12)))
                .initPrompt(initPrompt)
                .userId(loginUser.getId())
                // TODO 暂时设置多文件生成
                .codeGenType(CodeGenTypeEnum.MULTI_FILE.getValue())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .priority(0)
                .build();
        boolean saveResult = save(app);
        ThrowUtils.throwIf(!saveResult, ErrorCode.OPERATION_ERROR, "创建应用失败");
        return app.getId();
    }

    @Override
    public boolean updateApp(AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        // 校验参数
        ThrowUtils.throwIf(appUpdateRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        Long id = appUpdateRequest.getId();
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR, "应用id无效");
        String appName = appUpdateRequest.getAppName();
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 获取应用信息
        App app = getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 检查权限：非管理员只能修改自己的应用
        if (!UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole()) && !app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限修改该应用");
        }
        // 更新应用
        App updateApp = new App();
        updateApp.setId(id);
        updateApp.setAppName(appName);
        updateApp.setUpdateTime(LocalDateTime.now());
        // 管理员可以更新更多字段
        if (UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            updateApp.setCover(appUpdateRequest.getCover());
            updateApp.setPriority(appUpdateRequest.getPriority());
            updateApp.setInitPrompt(appUpdateRequest.getInitPrompt());
            updateApp.setCodeGenType(appUpdateRequest.getCodeGenType());
            updateApp.setDeployKey(appUpdateRequest.getDeployKey());
        }
        return updateById(updateApp);
    }

    @Override
    public boolean deleteApp(long id, HttpServletRequest request) {
        // 校验参数
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR, "应用id无效");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 获取应用信息
        App app = getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 检查权限：非管理员只能删除自己的应用
        if (!UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole()) && !app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限删除该应用");
        }
        return this.removeById(id);
    }

    @Override
    public App getAppById(long id, HttpServletRequest request) {
        // 校验参数
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR, "应用id无效");

        // 获取应用信息
        App app = getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        // 获取当前登录用户
        User loginUser = null;
        try {
            loginUser = userService.getLoginUser(request);
        } catch (Exception e) {
            // 未登录用户只能查看公开信息
        }
        // 检查权限：非管理员只能查看自己的应用
        if (loginUser != null && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole()) && !app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限查看该应用");
        }

        return app;
    }

    @Override
    public Page<AppVO> listAppVOByPage(AppQueryRequest appQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 非管理员只能查询自己的应用
        if (!UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            appQueryRequest.setUserId(loginUser.getId());
        }
        // 管理员查询无限制，但普通用户最多每页20条
        long pageNum = appQueryRequest.getCurrent();
        long pageSize = appQueryRequest.getPageSize();
        if (!UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole()) && pageSize > 20) {
            pageSize = 20;
        }
        Page<App> appPage = page(Page.of(pageNum, pageSize), getQueryWrapper(appQueryRequest));
        return getPageVo(appPage);
    }

    @Override
    public Page<AppVO> listFeaturedAppVOByPage(AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        // 最多每页20条
        long pageNum = appQueryRequest.getCurrent();
        long pageSize = appQueryRequest.getPageSize();
        if (pageSize > 20) {
            pageSize = 20;
        }
        // 构建查询条件，只查询priority > 0的应用作为精选应用
        QueryWrapper queryWrapper = getQueryWrapper(appQueryRequest)
                .gt("priority", 0);
        Page<App> appPage = page(Page.of(pageNum, pageSize), queryWrapper);
        return getPageVo(appPage);
    }

    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
        // 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 验证权限
        if (!UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole()) && !app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }
        // 获取代码生成类型
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.PARAMS_ERROR, "不支持的代码生成类型");
        // 保存用户消息
        boolean saved = chatHistoryService.addChatHistory(appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "保存用户消息失败");
        // 调用ai生成代码
        Flux<String> flux = aiCodeGeneratorFacade.generateCodeStream(message, codeGenTypeEnum, appId);
        // 收集AI响应的内容,并保存对话记录
        return streamHandlerExecutor.doExecute(flux, chatHistoryService, appId, loginUser, codeGenTypeEnum);
    }

    /**
     * 删除应用时关联删除对话历史
     *
     * @param id 应用ID
     * @return 是否成功
     */
    @Override
    public boolean removeById(Serializable id) {
        if (id == null) {
            return false;
        }
        // 转换为 Long 类型
        long appId = Long.parseLong(id.toString());
        if (appId <= 0) {
            return false;
        }
        // 先删除关联的对话历史
        try {
            chatHistoryService.deleteById(appId);
        } catch (Exception e) {
            // 记录日志但不阻止应用删除
            log.error("删除应用关联对话历史失败: {}", e.getMessage());
        }
        // 删除应用
        return super.removeById(id);
    }


    @Override
    public String appDeploy(Long appId, User loginUser) {
        // 权限检查
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");
        // 是否已有部署key
        String deployKey = app.getDeployKey();
        if (StringUtils.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 检查目录是否存在
        File file = new File(sourceDirPath);
        if (!file.exists() || !file.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码不存在，请先生成");
        }
        File distFile = new File(AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey);
        try {
            FileUtil.copyContent(file, distFile, true);
        } catch (IORuntimeException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, e.getMessage());
        }
        // 更新部署信息
        App updateApp = new App();
        updateApp.setId(app.getId());
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updated = this.updateById(updateApp);
        ThrowUtils.throwIf(!updated, ErrorCode.SYSTEM_ERROR, "更新应用部署信息失败");
        // 返回可访问的URL
        return String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
    }

    @Override
    public Page<AppVO> getMyAppVoList(HttpServletRequest request, AppQueryRequest appQueryRequest) {
        User loginUser = userService.getLoginUser(request);
        QueryWrapper queryWrapper = new QueryWrapper().eq(App::getUserId, loginUser.getId());
        Page<App> page = this.page(new Page<App>(appQueryRequest.getCurrent(), appQueryRequest.getPageSize()), queryWrapper);
        return getPageVo(page);
    }

    // region 工具方法
    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        appVO.setUser(userService.getUserVO(userService.getById(app.getUserId())));
        return appVO;
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (appList == null || appList.isEmpty()) {
            return new ArrayList<>();
        }
        List<AppVO> appVOList = appList.stream().map(this::getAppVO).collect(Collectors.toList());
        // 关系用户映射
        List<Long> userIdList = appList.stream().map(App::getUserId).distinct().toList();
        Map<Long, List<User>> userMap = userService.listByIds(userIdList).stream().collect(Collectors.groupingBy(User::getId));
        // 补充信息
        appVOList.forEach(appVO -> {
            UserVO userVO = userService.getUserVO(userMap.get(appVO.getUserId()).getFirst());
            // 关联用户信息
            appVO.setUser(userVO);
        });
        return appVOList;
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        Long userId = appQueryRequest.getUserId();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();

        return QueryWrapper.create()
                .eq("id", id, id != null)
                .eq("userId", userId, userId != null)
                .eq("codeGenType", codeGenType, codeGenType != null)
                .eq("deployKey", deployKey, deployKey != null)
                .eq("priority", priority, priority != null)
                .like("appName", appName, appName != null)
                .orderBy("updateTime", false);
    }

    private Page<AppVO> getPageVo(Page<App> appPage) {
        Page<AppVO> voPage = new Page<>(
                appPage.getPageNumber(),
                appPage.getPageSize(),
                appPage.getTotalRow()
        );
        voPage.setRecords(getAppVOList(appPage.getRecords()));
        return voPage;
    }
    // endregion
}
package com.zcw.voya.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.zcw.voya.ai.model.enums.CodeGenTypeEnum;
import com.zcw.voya.constant.UserConstant;
import com.zcw.voya.exception.BusinessException;
import com.zcw.voya.exception.ErrorCode;
import com.zcw.voya.exception.ThrowUtils;
import com.zcw.voya.model.dto.app.AppAddRequest;
import com.zcw.voya.model.dto.app.AppQueryRequest;
import com.zcw.voya.model.dto.app.AppUpdateRequest;
import com.zcw.voya.model.entity.App;
import com.zcw.voya.model.entity.User;
import com.zcw.voya.model.vo.AppVO;
import com.zcw.voya.mapper.AppMapper;
import com.zcw.voya.service.AppService;
import com.zcw.voya.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现
 *
 * @author zcw
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper,App> implements AppService {

    @Resource
    private UserService userService;

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
                .appName(initPrompt.substring(0,Math.min(initPrompt.length(), 12)))
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

    // region 工具方法
    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        return appVO;
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (appList == null || appList.isEmpty()) {
            return new ArrayList<>();
        }
        return appList.stream().map(this::getAppVO).collect(Collectors.toList());
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
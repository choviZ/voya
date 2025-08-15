package com.zcw.voya.controller;

import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.zcw.voya.annotation.AuthCheck;
import com.zcw.voya.common.BaseResponse;
import com.zcw.voya.common.DeleteRequest;
import com.zcw.voya.common.ResultUtils;
import com.zcw.voya.constant.UserConstant;
import com.zcw.voya.exception.ErrorCode;
import com.zcw.voya.exception.ThrowUtils;
import com.zcw.voya.model.dto.app.AppAddRequest;
import com.zcw.voya.model.dto.app.AppDeployRequest;
import com.zcw.voya.model.dto.app.AppQueryRequest;
import com.zcw.voya.model.dto.app.AppUpdateRequest;
import com.zcw.voya.model.entity.App;
import com.zcw.voya.model.entity.User;
import com.zcw.voya.model.vo.AppVO;
import com.zcw.voya.service.AppService;
import com.zcw.voya.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 应用 控制层
 *
 * @author zcw
 */
@RestController
@RequestMapping("/app")
public class AppController {

    @Resource
    private AppService appService;
    @Resource
    private UserService userService;

    /**
     * 应用部署
     * @param appDeployRequest 应用部署请求
     * @return 可访问的地址
     */
    @PostMapping("/deploy")
    public BaseResponse<String> appDeploy(@RequestBody @Valid AppDeployRequest appDeployRequest, HttpServletRequest httpRequest){
        ThrowUtils.throwIf(appDeployRequest == null,ErrorCode.PARAMS_ERROR);
        Long appId = appDeployRequest.getAppId();
        User loginUser = userService.getLoginUser(httpRequest);
        String deployUrl = appService.appDeploy(appId, loginUser);
        return ResultUtils.success(deployUrl);
    }

    /**
     * 聊天生成代码（SSE）
     *
     * @param appId   应用id
     * @param message 初始prompt
     * @param request 请求
     * @return SSE流
     */
    @GetMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatToGenCode(@RequestParam Long appId, @RequestParam String message, HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用id无效");
        ThrowUtils.throwIf(message == null || message.isEmpty(), ErrorCode.PARAMS_ERROR, "初始prompt不能为空");
        User loginUser = userService.getLoginUser(request);
        Flux<String> flux = appService.chatToGenCode(appId, message, loginUser);
        return flux
                .map(chunk -> {
                    // 包装成json
                    Map<String, String> map = Map.of("d", chunk);
                    String jsonStr = JSONUtil.toJsonStr(map);
                    return ServerSentEvent.<String>builder()
                            .data(jsonStr)
                            .build();
                }).concatWith(Mono.just(
                        // 发送结束事件
                        ServerSentEvent.<String>builder()
                                .data("")
                                .event("done")
                                .build()
                ));
    }

    /**
     * 创建应用
     *
     * @param appAddRequest 创建应用请求
     * @param request       请求
     * @return 应用id
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@Valid @RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        long id = appService.createApp(appAddRequest, request);
        return ResultUtils.success(id);
    }

    /**
     * 更新应用
     *
     * @param appUpdateRequest 更新应用请求
     * @param request          请求
     * @return 是否成功
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateApp(@Valid @RequestBody AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        boolean result = appService.updateApp(appUpdateRequest, request);
        return ResultUtils.success(result);
    }

    /**
     * 删除应用
     *
     * @param deleteRequest 删除请求
     * @param request       请求
     * @return 是否成功
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@Valid @RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR, "应用id无效");
        boolean result = appService.deleteApp(deleteRequest.getId(), request);
        return ResultUtils.success(result);
    }

    /**
     * 获取应用详情VO
     *
     * @param id      应用id
     * @param request 请求
     * @return 应用详情VO
     */
    @GetMapping("/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AppVO> getAppVOById(long id, HttpServletRequest request) {
        App app = appService.getAppById(id, request);
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 分页查询应用列表
     *
     * @param appQueryRequest 查询请求
     * @param request         请求
     * @return 应用VO分页列表
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<AppVO>> listAppVOByPage(@Valid @RequestBody AppQueryRequest appQueryRequest, HttpServletRequest request) {
        Page<AppVO> appVOPage = appService.listAppVOByPage(appQueryRequest, request);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 分页查询精选应用列表
     *
     * @param appQueryRequest 查询请求
     * @return 精选应用VO分页列表
     */
    @PostMapping("/list/featured/page/vo")
    public BaseResponse<Page<AppVO>> listFeaturedAppVOByPage(@Valid @RequestBody AppQueryRequest appQueryRequest) {
        Page<AppVO> appVOPage = appService.listFeaturedAppVOByPage(appQueryRequest);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 获取应用详情
     *
     * @param id      应用id
     * @param request 请求
     * @return 应用详情
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<App> getAppById(long id, HttpServletRequest request) {
        App app = appService.getAppById(id, request);
        return ResultUtils.success(app);
    }

    /**
     * 管理员删除任意应用
     *
     * @param deleteRequest 删除请求
     * @return 是否成功
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> adminDeleteApp(@Valid @RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR, "应用id无效");
        boolean result = appService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 管理员更新任意应用
     *
     * @param appUpdateRequest 更新应用请求
     * @return 是否成功
     */
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> adminUpdateApp(@Valid @RequestBody AppUpdateRequest appUpdateRequest) {
        App app = new App();
        app.setId(appUpdateRequest.getId());
        app.setAppName(appUpdateRequest.getAppName());
        app.setCover(appUpdateRequest.getCover());
        app.setPriority(appUpdateRequest.getPriority());
        app.setInitPrompt(appUpdateRequest.getInitPrompt());
        app.setCodeGenType(appUpdateRequest.getCodeGenType());
        app.setDeployKey(appUpdateRequest.getDeployKey());
        boolean result = appService.updateById(app);
        return ResultUtils.success(result);
    }

    /**
     * 管理员分页查询应用列表
     *
     * @param appQueryRequest 查询请求
     * @return 应用VO分页列表
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> adminListAppVOByPage(@Valid @RequestBody AppQueryRequest appQueryRequest) {
        long pageNum = appQueryRequest.getCurrent();
        long pageSize = appQueryRequest.getPageSize();
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), appService.getQueryWrapper(appQueryRequest));
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        appVOPage.setRecords(appService.getAppVOList(appPage.getRecords()));
        return ResultUtils.success(appVOPage);
    }
}
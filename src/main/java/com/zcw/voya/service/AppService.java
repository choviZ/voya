package com.zcw.voya.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.zcw.voya.model.dto.app.AppAddRequest;
import com.zcw.voya.model.dto.app.AppQueryRequest;
import com.zcw.voya.model.dto.app.AppUpdateRequest;
import com.zcw.voya.model.entity.App;
import com.zcw.voya.model.entity.User;
import com.zcw.voya.model.vo.AppVO;
import jakarta.servlet.http.HttpServletRequest;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层
 *
 * @author zcw
 */
public interface AppService extends IService<App> {

    /**
     * 创建应用
     *
     * @param appAddRequest 创建应用请求
     * @param request 请求
     * @return 应用id
     */
    long createApp(AppAddRequest appAddRequest, HttpServletRequest request);

    /**
     * 异步生成应用截图
     * @param appId 应用id
     * @param appUrl 应用部署url
     */
    void generateAppScreenshotAsync(Long appId, String appUrl);

    /**
     * 更新应用
     *
     * @param appUpdateRequest 更新应用请求
     * @param request 请求
     * @return 是否成功
     */
    boolean updateApp(AppUpdateRequest appUpdateRequest, HttpServletRequest request);

    /**
     * 删除应用
     *
     * @param id 应用id
     * @param request 请求
     * @return 是否成功
     */
    boolean deleteApp(long id, HttpServletRequest request);

    /**
     * 获取应用详情
     *
     * @param id 应用id
     * @param request 请求
     * @return 应用详情
     */
    App getAppById(long id, HttpServletRequest request);

    /**
     * 获取我的应用列表（用户）
     * @param request 请求
     * @param appQueryRequest 查询请求
     * @return
     */
    Page<AppVO> getMyAppVoList(HttpServletRequest request, AppQueryRequest appQueryRequest);

    /**
     * 获取应用VO
     *
     * @param app 应用实体
     * @return 应用VO
     */
    AppVO getAppVO(App app);

    /**
     * 获取应用VO列表
     *
     * @param appList 应用实体列表
     * @return 应用VO列表
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 获取查询条件
     *
     * @param appQueryRequest 查询请求
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 分页查询应用列表（用户）
     *
     * @param appQueryRequest 查询请求
     * @param request 请求
     * @return 应用VO分页列表
     */
    Page<AppVO> listAppVOByPage(AppQueryRequest appQueryRequest, HttpServletRequest request);

    /**
     * 分页查询精选应用列表
     *
     * @param appQueryRequest 查询请求
     * @return 精选应用VO分页列表
     */
    Page<AppVO> listFeaturedAppVOByPage(AppQueryRequest appQueryRequest);

    /**
     * 聊天生成代码
     * @param appId 应用id
     * @param message prompt
     * @param loginUser 登录用户
     * @return 流
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    /**
     * 应用部署
     * @param appId 应用id
     * @param loginUser 登录用户
     * @return URL
     */
    String appDeploy(Long appId, User loginUser);
}
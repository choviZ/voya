package com.zcw.voya.service;

import jakarta.servlet.http.HttpServletResponse;


/**
 * 项目下载服务
 */
public interface ProjectDownloadService {

    /**
     * 下载项目
     * @param projectPath 项目路径
     * @param downloadFileName 下载文件名
     * @param response 响应
     */
    void downLoadProjectAsZpi(String projectPath, String downloadFileName, HttpServletResponse response);
}

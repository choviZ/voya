package com.zcw.voya.service;

/**
 * 截图服务
 */
public interface ScreenShotService {

    /**
     * 保存网页截图并上传
     * @param webUrl 被截图的网页URL
     * @return 可访问的截图地址
     */
    String saveAndUpload(String webUrl);
}

package com.zcw.voya.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.zcw.voya.exception.ErrorCode;
import com.zcw.voya.exception.ThrowUtils;
import com.zcw.voya.manager.CosManager;
import com.zcw.voya.service.ScreenShotService;
import com.zcw.voya.util.WebScreenShotUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class ScreenShotServiceImpl implements ScreenShotService {

    @Resource
    private CosManager cosManager;

    @Override
    public String saveAndUpload(String webUrl) {
        // 校验
        ThrowUtils.throwIf(StrUtil.isBlank(webUrl), ErrorCode.PARAMS_ERROR,"地址不能为空");
        log.info("开始生成网页截图,URL：{}",webUrl);
        // 生成本地截图
        String savePath = WebScreenShotUtils.saveWebScreenShot(webUrl);
        ThrowUtils.throwIf(StrUtil.isBlank(savePath), ErrorCode.SYSTEM_ERROR,"本地截图保存失败");
        // 上传
        try {
            String cosUrl = uploadToCos(savePath);
            ThrowUtils.throwIf(StrUtil.isBlank(cosUrl), ErrorCode.SYSTEM_ERROR,"上传至对象存储失败");
            log.info("网页截图并上传成功：{} -> {}",webUrl,cosUrl);
            return cosUrl;
        } finally {
            // 删除本地截图
            clearLocalImage(savePath);
        }
    }

    /**
     * 清理本地截图文件
     * @param localPath 本地截图文件路径
     */
    private void clearLocalImage(String localPath) {
        File file = new File(localPath);
        if (file.exists()){
            File parentFile = file.getParentFile();
            FileUtil.del(parentFile);
            log.info("本地截图已清理：{}",localPath);
        }
    }

    /**
     * 上传至COS
     * @param localImgPath 本地截图文件路径
     * @return cosUrl
     */
    private String uploadToCos(String localImgPath) {
        File file = new File(localImgPath);
        if (!file.exists()){
            log.error("文件不存在：{}",localImgPath);
            return null;
        }
        // cos key
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = RandomUtil.randomString(6) + ".jpg";
        String key = String.format("/screenShots/%s/%s",datePath,fileName);
        return cosManager.uploadFile(key,file);
    }
}

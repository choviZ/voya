package com.zcw.voya.langgraph4j.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.zcw.voya.langgraph4j.model.ImageResource;
import com.zcw.voya.langgraph4j.model.enums.ImageCategoryEnum;
import com.zcw.voya.manager.CosManager;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * logo 生成工具
 */
@Slf4j
@Component
public class LogoGeneratorTool {

    @Value("${dashscope.api-key:}")
    private String dashScopeApiKey;

    @Value("${dashscope.image-model}")
    private String imageModel;

    @Resource
    private CosManager cosManager;

    @Tool("根据描述生成 Logo 设计图片，用于网站品牌标识")
    public List<ImageResource> generateLogos(@P("Logo 设计描述，如名称、行业、风格等，尽量详细") String description) {
        List<ImageResource> logoList = new ArrayList<>();
        try {
            // 构建 Logo 设计提示词
            String logoPrompt = String.format("生成 Logo，Logo中禁止包含中文！Logo介绍：%s", description);
            ImageSynthesisParam param = ImageSynthesisParam.builder()
                    .apiKey(dashScopeApiKey)
                    .model(imageModel)
                    .prompt(logoPrompt)
                    .size("512*512")
                    .n(1) // 生成 1 张足够，因为 AI 不知道哪张最好
                    .build();
            ImageSynthesis imageSynthesis = new ImageSynthesis();
            ImageSynthesisResult result = imageSynthesis.call(param);
            if (result != null && result.getOutput() != null && result.getOutput().getResults() != null) {
                List<Map<String, String>> results = result.getOutput().getResults();
                for (Map<String, String> imageResult : results) {
                    // 得到一个可访问的url【临时的】
                    String imageUrl = imageResult.get("url");
                    if (StrUtil.isNotBlank(imageUrl)) {
                        logoList.add(ImageResource.builder()
                                .category(ImageCategoryEnum.LOGO)
                                .description(description)
                                .url(imageUrl)
                                .build());
                    }
                }
            }
            // 下载图片
            ImageResource logo = logoList.get(0);
            File tempOutputFile = FileUtil.createTempFile("logo_output_", ".png", true);
            HttpUtil.downloadFile(logo.getUrl(), tempOutputFile);
            log.info("logo保存为临时文件：{}",tempOutputFile.getName());
            // 保存至COS
            String keyName = String.format("/logo/%s/%s", RandomUtil.randomString(5), tempOutputFile.getName());
            String cosUrl = cosManager.uploadFile(keyName, tempOutputFile);
            log.info("上传至cos：{}",cosUrl);
            logo.setUrl(cosUrl);
            // 清理临时文件
            FileUtil.del(tempOutputFile);
        } catch (Exception e) {
            log.error("生成 Logo 失败: {}", e.getMessage(), e);
        }
        return logoList;
    }
}

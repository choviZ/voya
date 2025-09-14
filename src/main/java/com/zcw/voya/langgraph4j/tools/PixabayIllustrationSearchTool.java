package com.zcw.voya.langgraph4j.tools;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.core.util.URLUtil;
import com.zcw.voya.exception.BusinessException;
import com.zcw.voya.langgraph4j.model.ImageResource;
import com.zcw.voya.langgraph4j.model.enums.ImageCategoryEnum;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Pixabay 插画图片搜索工具
 */
@Slf4j
@Component
public class PixabayIllustrationSearchTool {

    // 搜图API基础URL
    private static final String PIXABAY_IMAGE_API_URL = "https://pixabay.com/api/";

    @Value("${pixabay.api-key}")
    private String apiKey;

    @Tool("搜索插画图片，用于网站美化和装饰")
    public List<ImageResource> pixabaySearchIllustrations(@P("搜索关键词") String query) {
        List<ImageResource> imageResources = new ArrayList<>();
        // 构建请求参数
        HttpRequest request = HttpRequest.get(PIXABAY_IMAGE_API_URL)
                .form("key", apiKey)
                .form("q", URLUtil.encode(query))
                .form("per_page", 12)
                .form("image_type", "illustration"); // 只搜索插画类型
        // 发送请求并获取响应
        try (HttpResponse response = request.execute()) {
            // 处理HTTP错误
            int statusCode = response.getStatus();
            if (statusCode != 200) {
                log.error("插画搜索失败：{}", response.body());
            }
            // 解析JSON响应
            JSONObject apiResponse = JSONUtil.parseObj(response.body());
            JSONArray imageHits = apiResponse.getJSONArray("hits");
            // 转换为ImageResource列表
            if (imageHits != null && !imageHits.isEmpty()) {
                for (Object hit : imageHits) {
                    JSONObject image = (JSONObject) hit;
                    ImageResource resource = new ImageResource();
                    resource.setCategory(ImageCategoryEnum.ILLUSTRATION); // 固定为ILLUSTRATION
                    resource.setDescription(image.getStr("tags")); // 使用tags作为描述
                    resource.setUrl(image.getStr("webformatURL")); // 使用中等分辨率图片URL
                    imageResources.add(resource);
                }
            }
        }
        return imageResources;
    }


}
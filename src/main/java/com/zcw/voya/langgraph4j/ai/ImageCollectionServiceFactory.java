package com.zcw.voya.langgraph4j.ai;

import com.zcw.voya.langgraph4j.tools.ImageSearchTool;
import com.zcw.voya.langgraph4j.tools.LogoGeneratorTool;
import com.zcw.voya.langgraph4j.tools.MermaidDiagramTool;
import com.zcw.voya.langgraph4j.tools.PixabayIllustrationSearchTool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * 图片收集服务工厂
 */
@Slf4j
@Component
public class ImageCollectionServiceFactory {

    @Resource
    private ChatModel openAiChatModel;

    @Resource
    private ImageSearchTool imageSearchTool;

    @Resource
    private LogoGeneratorTool logoGeneratorTool;

    @Resource
    private MermaidDiagramTool mermaidDiagramTool;

    @Resource
    private PixabayIllustrationSearchTool pixabayIllustrationSearchTool;

    /**
     * 创建图片收集AI Service实例
     * @return ImageCollectionService
     */
    @Bean
    public ImageCollectionService createImageCollectionService() {
        return AiServices.builder(ImageCollectionService.class)
                .chatModel(openAiChatModel)
                .tools(
                        imageSearchTool,
                        logoGeneratorTool,
                        mermaidDiagramTool,
                        pixabayIllustrationSearchTool
                )
                .build();
    }
}

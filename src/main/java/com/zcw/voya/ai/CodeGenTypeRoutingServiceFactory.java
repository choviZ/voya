package com.zcw.voya.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ai代码生成类型路由服务工厂
 */
@Configuration
@Slf4j
public class CodeGenTypeRoutingServiceFactory {

    @Resource
    private ChatModel openAiChatModel;

    @Bean
    public CodeGenTypeRoutingService getCodeGenTypeRoutingService() {
        return AiServices.builder(CodeGenTypeRoutingService.class)
                .chatModel(openAiChatModel)
                .build();
    }
}

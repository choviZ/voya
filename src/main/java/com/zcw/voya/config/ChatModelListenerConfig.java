package com.zcw.voya.config;

import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 模型配置
 */
@Configuration
@Slf4j
public class ChatModelListenerConfig {

    @Bean
    ChatModelListener chatModelListener() {
        return new ChatModelListener(){
            @Override
            public void onResponse(ChatModelResponseContext responseContext) {
                // 记录每次请求消耗的Token
                ChatResponse chatResponse = responseContext.chatResponse();
                TokenUsage tokenUsage = chatResponse.tokenUsage();
                log.info("输入的token：{}", tokenUsage.inputTokenCount());
                log.info("输出的token：{}", tokenUsage.outputTokenCount());
                log.info("总token：{}", tokenUsage.totalTokenCount());
            }
        };
    }
}

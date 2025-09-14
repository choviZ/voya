package com.zcw.voya.ai;

import com.zcw.voya.util.SpringContextUtil;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ai代码生成类型路由服务工厂
 */
@Configuration
@Slf4j
public class CodeGenTypeRoutingServiceFactory {

    /**
     * 创建代码生成类型路由服务
     * @return CodeGenTypeRoutingService
     */
    public CodeGenTypeRoutingService createCodeGenTypeRoutingService() {
        ChatModel routingChatModelPrototype = SpringContextUtil.getBean("routingChatModelPrototype", ChatModel.class);
        return AiServices.builder(CodeGenTypeRoutingService.class)
                .chatModel(routingChatModelPrototype)
                .build();
    }

    /**
     * 默认提供一个Bean，兼容老逻辑
     * @return
     */
    @Bean
    public CodeGenTypeRoutingService aiCodeGenTypeRoutingService() {
        return createCodeGenTypeRoutingService();
    }

}

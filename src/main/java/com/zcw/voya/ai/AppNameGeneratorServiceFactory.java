package com.zcw.voya.ai;

import com.zcw.voya.util.SpringContextUtil;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 应用名称生成服务工厂
 */
@Configuration
@Slf4j
public class AppNameGeneratorServiceFactory {

    /**
     * 创建应用名称生成服务
     * @return AppNameGeneratorService
     */
    public AppNameGeneratorService createAppNameGeneratorService() {
        ChatModel simpleTaskChatModel = SpringContextUtil.getBean("simpleTaskChatModelPrototype", ChatModel.class);
        return AiServices.builder(AppNameGeneratorService.class)
                .chatModel(simpleTaskChatModel)
                .build();
    }
}

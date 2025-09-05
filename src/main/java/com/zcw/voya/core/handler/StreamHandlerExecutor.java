package com.zcw.voya.core.handler;

import com.zcw.voya.ai.model.enums.CodeGenTypeEnum;
import com.zcw.voya.model.entity.User;
import com.zcw.voya.service.ChatHistoryService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import jakarta.annotation.Resource;

/**
 * 流处理器执行器
 * 根据代码生成的类型创建合适的流处理器
 */
@Component
public class StreamHandlerExecutor {

    @Resource
    private JsonMessageStreamHandler jsonMessageStreamHandler;

    /**
     * 执行流处理器
     * @param originFlux 原始数据流
     * @param chatHistoryService 对话历史服务
     * @param appId 应用ID
     * @param loginUser 登录用户
     * @param codeGenTypeEnum 代码生成的类型
     * @return 处理后的数据流
     */
    public Flux<String> doExecute(Flux<String> originFlux, ChatHistoryService chatHistoryService, long appId, User loginUser, CodeGenTypeEnum codeGenTypeEnum) {
        return switch (codeGenTypeEnum){
            // vue工程-使用json消息流处理器
            case VUE_PROJECT -> jsonMessageStreamHandler.handle(originFlux, chatHistoryService, appId, loginUser);
            // 其他类型-使用简单文本消息流处理器
            case HTML,MULTI_FILE -> new SimpleTextStreamHandler().handler(originFlux, chatHistoryService, appId, loginUser);
        };
    }
}
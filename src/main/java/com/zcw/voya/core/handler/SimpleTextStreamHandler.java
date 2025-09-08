package com.zcw.voya.core.handler;

import cn.hutool.core.util.StrUtil;
import com.zcw.voya.model.entity.User;
import com.zcw.voya.model.enums.ChatHistoryMessageTypeEnum;
import com.zcw.voya.service.ChatHistoryService;
import reactor.core.publisher.Flux;

/**
 * 处理简单的文本流
 * 适用于：htm和多文件的流式响应
 */
public class SimpleTextStreamHandler {

    /**
     * 处理简单文本
     * @param flux
     * @param chatHistoryService
     * @param appId
     * @param loginUser
     * @return
     */
    public Flux<String> handler(Flux<String> flux, ChatHistoryService chatHistoryService,Long appId, User loginUser) {
        StringBuilder builder = new StringBuilder();
        return flux
                .map(chunk -> {
                    // 收集ai响应的内容
                    builder.append(chunk);
                    return chunk;
                })
                .doOnComplete(() -> {
                    // 响应完成，保存AI响应内容
                    String completeResponse = builder.toString();
                    if (StrUtil.isNotBlank(completeResponse)) {
                        chatHistoryService.addChatHistory(appId, completeResponse, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                    }
                }).doOnError(error -> {
                    // 如果响应错误同样保存记录
                    String errorMessage = error.getMessage();
                    chatHistoryService.addChatHistory(appId, errorMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                });
    }
}

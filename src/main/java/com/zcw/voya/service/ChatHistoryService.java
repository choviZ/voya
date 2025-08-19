package com.zcw.voya.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.zcw.voya.model.dto.chat.ChatHistoryQueryRequest;
import com.zcw.voya.model.entity.ChatHistory;
import com.zcw.voya.model.entity.User;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 * @author zcw
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 添加对话历史
     *
     * @param appId       应用id
     * @param message     消息
     * @param messageType 消息类型
     * @param userId      用户id
     * @return 是否添加成功
     */
    boolean addChatHistory(Long appId, String message, String messageType, Long userId);

    Page<ChatHistory> listChatHistoryByPage(Long appId, int pageSize, LocalDateTime lastCreateTime, User loginUser);

    int loadHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxMessages);

    /**
     * 删除对话历史
     *
     * @param appId 应用id
     * @return 是否删除成功
     */
    boolean deleteById(Long appId);

    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);
}

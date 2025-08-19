package com.zcw.voya.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.zcw.voya.exception.ErrorCode;
import com.zcw.voya.exception.ThrowUtils;
import com.zcw.voya.model.dto.chat.ChatHistoryQueryRequest;
import com.zcw.voya.model.entity.ChatHistory;
import com.zcw.voya.mapper.ChatHistoryMapper;
import com.zcw.voya.model.entity.User;
import com.zcw.voya.model.enums.ChatHistoryMessageTypeEnum;
import com.zcw.voya.service.ChatHistoryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.internal.chat.AssistantMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话历史 服务层实现。
 *
 * @author zcw
 */
@Service
@Slf4j
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    @Override
    public boolean addChatHistory(Long appId, String message, String messageType, Long userId) {
        ThrowUtils.throwIf(appId == null, ErrorCode.PARAMS_ERROR, "appId不能为空");
        ThrowUtils.throwIf(message == null || message.isEmpty(), ErrorCode.PARAMS_ERROR, "message不能为空");
        ThrowUtils.throwIf(messageType == null, ErrorCode.PARAMS_ERROR, "typeEnum不能为空");
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR, "userId不能为空");
        // 验证消息类型是否有效
        ChatHistoryMessageTypeEnum typeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(typeEnum == null, ErrorCode.PARAMS_ERROR, "typeEnum错误");
        // 保存
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .message(message)
                .messageType(messageType)
                .userId(userId)
                .build();
        return this.save(chatHistory);
    }

    @Override
    public Page<ChatHistory> listChatHistoryByPage(Long appId, int pageSize, LocalDateTime lastCreateTime, User loginUser) {
        ThrowUtils.throwIf(appId == null, ErrorCode.PARAMS_ERROR, "appId不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "pageSize应在0~50之间");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // TODO 业务逻辑：是否验证权限仅本人能查看记录
        // 构建查询条件
        ChatHistoryQueryRequest chatHistoryQueryRequest = new ChatHistoryQueryRequest();
        chatHistoryQueryRequest.setAppId(appId);
        chatHistoryQueryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = getQueryWrapper(chatHistoryQueryRequest);
        // 查询
        return page(Page.of(1, pageSize), queryWrapper);
    }

    @Override
    public int loadHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxMessages) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(ChatHistory::getAppId, appId)
                .orderBy(ChatHistory::getCreateTime, false)
                .limit(1, maxMessages);

        List<ChatHistory> chatHistories = this.list(queryWrapper);
        if (CollUtil.isEmpty(chatHistories)) {
            return 0;
        }
        // 翻转列表，确保顺序正确，老的在前新的在后
        chatHistories = chatHistories.reversed();
        // 按时间顺序添加到记忆中
        int loadedCount = 0;
        // 清理历史缓存
        chatMemory.clear();
        try {
            for (ChatHistory chatHistory : chatHistories) {
                if (ChatHistoryMessageTypeEnum.USER.getValue().equals(chatHistory.getMessageType())) {
                    // 用户消息
                    chatMemory.add(UserMessage.from(chatHistory.getMessage()));
                    loadedCount++;
                }
                if (ChatHistoryMessageTypeEnum.AI.getValue().equals(chatHistory.getMessageType())) {
                    // AI回复的助手消息
                    chatMemory.add(AiMessage.from(chatHistory.getMessage()));
                    loadedCount++;
                }
            }
            log.info("appId:{}加载了{}条历史消息", appId, loadedCount);
        } catch (Exception e) {
            log.error("加载历史消息失败,appId:{},error:{}",appId,e.getMessage());
            return 0;
        }
        return loadedCount;
    }

    @Override
    public boolean deleteById(Long appId) {
        ThrowUtils.throwIf(appId == null, ErrorCode.PARAMS_ERROR, "appId不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(ChatHistory::getAppId, appId);
        return this.remove(queryWrapper);
    }

    /**
     * 获取查询包装类
     *
     * @param chatHistoryQueryRequest
     * @return
     */
    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (chatHistoryQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chatHistoryQueryRequest.getId();
        String message = chatHistoryQueryRequest.getMessage();
        String messageType = chatHistoryQueryRequest.getMessageType();
        Long appId = chatHistoryQueryRequest.getAppId();
        Long userId = chatHistoryQueryRequest.getUserId();
        LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();
        // 拼接查询条件
        queryWrapper.eq("id", id)
                .like("message", message)
                .eq("messageType", messageType)
                .eq("appId", appId)
                .eq("userId", userId);
        // 游标查询逻辑 - 只使用 createTime 作为游标
        if (lastCreateTime != null) {
            queryWrapper.lt("createTime", lastCreateTime);
        }
        // 排序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            // 默认按创建时间降序排列
            queryWrapper.orderBy("createTime", false);
        }
        return queryWrapper;
    }

}

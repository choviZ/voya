package com.zcw.voya.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zcw.voya.ai.guardrail.PromptSafetyInputGuardrail;
import com.zcw.voya.ai.model.enums.CodeGenTypeEnum;
import com.zcw.voya.ai.tools.*;
import com.zcw.voya.exception.BusinessException;
import com.zcw.voya.exception.ErrorCode;
import com.zcw.voya.service.ChatHistoryService;
import com.zcw.voya.util.SpringContextUtil;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Resource
    private ChatModel openAiChatModel;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    /**
     * AI 服务实例缓存
     * 缓存策略：
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，appId: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * 根据 appId 获取服务（带缓存）
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }

    /**
     * 根据 appId及生成类型 获取服务（带缓存）
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum genTypeEnum) {
        String cacheKey = genTypeEnum + "_" + appId;
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, genTypeEnum));
    }

    /**
     * 创建新的 AI 服务实例
     *
     * @param appId       appId
     * @param genTypeEnum 生成类型
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum genTypeEnum) {
        log.info("为 appId: {} 创建新的 AI 服务实例", appId);
        // 根据 appId 构建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(50)
                .build();
        // 加载对话历史到对话记忆中
        chatHistoryService.loadHistoryToMemory(appId, chatMemory, 20);
        return switch (genTypeEnum) {
            // 普通项目用默认模型
            case HTML, MULTI_FILE -> {
                // 获取模型
                StreamingChatModel chatModel = SpringContextUtil.getBean("streamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .streamingChatModel(chatModel)
                        .chatMemory(chatMemory)
                        // 输入护轨
                        .inputGuardrails(new PromptSafetyInputGuardrail())
                        .build();
            }
            // Vue 项目用推理模型
            case VUE_PROJECT -> {
                StreamingChatModel chatModel = SpringContextUtil.getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(openAiChatModel)
                        .streamingChatModel(chatModel)
                        .chatMemoryProvider(memoryId -> chatMemory)
                        // 添加工具
                        .tools(
                                new FileWriteTool(),
                                new FileReadTool(),
                                new FileModifyTool(),
                                new FileDeleteTool(),
                                new FileDirReadTool()
                        )
                        // 幻觉工具名称处理（调用了不存在的工具）
                        .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                                toolExecutionRequest, "Error:no tool called " + toolExecutionRequest.name()
                        ))
                        // 输入护轨
                        .inputGuardrails(new PromptSafetyInputGuardrail())
                        .build();
            }
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的生成类型");
        };
    }

}

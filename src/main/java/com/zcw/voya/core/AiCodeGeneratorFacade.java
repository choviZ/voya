package com.zcw.voya.core;

import cn.hutool.json.JSONUtil;
import com.zcw.voya.ai.AiCodeGeneratorService;
import com.zcw.voya.ai.AiCodeGeneratorServiceFactory;
import com.zcw.voya.ai.model.HtmlCodeResult;
import com.zcw.voya.ai.model.MultiFileCodeResult;
import com.zcw.voya.ai.model.enums.CodeGenTypeEnum;
import com.zcw.voya.ai.model.message.AiResponseMessage;
import com.zcw.voya.ai.model.message.ToolExecutedMessage;
import com.zcw.voya.ai.model.message.ToolRequestMessage;
import com.zcw.voya.core.parser.CodeParserExecutor;
import com.zcw.voya.core.saver.CodeFileSaverExecutor;
import com.zcw.voya.exception.BusinessException;
import com.zcw.voya.exception.ErrorCode;
import dev.langchain4j.service.TokenStream;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * Ai 代码生成门面类，组合代码生成和文件写入
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    /**
     * 统一入口：根据类型生成代码并保存（阻塞）
     *
     * @param prompt
     * @param codeGenTypeEnum
     * @return
     */
    public File generateCode(String prompt, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择代码生成类型");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(prompt);
                yield CodeFileSaverExecutor.executorSaver(htmlCodeResult, codeGenTypeEnum, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(prompt);
                yield CodeFileSaverExecutor.executorSaver(multiFileCodeResult, codeGenTypeEnum, appId);
            }
            default -> {
                throw new IllegalArgumentException("不支持的代码生成类型");
            }
        };
    }

    /**
     * 统一入口：根据类型生成代码并保存（流式）
     *
     * @param prompt
     * @param codeGenTypeEnum
     * @return
     */
    public Flux<String> generateCodeStream(String prompt, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择代码生成类型");
        }
        log.info("即将开始生成，类型为:{}", codeGenTypeEnum.getValue());
        return switch (codeGenTypeEnum) {
            case HTML -> generateHtmlCodeStream(prompt, appId);
            case MULTI_FILE -> generateMultiFileCodeStream(prompt, appId);
            case VUE_PROJECT -> generateVueProjectStream(prompt, appId);
            default -> {
                throw new IllegalArgumentException("不支持的代码生成类型");
            }
        };
    }

    /**
     * 生成Vue项目代码流
     *
     * @param prompt
     * @param appId
     * @return
     */
    private Flux<String> generateVueProjectStream(String prompt, Long appId) {
        AiCodeGeneratorService service = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, CodeGenTypeEnum.VUE_PROJECT);
        TokenStream tokenStream = service.generateVueProjectCodeStream(appId, prompt);
        Flux<String> flux = processTokenStream(tokenStream);
        return processStreamCode(flux, CodeGenTypeEnum.VUE_PROJECT, prompt, appId);
    }

    /**
     * 处理token流转化为Flux流
     */
    private Flux<String> processTokenStream(TokenStream tokenStream) {
        return Flux.create(sink -> {
            // 部分响应
            tokenStream.onPartialResponse(partialResponse -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    // 工具请求
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    // 执行工具
                    .onToolExecuted(toolExecution -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    // 完成响应
                    .onCompleteResponse(completeResponse -> {
                        sink.complete();
                    })
                    .onError(error -> {
                        error.printStackTrace();
                        sink.error(error);
                    })
                    .start();
        });

    }

    /**
     * 生成多文件代码流
     *
     * @param prompt
     * @param appId
     * @return
     */
    private Flux<String> generateMultiFileCodeStream(String prompt, Long appId) {
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId);
        Flux<String> flux = aiCodeGeneratorService.generateMultiFileCodeStream(prompt);
        return processStreamCode(flux, CodeGenTypeEnum.MULTI_FILE, prompt, appId);
    }

    /**
     * 生成HTML代码流
     *
     * @param prompt
     * @param appId
     * @return
     */
    private Flux<String> generateHtmlCodeStream(String prompt, Long appId) {
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId);
        Flux<String> flux = aiCodeGeneratorService.generateHtmlCodeStream(prompt);
        return processStreamCode(flux, CodeGenTypeEnum.HTML, prompt, appId);
    }

    /**
     * 流式处理代码生成结果
     *
     * @param flux
     * @param codeGenTypeEnum
     * @param prompt
     * @return
     */
    private Flux<String> processStreamCode(Flux<String> flux, CodeGenTypeEnum codeGenTypeEnum, String prompt, Long appId) {
        log.info("开始生成代码：{}", prompt);
        StringBuilder builder = new StringBuilder();
        // 收集生成结果
        return flux
                .doOnNext(builder::append)
                .doOnComplete(() -> {
                    try {
                        // 全部生成完毕后开始解析
                        String completeResponse = builder.toString();
                        Object codeResult = CodeParserExecutor.executeParser(completeResponse, codeGenTypeEnum);
                        File saveDir = CodeFileSaverExecutor.executorSaver(codeResult, codeGenTypeEnum, appId);
                        log.info("保存文件成功：{}", saveDir.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("代码生成失败：{}", e.getMessage());
                    }
                });

    }
}

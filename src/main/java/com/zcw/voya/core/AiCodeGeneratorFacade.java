package com.zcw.voya.core;

import com.zcw.voya.ai.AiCodeGeneratorService;
import com.zcw.voya.ai.model.HtmlCodeResult;
import com.zcw.voya.ai.model.MultiFileCodeResult;
import com.zcw.voya.ai.model.enums.CodeGenTypeEnum;
import com.zcw.voya.core.parser.CodeParserExecutor;
import com.zcw.voya.core.saver.CodeFileSaverExecutor;
import com.zcw.voya.exception.BusinessException;
import com.zcw.voya.exception.ErrorCode;
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
    private AiCodeGeneratorService aiCodeGeneratorService;

    /**
     * 统一入口：根据类型生成代码并保存
     *
     * @param prompt
     * @param codeGenTypeEnum
     * @return
     */
    public File generateCode(String prompt, CodeGenTypeEnum codeGenTypeEnum,Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择代码生成类型");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(prompt);
                yield CodeFileSaverExecutor.executorSaver(htmlCodeResult, codeGenTypeEnum,appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(prompt);
                yield CodeFileSaverExecutor.executorSaver(multiFileCodeResult, codeGenTypeEnum,appId);
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
    public Flux<String> generateCodeStream(String prompt, CodeGenTypeEnum codeGenTypeEnum,Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择代码生成类型");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> generateHtmlCodeStream(prompt,appId);
            case MULTI_FILE -> generateMultiFileCodeStream(prompt,appId);
            default -> {
                throw new IllegalArgumentException("不支持的代码生成类型");
            }
        };
    }

    private Flux<String> generateMultiFileCodeStream(String prompt,Long appId) {
        Flux<String> flux = aiCodeGeneratorService.generateMultiFileCodeStream(prompt);
        return processStreamCode(flux, CodeGenTypeEnum.MULTI_FILE, prompt,appId);
    }

    private Flux<String> generateHtmlCodeStream(String prompt,Long appId) {
        Flux<String> flux = aiCodeGeneratorService.generateHtmlCodeStream(prompt);
        return processStreamCode(flux, CodeGenTypeEnum.HTML, prompt,appId);
    }

    /**
     * 流式处理代码生成结果
     *
     * @param flux
     * @param codeGenTypeEnum
     * @param prompt
     * @return
     */
    private Flux<String> processStreamCode(Flux<String> flux, CodeGenTypeEnum codeGenTypeEnum,String prompt,Long appId) {
        StringBuilder builder = new StringBuilder();
        // 收集生成结果
        return flux
                .doOnNext(builder::append)
                .doOnComplete(() -> {
                    try {
                        // 全部生成完毕后开始解析
                        String completeResponse = builder.toString();
                        Object codeResult = CodeParserExecutor.executeParser(completeResponse, codeGenTypeEnum);
                        File saveDir = CodeFileSaverExecutor.executorSaver(codeResult, codeGenTypeEnum,appId);
                        log.info("保存文件成功：{}", saveDir.getAbsolutePath());
                    }catch (Exception e){
                        log.error("代码生成失败：{}", e.getMessage());
                    }
                });

    }
}

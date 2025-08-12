package com.zcw.voya.core;

import com.zcw.voya.ai.AiCodeGeneratorService;
import com.zcw.voya.ai.model.HtmlCodeResult;
import com.zcw.voya.ai.model.MultiFileCodeResult;
import com.zcw.voya.ai.model.enums.CodeGenTypeEnum;
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
     * @param prompt
     * @param codeGenTypeEnum
     * @return
     */
    public File generateCode(String prompt, CodeGenTypeEnum codeGenTypeEnum) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择代码生成类型");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> generateHtmlCode(prompt);
            case MULTI_FILE -> generateMultiFileCode(prompt);
            default -> {
                throw new IllegalArgumentException("不支持的代码生成类型");
            }
        };
    }

    /**
     * 统一入口：根据类型生成代码并保存（流式）
     * @param prompt
     * @param codeGenTypeEnum
     * @return
     */
    public Flux<String> generateCodeStream(String prompt, CodeGenTypeEnum codeGenTypeEnum) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择代码生成类型");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> generateHtmlCodeStream(prompt);
            case MULTI_FILE -> generateMultiFileCodeStream(prompt);
            default -> {
                throw new IllegalArgumentException("不支持的代码生成类型");
            }
        };
    }

    private Flux<String> generateMultiFileCodeStream(String prompt) {
        Flux<String> flux = aiCodeGeneratorService.generateMultiFileCodeStream(prompt);
        StringBuilder builder = new StringBuilder();
        return flux
                .doOnNext(chunk -> {
                    // 收集生成结果
                    builder.append(chunk);
                })
                .doOnComplete(() -> {
                    // 全部生成完毕后开始解析
                    String completeResponse = builder.toString();
                    MultiFileCodeResult multiFileCodeResult = CodeParser.parseMultiFileCode(completeResponse);
                    File saveDir = CodeFileSaver.saveMultiFileCodeResult(multiFileCodeResult);
                    log.info("保存文件成功：{}", saveDir.getAbsolutePath());
                });
    }

    private Flux<String> generateHtmlCodeStream(String prompt) {
        Flux<String> flux = aiCodeGeneratorService.generateHtmlCodeStream(prompt);
        StringBuilder builder = new StringBuilder();
        return flux
                .doOnNext(chunk -> {
                    // 收集生成结果
                    builder.append(chunk);
                })
                .doOnComplete(() -> {
                    // 全部生成完毕后开始解析
                    String completeResponse = builder.toString();
                    HtmlCodeResult htmlCodeResult = CodeParser.parseHtmlCode(completeResponse);
                    File saveDir = CodeFileSaver.saveHtmlCodeResult(htmlCodeResult);
                    log.info("保存文件成功：{}", saveDir.getAbsolutePath());
                });

    }

    /**
     * 生成html并保存
     *
     * @param userPrompt
     * @return
     */
    private File generateHtmlCode(String userPrompt) {
        HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userPrompt);
        return CodeFileSaver.saveHtmlCodeResult(htmlCodeResult);
    }

    /**
     * 生成多文件并保存
     *
     * @param userPrompt
     * @return
     */
    private File generateMultiFileCode(String userPrompt) {
        MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(userPrompt);
        return CodeFileSaver.saveMultiFileCodeResult(multiFileCodeResult);
    }
}

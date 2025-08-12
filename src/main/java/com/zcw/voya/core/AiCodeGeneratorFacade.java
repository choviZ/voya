package com.zcw.voya.core;

import com.zcw.voya.ai.AiCodeGeneratorService;
import com.zcw.voya.ai.model.HtmlCodeResult;
import com.zcw.voya.ai.model.MultiFileCodeResult;
import com.zcw.voya.ai.model.enums.CodeGenTypeEnum;
import com.zcw.voya.exception.BusinessException;
import com.zcw.voya.exception.ErrorCode;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * Ai 代码生成门面类，组合代码生成和文件写入
 */
@Service
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

package com.zcw.voya.core.saver;

import com.zcw.voya.ai.model.HtmlCodeResult;
import com.zcw.voya.ai.model.MultiFileCodeResult;
import com.zcw.voya.ai.model.enums.CodeGenTypeEnum;
import com.zcw.voya.exception.BusinessException;
import com.zcw.voya.exception.ErrorCode;

import java.io.File;

/**
 * 代码文件保存执行器
 */
public class CodeFileSaverExecutor {

    private static final HtmlCodeFileSaverTemplate htmlCodeFileSaverTemplate = new HtmlCodeFileSaverTemplate();
    private static final MultiFIleCodeFileSaverTemplate multiFIleCodeFileSaverTemplate = new MultiFIleCodeFileSaverTemplate();

    /**
     * 执行代码保存
     * @param resultObj
     * @param codeGenTypeEnum
     * @return
     */
    public static File executorSaver(Object resultObj, CodeGenTypeEnum codeGenTypeEnum){
        return switch (codeGenTypeEnum){
            case HTML -> htmlCodeFileSaverTemplate.saveCode((HtmlCodeResult) resultObj);
            case MULTI_FILE -> multiFIleCodeFileSaverTemplate.saveCode((MultiFileCodeResult) resultObj);
            default -> {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"不支持的代码生成类型");
            }
        };
    }
}

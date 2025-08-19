package com.zcw.voya.core.saver;

import com.zcw.voya.ai.model.MultiFileCodeResult;
import com.zcw.voya.ai.model.enums.CodeGenTypeEnum;
import com.zcw.voya.exception.ErrorCode;
import com.zcw.voya.exception.ThrowUtils;

/**
 * 多文件代码保存模板
 */
public class MultiFIleCodeFileSaverTemplate extends CodeFileSaverTemplate<MultiFileCodeResult>{
    @Override
    protected CodeGenTypeEnum getCodeGenType() {
        return CodeGenTypeEnum.MULTI_FILE;
    }

    @Override
    protected void saveCodeFiles(MultiFileCodeResult result, String dirPath) {
        writeToFile(dirPath, "index.html", result.getHtmlCode());
        writeToFile(dirPath, "style.css", result.getCssCode());
        writeToFile(dirPath, "script.js", result.getJsCode());
    }

    @Override
    protected void validateInput(MultiFileCodeResult result) {
        super.validateInput(result);
        // 至少要有html代码，css和js可以为空
        ThrowUtils.throwIf(result.getHtmlCode() == null, ErrorCode.PARAMS_ERROR, "html代码不能为空");
    }
}

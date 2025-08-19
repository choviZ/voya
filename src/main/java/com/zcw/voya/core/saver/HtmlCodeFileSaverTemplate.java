package com.zcw.voya.core.saver;

import com.zcw.voya.ai.model.HtmlCodeResult;
import com.zcw.voya.ai.model.enums.CodeGenTypeEnum;
import com.zcw.voya.exception.ErrorCode;
import com.zcw.voya.exception.ThrowUtils;

/**
 * html代码保存模板
 */
public class HtmlCodeFileSaverTemplate extends CodeFileSaverTemplate<HtmlCodeResult>{
    @Override
    protected CodeGenTypeEnum getCodeGenType() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    protected void saveCodeFiles(HtmlCodeResult result, String dirPath) {
        // 保存html代码
        writeToFile(dirPath, "index.html", result.getHtmlCode());
    }

    @Override
    protected void validateInput(HtmlCodeResult result) {
        super.validateInput(result);
        ThrowUtils.throwIf(result.getHtmlCode() == null, ErrorCode.PARAMS_ERROR, "html代码不能为空");
    }
}

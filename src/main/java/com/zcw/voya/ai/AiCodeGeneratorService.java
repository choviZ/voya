package com.zcw.voya.ai;

import com.zcw.voya.ai.model.HtmlCodeResult;
import com.zcw.voya.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.SystemMessage;

/**
 * LangChain4j AiService接口
 */
public interface AiCodeGeneratorService {

    /**
     * 生成 HTML 代码
     *
     * @param userMessage 用户消息
     * @return 生成的代码结果
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    HtmlCodeResult generateHtmlCode(String userMessage);

    /**
     * 生成多文件代码
     *
     * @param userMessage 用户消息
     * @return 生成的代码结果
     */
    @SystemMessage(fromResource = "prompt/codegen-multifile-system-prompt.txt")
    MultiFileCodeResult generateMultiFileCode(String userMessage);
}


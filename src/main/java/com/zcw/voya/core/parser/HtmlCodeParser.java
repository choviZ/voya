package com.zcw.voya.core.parser;

import com.zcw.voya.ai.model.HtmlCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTML 单文件解析器
 */
public class HtmlCodeParser implements CodeParser<HtmlCodeResult>{

    private static final Pattern HTML_CODE_PATTERN = Pattern.compile("```html\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    @Override
    public HtmlCodeResult parser(String codeContent) {
        HtmlCodeResult htmlCodeResult = new HtmlCodeResult();
        // 提取html代码
        String htmlCode = extractHtmlCode(codeContent);
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            htmlCodeResult.setHtmlCode(htmlCode.trim());
        }else {
            // 如果没有找到代码块，将整个内容作为HTML
            htmlCodeResult.setHtmlCode(codeContent);
        }
        return htmlCodeResult;
    }

    /**
     * 提取HTML代码
     * @param content
     * @return
     */
    private String extractHtmlCode(String content) {
        Matcher matcher = HTML_CODE_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}

package com.zcw.voya.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * 原生-单文件生成结果
 */
@Data
@Description("生成html代码文件的结果")
public class HtmlCodeResult {

    @Description(value = "html代码")
    private String htmlCode;

    @Description(value = "对生成的代码的描述")
    private String description;
}

package com.zcw.voya.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * 原生-多文件生成结果
 */
@Data
@Description("生成多个代码文件的结果")
public class MultiFileCodeResult {

    @Description(value = "html代码")
    private String htmlCode;

    @Description(value = "css代码")
    private String cssCode;

    @Description(value = "javascript代码")
    private String jsCode;

    @Description(value = "代码的描述")
    private String description;
}

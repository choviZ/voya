package com.zcw.voya.core.parser;

/**
 * 代码解析接口
 */
public interface CodeParser<T> {

    /**
     * 解析代码内容
     * @param codeContent
     * @return
     */
    T parser(String codeContent);
}

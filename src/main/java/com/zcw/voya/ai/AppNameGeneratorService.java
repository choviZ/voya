package com.zcw.voya.ai;

import dev.langchain4j.service.SystemMessage;

/**
 * 应用名称生成服务
 */
public interface AppNameGeneratorService {

    /**
     * 生成应用名称
     * @param userPrompt 用户输入
     * @return 应用名称
     */
    @SystemMessage(fromResource = "prompt/appname-gen-system-prompt.txt")
    String generateAppName(String userPrompt);
}

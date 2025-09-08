package com.zcw.voya.ai;

import com.zcw.voya.ai.model.enums.CodeGenTypeEnum;
import dev.langchain4j.service.SystemMessage;

/**
 * Ai代码生成类型路由服务工厂
 */
public interface CodeGenTypeRoutingService {

    /**
     * 根据用户需求智能选择代码生成类型
     * @param userPrompt userPrompt
     * @return 生成类型枚举
     */
    @SystemMessage(fromResource = "prompt/mode-routing-system-prompt.txt")
    CodeGenTypeEnum routeCodeGenType(String userPrompt);
}

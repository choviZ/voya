package com.zcw.voya.ai.tools;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 工具管理器
 */
@Slf4j
@Component
public class ToolManager {

    /**
     * 工具名称和实例的映射
     */
    private final Map<String,BaseTool> map = new HashMap<>();

    /**
     * 自动注入所有工具
     */
    @Resource
    private BaseTool[] tools;

    /**
     * 初始化工具映射
     */
    @PostConstruct
    public void init(){
        for (BaseTool tool : tools) {
            map.put(tool.getToolName(),tool);
            log.info("工具 {} 注册成功",tool.getToolName());
        }
        log.info("工具管理器初始化完成，共注册：{}个工具",map.size());
    }

    /**
     * 根据工具名称获取工具实例
     * @param toolName 工具英文名称
     * @return 工具实例
     */
    public BaseTool getTool(String toolName){
        return map.get(toolName);
    }

    /**
     * 获取所有工具
     * @return 工具实例集合
     */
    public BaseTool[] getAllTools(){
        return tools;
    }
}

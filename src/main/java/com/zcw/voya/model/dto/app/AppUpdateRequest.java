package com.zcw.voya.model.dto.app;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 更新应用请求
 *
 * @author zcw
 */
@Data
public class AppUpdateRequest {

    /**
     * 应用id
     */
    @NotNull(message = "应用id不能为空")
    private Long id;

    /**
     * 应用名称
     */
    @NotBlank(message = "应用名称不能为空")
    @Size(max = 50, message = "应用名称长度不能超过50个字符")
    private String appName;

    /**
     * 应用封面
     */
    private String cover;

    /**
     * 初始化提示词
     */
    private String initPrompt;

    /**
     * 代码生成类型
     */
    private String codeGenType;

    /**
     * 部署密钥
     */
    private String deployKey;

    /**
     * 部署时间
     */
    private String deployedTime;

    /**
     * 优先级
     */
    private Integer priority;

}
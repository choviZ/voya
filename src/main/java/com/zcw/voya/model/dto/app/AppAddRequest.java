package com.zcw.voya.model.dto.app;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建应用请求
 *
 * @author zcw
 */
@Data
public class AppAddRequest {
    /**
     * 初始化提示词
     */
    @NotBlank(message = "初始化提示词不能为空")
    private String initPrompt;

}
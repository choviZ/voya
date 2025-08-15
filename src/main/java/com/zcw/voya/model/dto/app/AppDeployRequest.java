package com.zcw.voya.model.dto.app;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 应用部署请求
 */
@Data
public class AppDeployRequest implements Serializable {

    /**
     * 应用id
     */
    @NotNull(message = "应用id不能为空")
    private Long appId;

}

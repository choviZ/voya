package com.zcw.voya.model.dto.app;

import com.zcw.voya.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 应用查询请求
 *
 * @author zcw
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AppQueryRequest extends PageRequest {

    /**
     * 应用id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 创建用户id
     */
    private Long userId;

    /**
     * 代码生成类型
     */
    private String codeGenType;

    /**
     * 部署密钥
     */
    private String deployKey;

    /**
     * 优先级
     */
    private Integer priority;

}
package com.zcw.voya.model.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.io.Serializable;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;


@Data
public class UserAddRequest implements Serializable {

    /**
     * 用户昵称
     */
    @NotBlank(message = "用户昵称不能为空")
    @Length(min = 1, max = 20, message = "用户昵称长度在1~20之间")
    private String userName;

    /**
     * 账号
     */
    @NotBlank(message = "账号不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "账号只能包含字母、数字和下划线")
    @Length(min = 6, max = 16, message = "账号长度在6~16之间")
    private String userAccount;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    @Length(max = 500, message = "用户简介长度不能超过500")
    private String userProfile;

    /**
     * 用户角色: user, admin
     */
    @Pattern(regexp = "^(user|admin)$", message = "用户角色只能为user或admin")
    private String userRole;

    private static final long serialVersionUID = 1L;
}

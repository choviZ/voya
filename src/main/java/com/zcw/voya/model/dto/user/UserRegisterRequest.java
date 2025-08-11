package com.zcw.voya.model.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.io.Serializable;

@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 账号
     */
    @NotBlank(message = "账号不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "账号只能包含字母、数字和下划线")
    @Length(min = 6, max = 16, message = "账号长度在6~16之间")
    private String userAccount;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]+$",
            message = "密码只能包含字母、数字和特殊字符")
    @Length(min = 6, max = 16, message = "密码长度在6~16之间")
    private String userPassword;

    /**
     * 确认密码
     */
    @NotBlank(message = "请再次输入密码")
    @Pattern(regexp = "^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]+$",
            message = "密码只能包含字母、数字和特殊字符")
    @Length(min = 6, max = 16, message = "密码长度在6~16之间")
    private String checkPassword;
}

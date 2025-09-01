package com.q.library_management_system.dto.request;

import lombok.Data;
import org.hibernate.validator.constraints.Email;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class UserInfoUpdateRequestDTO {
    @NotNull(message = "用户ID不能为空")
    private Integer userId;

    private String realName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;
}


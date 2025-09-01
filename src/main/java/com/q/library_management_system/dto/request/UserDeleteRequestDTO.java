package com.q.library_management_system.dto.request;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 用户账号删除请求DTO
 */
@Data
public class UserDeleteRequestDTO {
    @NotNull(message = "用户ID不能为空")
    @Min(value = 1, message = "用户ID必须为正数")
    private Integer userId;

    @NotBlank(message = "密码不能为空")
    private String password; // 用于验证身份，防止误操作
}


package com.q.library_management_system.dto.request;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;
/**
 * 用户登录请求DTO
 * 接收登录参数，仅需用户名和密码。
 */
@Data
public class UserLoginRequestDTO {
    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    private String userName;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    private String password;
}

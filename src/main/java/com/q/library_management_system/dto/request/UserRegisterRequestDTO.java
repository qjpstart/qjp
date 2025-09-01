package com.q.library_management_system.dto.request;

import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 用户注册请求DTO
 * 接收用户注册时的必要参数，添加校验（如手机号格式、密码长度）。
 */
@Data
public class UserRegisterRequestDTO {
    /** 用户名（3-20位，字母/数字/下划线） */
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,20}$", message = "用户名需为3-20位字母、数字或下划线")
    private String userName;

    /** 密码（6-20位，包含字母和数字） */
    @NotBlank(message = "密码不能为空")
    @Length(min = 6, max = 20, message = "密码长度需为6-20位")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).+$", message = "密码需包含字母和数字")
    private String password;

    /** 真实姓名（2-10位中文） */
    @NotBlank(message = "真实姓名不能为空")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5]{2,10}$", message = "真实姓名需为2-10位中文")
    private String realName;

    /** 手机号（11位数字） */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的11位手机号")
    private String phone;

    /** 邮箱（可选，格式校验） */
    @Pattern(regexp = "^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$", message = "邮箱格式不正确")
    private String email;
}


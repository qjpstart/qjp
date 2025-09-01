package com.q.library_management_system.dto.response;

import lombok.Data;

/**
 * 登录响应DTO
 * 用于向前端返回登录成功后的用户信息和令牌
 */
@Data
public class UserLoginResponseDTO {
    private Integer userId; // 用户ID

    private String userName; // 用户名

    private String userType; // 用户类型（如"reader"/"admin"，用于权限控制）

    private String token; // 登录令牌（用于后续接口认证）
}


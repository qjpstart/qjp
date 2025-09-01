package com.q.library_management_system.dto.response;

import com.q.library_management_system.entity.User;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户列表项DTO
 * 用于分页查询时，封装单条用户数据（仅包含列表展示所需字段）
 */
@Data
public class UserItemDTO {
    private Integer userId;         // 用户ID
    private String userName;        // 用户名
    private String realName;        // 真实姓名
    private String phone;           // 手机号
    private String email;           // 邮箱
    private String userType;        // 用户类型（reader/admin）
    private String status;          // 用户状态（normal/frozen/deleted）
    private Integer creditScore;    // 信用分
    private LocalDateTime registerTime; // 注册时间

    /**
     * 从User实体转换为UserItemDTO
     * @param user 用户实体对象
     * @return 转换后的UserItemDTO
     */
    public static UserItemDTO fromEntity(User user) {
        UserItemDTO dto = new UserItemDTO();
        dto.setUserId(user.getUserId());
        dto.setUserName(user.getUserName());
        dto.setRealName(user.getRealName());
        dto.setPhone(user.getPhone());
        dto.setEmail(user.getEmail());
        // 转换枚举为字符串（避免直接返回枚举对象）
        dto.setUserType(user.getUserType().name());
        dto.setStatus(user.getStatus().name());
        dto.setCreditScore(user.getCreditScore());
        dto.setRegisterTime(user.getRegisterTime());
        return dto;
    }
}


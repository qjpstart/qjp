package com.q.library_management_system.dto.request;

import com.q.library_management_system.entity.User;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户状态变更请求DTO
 * 用于管理员禁用/启用用户账号
 */
@Data
public class UserStatusChangeRequestDTO {

    /** 目标用户ID */
    @NotNull(message = "用户ID不能为空")
    private Integer userId;

    /** 目标状态（normal-正常，frozen-冻结） */
    @NotNull(message = "目标状态不能为空")
    private User.UserStatus targetStatus; // 引用User实体类的内部枚举

    /** 操作人ID（管理员ID） */
    @NotNull(message = "操作人ID不能为空")
    private Integer operatorId;

    /** 状态变更原因 */
    private String reason; // 可选，记录变更原因（如"违规操作冻结"）
}


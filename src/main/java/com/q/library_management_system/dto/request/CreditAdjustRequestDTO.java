package com.q.library_management_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;



/**
 * 信用分调整请求DTO
 * 用于接收前端传递的信用分调整参数
 */
@Data
public class CreditAdjustRequestDTO {

    /**
     * 用户ID
     * 必须不为空，用于指定调整哪个用户的信用分
     */
    @NotNull(message = "用户ID不能为空")
    private Integer userId;

    /**
     * 调整分值
     * 正数表示增加信用分，负数表示扣减信用分，必须不为空
     */
    @NotNull(message = "调整分值不能为空")
    private Integer adjustScore;

    /**
     * 调整原因
     * 记录信用分调整的原因（如"逾期还书扣减"、"推荐图书奖励"等）
     * 用于日志记录和审计，必须不为空
     */
    @NotBlank(message = "调整原因不能为空")
    private String reason;
}

package com.q.library_management_system.dto.request;



import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 用户信用分调整请求DTO
 * 用于管理员增加/扣减用户信用分（如逾期扣减、良好行为奖励）
 */
public class UserCreditAdjustRequestDTO {
    /** 用户ID */
    @NotNull(message = "用户ID不能为空")
    @Min(value = 1, message = "用户ID必须为正数")
    private Integer userId;

    /** 信用分调整值（正数=增加，负数=扣减） */
    @NotNull(message = "调整分值不能为空")
    private Integer creditAdjust;

    /** 调整原因（如“逾期未还扣减”“连续3个月无逾期奖励”） */
    @NotNull(message = "调整原因不能为空")
    @Size(min = 2, max = 100, message = "调整原因需为2-100位")
    private String reason;

    /** 操作人ID（管理员ID） */
    @NotNull(message = "操作人ID不能为空")
    @Min(value = 1, message = "操作人ID必须为正数")
    private Integer operatorId;

    // getter/setter
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public Integer getCreditAdjust() { return creditAdjust; }
    public void setCreditAdjust(Integer creditAdjust) { this.creditAdjust = creditAdjust; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Integer getOperatorId() { return operatorId; }
    public void setOperatorId(Integer operatorId) { this.operatorId = operatorId; }
}

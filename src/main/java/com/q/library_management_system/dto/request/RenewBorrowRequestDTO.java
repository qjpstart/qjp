package com.q.library_management_system.dto.request;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 图书续借请求DTO
 */
public class RenewBorrowRequestDTO {

    /**
     * 借阅记录ID（唯一标识一条借阅记录）
     * 必须为正数，且对应未归还的借阅记录
     */
    @NotNull(message = "借阅记录ID不能为空")
    @Min(value = 1, message = "借阅记录ID必须为正数")
    private Integer recordId;

    /**
     * 用户ID（用于权限校验，确保是本人续借）
     */
    @NotNull(message = "用户ID不能为空")
    @Min(value = 1, message = "用户ID必须为正数")
    private Integer userId;

    /**
     * 续借天数（可选，默认7天）
     * 最大续借天数由系统配置（如最多续借15天）
     */
    @Min(value = 1, message = "续借天数必须为正数")
    private Integer renewDays = 7;

    // 手动生成getter和setter（Java 7无Lombok）
    public Integer getRecordId() {
        return recordId;
    }

    public void setRecordId(Integer recordId) {
        this.recordId = recordId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getRenewDays() {
        return renewDays;
    }

    public void setRenewDays(Integer renewDays) {
        this.renewDays = renewDays;
    }
}


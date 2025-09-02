package com.q.library_management_system.dto.request;

import jakarta.validation.constraints.Min;



/**
 * 取消预约请求DTO
 * 接收取消预约的必要参数，包含权限校验
 */
public class CancelReserveRequestDTO {

    /** 预约记录ID（必须为正数） */
    @Min(value = 1, message = "预约记录ID必须为正数")
    private Integer reserveId;

    /** 用户ID（用于权限校验，必须为正数） */
    @Min(value = 1, message = "用户ID必须为正数")
    private Integer userId;

    // 手动生成getter和setter
    public Integer getReserveId() {
        return reserveId;
    }

    public void setReserveId(Integer reserveId) {
        this.reserveId = reserveId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }
}


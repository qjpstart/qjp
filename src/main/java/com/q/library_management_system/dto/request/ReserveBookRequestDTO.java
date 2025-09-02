package com.q.library_management_system.dto.request;


import jakarta.validation.constraints.Min;

/**
 * 图书预约请求DTO
 * 接收用户预约图书的必要参数，包含基础校验
 */
public class ReserveBookRequestDTO {

    /** 图书ID（必须为正数） */
    @Min(value = 1, message = "图书ID必须为正数")
    private Integer bookId;

    /** 用户ID（必须为正数） */
    @Min(value = 1, message = "用户ID必须为正数")
    private Integer userId;


    // 手动生成getter和setter
    public Integer getBookId() {
        return bookId;
    }

    public void setBookId(Integer bookId) {
        this.bookId = bookId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

}


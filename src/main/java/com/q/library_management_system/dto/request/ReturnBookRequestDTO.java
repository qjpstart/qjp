package com.q.library_management_system.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * 图书归还请求DTO
 * 接收归还参数，需借阅记录 ID（确保归还的是指定借阅记录）。
 */
@Data
public class ReturnBookRequestDTO {
    /** 借阅记录ID（唯一标识一条借阅记录） */
    @NotNull(message = "借阅记录ID不能为空")
    @Min(value = 0, message = "借阅记录ID必须为正数")
    private Integer recordId;

    /** 用户ID（防越权：确保归还的是当前用户的记录） */
    @NotNull(message = "用户ID不能为空")
    @Min(value = 0, message = "用户ID必须为正数")
    private Integer userId;
}


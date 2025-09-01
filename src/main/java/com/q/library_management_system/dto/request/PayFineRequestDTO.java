package com.q.library_management_system.dto.request;

import lombok.Data;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;

/**
 * 罚款缴纳请求DTO
 * 接收罚款缴纳参数，需借阅记录 ID 和用户 ID（防越权）。
 */
@Data
public class PayFineRequestDTO {
    /** 借阅记录ID（对应一条逾期记录） */
    @NotNull(message = "借阅记录ID不能为空")
    @Min(value = 1, message = "借阅记录ID必须为正数")
    private Integer recordId;

    /** 用户ID（防越权：确保缴纳的是当前用户的罚款） */
    @NotNull(message = "用户ID不能为空")
    @Min(value = 1, message = "用户ID必须为正数")
    private Integer userId;
}


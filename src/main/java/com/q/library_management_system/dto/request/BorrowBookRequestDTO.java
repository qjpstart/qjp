package com.q.library_management_system.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * 图书借阅请求DTO
 * 接收借阅参数，仅需用户 ID 和图书 ID（借阅时间、应还时间由系统自动生成）。
 */
@Data
public class BorrowBookRequestDTO {
    /** 用户ID */
    @NotNull(message = "用户ID不能为空")
    @Min(value = 1, message = "用户ID必须为正数")
    private Integer userId;

    /** 图书ID */
    @NotNull(message = "图书ID不能为空")
    @Min(value = 1, message = "图书ID必须为正数")
    private Integer bookId;

    /** 借阅天数（可选，默认15天） */
    @Min(value = 1, message = "借阅天数必须为正数")
    private Integer borrowDays = 15;
}

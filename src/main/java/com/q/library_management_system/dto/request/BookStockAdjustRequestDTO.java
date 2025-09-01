package com.q.library_management_system.dto.request;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

/**
 * 图书库存调整请求DTO
 * 用于管理员增加/减少图书库存（如补充新书、处理破损图书）
 */
public class BookStockAdjustRequestDTO {
    /** 图书ID */
    @NotNull(message = "图书ID不能为空")
    @Min(value = 1, message = "图书ID必须为正数")
    private Integer bookId;

    /** 调整数量（正数=增加库存，负数=减少库存） */
    @NotNull(message = "调整数量不能为空")
    private Integer adjustNum;

    /** 调整原因（如“补充新书”“图书破损”） */
    @NotNull(message = "调整原因不能为空")
    @Length(min = 2, max = 100, message = "调整原因需为2-100位")
    private String reason;

    /** 操作人ID（管理员ID） */
    @NotNull(message = "操作人ID不能为空")
    @Min(value = 1, message = "操作人ID必须为正数")
    private Integer operatorId;

    // getter/setter
    public Integer getBookId() { return bookId; }
    public void setBookId(Integer bookId) { this.bookId = bookId; }
    public Integer getAdjustNum() { return adjustNum; }
    public void setAdjustNum(Integer adjustNum) { this.adjustNum = adjustNum; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Integer getOperatorId() { return operatorId; }
    public void setOperatorId(Integer operatorId) { this.operatorId = operatorId; }
}

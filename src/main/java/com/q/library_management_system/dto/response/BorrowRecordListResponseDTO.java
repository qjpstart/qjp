package com.q.library_management_system.dto.response;

import com.q.library_management_system.entity.BorrowRecord;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 借阅记录列表响应DTO
 * 返回用户借阅记录列表，包含图书名称、借阅状态、逾期信息等。
 */
@Data
public class BorrowRecordListResponseDTO {
    /** 借阅记录ID */
    private Integer recordId;

    /** 图书ID */
    private Integer bookId;

    /** 图书名称 */
    private String bookTitle;

    /** 借阅时间 */
    private LocalDateTime borrowDate;

    /** 应还时间 */
    private LocalDateTime dueDate;

    /** 实际归还时间（未归还则为null） */
    private LocalDateTime returnDate;

    /** 借阅状态（unreturned=未归还，returned=已归还，overdue=逾期） */
    private BorrowRecord.BorrowStatus borrowStatus;

    /** 逾期天数（未逾期则为0） */
    private Integer overdueDays;

    /** 罚款金额（无罚款则为0） */
    private BigDecimal fineAmount;

    /** 续借次数 */
    private Integer renewCount;
}

package com.q.library_management_system.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "borrow_record")
public class BorrowRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer recordId;
    @Column(name = "book_id", nullable = false)
    private Integer bookId;
    @Column(name = "user_id", nullable = false)
    private Integer userId;
    @Column(name = "borrow_date", nullable = false)
    private LocalDateTime borrowDate;
    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;
    @Column(name = "return_date")
    private LocalDateTime returnDate;
    @Column(name = "borrow_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private BorrowStatus borrowStatus = BorrowStatus.unreturned;
    @Column(name = "renew_count", nullable = false)
    private Integer renewCount = 0;
    @Column(name = "fine_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal fineAmount = BigDecimal.ZERO;

    public enum BorrowStatus {
        unreturned,
        returned,
        overdue
    }
}

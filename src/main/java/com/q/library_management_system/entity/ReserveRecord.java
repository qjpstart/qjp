package com.q.library_management_system.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "reserve_record")
public class ReserveRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer reserveId;
    @Column(name = "book_id", nullable = false)
    private Integer bookId;
    @Column(name = "user_id", nullable = false)
    private Integer userId;
    @Column(name = "reserve_date", nullable = false)
    private LocalDateTime reserveDate;
    @Column(name = "expire_date", nullable = false)
    private LocalDateTime expireDate;
    @Column(name = "reserve_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReserveStatus reserveStatus = ReserveStatus.waiting;

    public enum ReserveStatus {
        waiting,
        reserved,
        cancelled
    }
}


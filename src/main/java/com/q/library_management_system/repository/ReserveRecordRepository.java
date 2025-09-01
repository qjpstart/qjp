package com.q.library_management_system.repository;

import com.q.library_management_system.entity.ReserveRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReserveRecordRepository extends JpaRepository<ReserveRecord, Integer> {
    // 根据用户ID查询预约记录
    List<ReserveRecord> findByUserId(Integer userId);

    // 根据图书ID查询预约记录
    List<ReserveRecord> findByBookId(Integer bookId);

    // 根据预约状态查询（如查询所有待处理的预约）
    List<ReserveRecord> findByReserveStatus(ReserveRecord.ReserveStatus status);

    // 检查用户是否已预约某本书（避免重复预约）
    boolean existsByBookIdAndUserIdAndReserveStatusIn(
            Integer bookId,
            Integer userId,
            List<ReserveRecord.ReserveStatus> statuses
    );

    // 根据用户ID和预约状态查询预约记录
    List<ReserveRecord> findByUserIdAndReserveStatus(Integer userId, ReserveRecord.ReserveStatus status);

    // 根据图书ID和预约状态查询记录（解决当前报错）
    List<ReserveRecord> findByBookIdAndReserveStatus(Integer bookId, ReserveRecord.ReserveStatus status);

    // 查询所有状态为等待中且已过期的预约（批量处理用）
    List<ReserveRecord> findByReserveStatusAndExpireDateBefore(
            ReserveRecord.ReserveStatus status,
            LocalDateTime expireDate
    );

    // 查询即将到期的预约（提醒功能用）
    List<ReserveRecord> findByReserveStatusAndExpireDateBetween(
            ReserveRecord.ReserveStatus status,
            LocalDateTime start,
            LocalDateTime end
    );
}


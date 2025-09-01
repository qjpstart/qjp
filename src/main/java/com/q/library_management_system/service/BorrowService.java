package com.q.library_management_system.service;

import com.q.library_management_system.entity.BorrowRecord;
import java.util.List;

public interface BorrowService {
    // 借书
    BorrowRecord borrowBook(Integer userId, Integer bookId, int days);

    // 还书
    BorrowRecord returnBook(Integer recordId);

    // 续借
    BorrowRecord renewBook(Integer recordId, int days);

    // 查询用户的借阅记录
    List<BorrowRecord> getUserBorrowRecords(Integer userId, BorrowRecord.BorrowStatus status);

    // 查询图书的借阅记录
    List<BorrowRecord> getBookBorrowRecords(Integer bookId, BorrowRecord.BorrowStatus status);

    /**
     * 处理逾期记录
     * @param recordId 单个记录ID（传null则处理所有未逾期的未归还记录）
     */
    void handleOverdueRecords(Integer recordId);

    /**
     * 缴纳逾期罚款
     * @param recordId 借阅记录ID
     * @param userId 用户ID（用于权限校验）
     * @return 缴纳结果信息
     */
    String payPenalty(Integer recordId, Integer userId);

    /**
     * 查询用户的逾期未缴罚款记录
     * @param userId 用户ID
     * @return 逾期未缴罚款的记录列表
     */
    List<BorrowRecord> getUnpaidOverdueRecords(Integer userId);

    BorrowRecord getBorrowRecordById(Integer recordId);
}

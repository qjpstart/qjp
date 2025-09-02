package com.q.library_management_system.repository;

import com.q.library_management_system.entity.BorrowRecord;
import lombok.Data;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Integer> {
    // 根据用户ID查询借阅记录
    List<BorrowRecord> findByUserId(Integer userId);

    // 根据图书ID查询借阅记录
    List<BorrowRecord> findByBookId(Integer bookId);

    /**
     * 查询指定图书ID列表中存在未归还借出记录的图书ID
     * @param bookIds 待查询的图书ID列表
     * @return 存在未归还借出记录的图书ID列表
     */
    @Query("SELECT DISTINCT br.bookId FROM BorrowRecord br " +
            "WHERE br.bookId IN :bookIds " +
            "AND br.returnDate IS NULL") // 只查询未归还的记录
    List<Integer> findBorrowedBookIds(@Param("bookIds") List<Integer> bookIds);

    // 根据借阅状态查询（如查询所有未归还的记录）
    List<BorrowRecord> findByBorrowStatus(BorrowRecord.BorrowStatus  borrowStatus);

    // 根据用户ID和状态查询（如查询用户的未还记录）
    List<BorrowRecord> findByUserIdAndBorrowStatus(Integer userId, BorrowRecord.BorrowStatus  borrowStatus);

    // 检查“指定用户、指定图书、指定状态”的借阅记录是否存在，避免用户重复借阅同一本未归还的图书
    boolean existsByUserIdAndBookIdAndBorrowStatus(
            Integer userId,          // 参数1：用户ID
            Integer bookId,          // 参数2：图书ID
            BorrowRecord.BorrowStatus borrowStatus // 参数3：借阅状态（如 unreturned）
    );

    //  按图书ID + 借阅状态查询记录
    List<BorrowRecord> findByBookIdAndBorrowStatus(Integer bookId, BorrowRecord.BorrowStatus  borrowStatus);

    // 检查指定图书是否存在未归还的借阅记录,删除图书前验证（有未归还记录则不允许删除）
    boolean existsByBookIdAndBorrowStatus(Integer bookId, BorrowRecord.BorrowStatus  borrowStatus);

    // 查询所有已借出且超期未还的记录（用于批量处理）
    List<BorrowRecord> findByBorrowStatusAndDueDateBefore(BorrowRecord.BorrowStatus borrowStatus, LocalDateTime dueDate);
}
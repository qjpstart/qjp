package com.q.library_management_system.service.impl;

import com.q.library_management_system.entity.Book;
import com.q.library_management_system.entity.BorrowRecord;
import com.q.library_management_system.entity.ReserveRecord;
import com.q.library_management_system.entity.User;
import com.q.library_management_system.entity.User.UserStatus;
import com.q.library_management_system.exception.BusinessException;
import com.q.library_management_system.repository.BookRepository;
import com.q.library_management_system.repository.BorrowRecordRepository;
import com.q.library_management_system.repository.ReserveRecordRepository;
import com.q.library_management_system.repository.UserRepository;
import com.q.library_management_system.service.BorrowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class BorrowServiceImpl implements BorrowService {

    private final BorrowRecordRepository borrowRecordRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final ReserveRecordRepository reserveRecordRepository;

    // 锁对象缓存，确保同一bookId对应同一锁实例
    private final ConcurrentHashMap<Integer, Object> lockMap = new ConcurrentHashMap<>();

    private Object getLock(Integer bookId) {
        return lockMap.computeIfAbsent(bookId, k -> new Object());
    }

    // 逾期相关配置
    private static final int OVERDUE_FREEZE_THRESHOLD = 7; // 逾期7天冻结账户
    private static final BigDecimal DAILY_FINE_RATE = new BigDecimal("0.5"); // 每天罚款0.5元
    private static final BigDecimal MAX_FINE_AMOUNT = new BigDecimal("20.00"); // 最高罚款20元

    private LocalDateTime addDaysToCurrentDate(int days) {
        LocalDateTime now = LocalDateTime.now();
        // 手动计算：每天86400秒，通过秒数累加实现天数增加
        return now.plusSeconds(days * 86400L);
    }

    private long calculateOverdueDays(LocalDateTime dueDate, LocalDateTime returnDate) {
        // 转换为UTC时区的秒数时间戳，避免时区差异
        long dueSeconds = dueDate.toEpochSecond(ZoneOffset.UTC);
        long returnSeconds = returnDate.toEpochSecond(ZoneOffset.UTC);

        if (returnSeconds <= dueSeconds) {
            return 0; // 未逾期
        }

        // 秒数差 → 天数（1天 = 86400秒）
        long diffSeconds = returnSeconds - dueSeconds;
        long overdueDays = diffSeconds / 86400L;

        // 不足1天按1天算（如逾期1天1秒，按2天算）
        if (diffSeconds % 86400L != 0) {
            overdueDays++;
        }

        return overdueDays;
    }

    // -------------------------- 业务方法实现 --------------------------
    @Override
    @Transactional
    public BorrowRecord borrowBook(Integer userId, Integer bookId, int days) {
        // 增加锁机制，确保同一本书的借阅操作串行执行，防止并发超借
        synchronized (getLock(bookId)) {
            // 1. 校验用户状态
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException("用户不存在"));
            if (user.getStatus() == User.UserStatus.frozen) {
                throw new BusinessException("用户账号已冻结，无法借书");
            }

            // 2. 校验图书状态和库存（查询时加锁或使用悲观锁查询）
            Book book = bookRepository.findByIdWithLock(bookId) // 需在BookRepository添加带锁查询方法
                    .orElseThrow(() -> new BusinessException("图书不存在"));
            if (book.getAvailableCount() <= 0) {
                throw new BusinessException("图书库存不足");
            }

            // 3. 检查是否有未归还的同一本书
            if (borrowRecordRepository.existsByUserIdAndBookIdAndBorrowStatus(
                    userId, bookId, BorrowRecord.BorrowStatus.unreturned)) {
                throw new BusinessException("不可重复借阅同一本书");
            }

            // 4. 检查预约权限：只有当前预约队列的第一位用户可借阅
            List<ReserveRecord> validReserves = reserveRecordRepository
                    .findByBookIdAndReserveStatusOrderByReserveDateAsc(
                            bookId, ReserveRecord.ReserveStatus.reserved);
            if (!validReserves.isEmpty()) {
                ReserveRecord firstReserve = validReserves.get(0);
                if (!firstReserve.getUserId().equals(userId)) {
                    throw new BusinessException("当前有其他用户预约该图书，请排队等待");
                }
                // 5. 若为当前有效预约用户，更新预约状态为completed
                firstReserve.setReserveStatus(ReserveRecord.ReserveStatus.completed);
                reserveRecordRepository.save(firstReserve);
            }

            // 6. 创建借阅记录
            BorrowRecord record = new BorrowRecord();
            record.setUserId(userId);
            record.setBookId(bookId);
            record.setBorrowDate(LocalDateTime.now());
            record.setDueDate(addDaysToCurrentDate(days));
            record.setBorrowStatus(BorrowRecord.BorrowStatus.unreturned);
            record.setRenewCount(0);
            record.setFineAmount(BigDecimal.ZERO);

            // 7. 扣减库存（基于加锁后的查询结果，确保库存正确）
            book.setAvailableCount(book.getAvailableCount() - 1);
            bookRepository.save(book);

            return borrowRecordRepository.save(record);
        }
    }


    @Override
    @Transactional
    public BorrowRecord returnBook(Integer recordId) {
        BorrowRecord record = borrowRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException("借阅记录不存在"));

        // 检查是否已归还
        if (record.getBorrowStatus() != BorrowRecord.BorrowStatus.unreturned) {
            throw new BusinessException("该记录已归还或逾期");
        }

        // 更新记录状态（使用LocalDateTime）
        LocalDateTime returnDate = LocalDateTime.now();
        record.setReturnDate(returnDate);

        // 计算逾期罚款（每天0.5元）
        BigDecimal fine = BigDecimal.ZERO;
        if (returnDate.isAfter(record.getDueDate())) {
            // 计算逾期天数（不依赖ChronoUnit）
            final long overdueDays = calculateOverdueDays(record.getDueDate(), returnDate);
            fine = BigDecimal.valueOf(overdueDays * 0.5);
            record.setBorrowStatus(BorrowRecord.BorrowStatus.overdue);

            // 逾期扣信用分（overdueDays是有效final，Lambda中可安全使用）
            userRepository.findById(record.getUserId()).ifPresent(user -> {
                user.setCreditScore(Math.max(0, user.getCreditScore() - (int) overdueDays));
                userRepository.save(user);
            });
        } else {
            record.setBorrowStatus(BorrowRecord.BorrowStatus.returned);
        }

        record.setFineAmount(fine);
        borrowRecordRepository.save(record);

        // 恢复库存
        bookRepository.findById(record.getBookId()).ifPresent(book -> {
            book.setAvailableCount(book.getAvailableCount() + 1);
            bookRepository.save(book);
        });

        return record;
    }

    @Override
    @Transactional
    public BorrowRecord renewBook(Integer recordId, int days) {
        BorrowRecord record = borrowRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException("借阅记录不存在"));

        // 检查续借条件
        if (record.getBorrowStatus() != BorrowRecord.BorrowStatus.unreturned) {
            throw new BusinessException("只有未归还的图书可续借");
        }
        if (record.getRenewCount() >= 2) {
            throw new BusinessException("已达到最大续借次数");
        }

        // 更新到期日（不依赖ChronoUnit）
        LocalDateTime newDueDate = record.getDueDate().plusSeconds(days * 86400L);
        record.setDueDate(newDueDate);
        record.setRenewCount(record.getRenewCount() + 1);

        return borrowRecordRepository.save(record);
    }

    @Override
    public List<BorrowRecord> getUserBorrowRecords(Integer userId, BorrowRecord.BorrowStatus status) {
        if (status == null) {
            return borrowRecordRepository.findByUserId(userId);
        } else {
            return borrowRecordRepository.findByUserIdAndBorrowStatus(userId, status);
        }
    }

    @Override
    public List<BorrowRecord> getBookBorrowRecords(Integer bookId, BorrowRecord.BorrowStatus status) {
        if (status == null) {
            return borrowRecordRepository.findByBookId(bookId);
        } else {
            return borrowRecordRepository.findByBookIdAndBorrowStatus(bookId, status);
        }
    }

    /**
     * 处理逾期记录：标记逾期状态、计算罚款、冻结超期账户
     */
    @Override
    @Transactional
    public void handleOverdueRecords(Integer recordId) {
        List<BorrowRecord> records = getRecordsToProcess(recordId);

        for (BorrowRecord record : records) {
            // 跳过已处理或未逾期的记录
            if (isRecordProcessedOrNotOverdue(record)) {
                continue;
            }

            // 1. 计算逾期天数和罚款金额
            long overdueDays = calculateOverdueDays(record.getDueDate());
            BigDecimal fineAmount = calculateFineAmount(overdueDays);

            // 2. 更新借阅记录状态和罚款金额
            record.setBorrowStatus(BorrowRecord.BorrowStatus.overdue);
            record.setFineAmount(fineAmount);
            borrowRecordRepository.save(record);

            // 3. 超过阈值天数则冻结用户账户
            if (overdueDays >= OVERDUE_FREEZE_THRESHOLD) {
                freezeUserAccount(record.getUserId());
            }
        }
    }

    /**
     * 缴纳逾期罚款：更新罚款状态、解冻账户
     */
    @Override
    @Transactional
    public String payPenalty(Integer recordId, Integer userId) {
        // 1. 获取并验证记录
        BorrowRecord record = borrowRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException("借阅记录不存在"));

        validateRecordPermission(record, userId);
        validateRecordIsOverdue(record);
        validateFineNotPaid(record);

        // 2. 更新记录状态（假设returned状态表示已处理）
        record.setBorrowStatus(BorrowRecord.BorrowStatus.returned);
        // 实际场景中可添加支付时间字段：record.setPayTime(LocalDateTime.now());
        borrowRecordRepository.save(record);

        // 3. 解冻用户账户
        unfreezeUserAccount(userId);

        return String.format("罚款缴纳成功！金额：%.2f元，账户已恢复正常", record.getFineAmount());
    }

    /**
     * 查询用户的逾期未缴罚款记录
     */
    @Override
    public List<BorrowRecord> getUnpaidOverdueRecords(Integer userId) {
        return borrowRecordRepository.findByUserIdAndBorrowStatus(
                userId, BorrowRecord.BorrowStatus.overdue
        );
    }

    // -------------------------- 辅助方法 --------------------------

    /**
     * 获取需要处理的记录（单个或批量）
     */
    private List<BorrowRecord> getRecordsToProcess(Integer recordId) {
        if (recordId != null) {
            // 处理单个记录
            return List.of(borrowRecordRepository.findById(recordId)
                    .orElseThrow(() -> new BusinessException("借阅记录不存在")));
        } else {
            // 批量处理：查询所有未归还且已逾期的记录
            return borrowRecordRepository.findByStatusAndDueTimeBefore(
                    BorrowRecord.BorrowStatus.unreturned, LocalDateTime.now()
            );
        }
    }

    /**
     * 判断记录是否已处理或未逾期
     */
    private boolean isRecordProcessedOrNotOverdue(BorrowRecord record) {
        // 已逾期或已归还的记录无需处理
        if (record.getBorrowStatus() == BorrowRecord.BorrowStatus.overdue ||
                record.getBorrowStatus() == BorrowRecord.BorrowStatus.returned) {
            return true;
        }
        // 未逾期的记录无需处理
        return LocalDateTime.now().isBefore(record.getDueDate()) ||
                LocalDateTime.now().isEqual(record.getDueDate());
    }

    /**
     * 计算逾期天数
     */
    private long calculateOverdueDays(LocalDateTime dueDate) {
        Duration duration = Duration.between(dueDate, LocalDateTime.now());
        return Math.max(duration.toDays(), 0); // 确保天数不为负
    }

    /**
     * 计算罚款金额
     */
    private BigDecimal calculateFineAmount(long overdueDays) {
        BigDecimal calculatedFine = DAILY_FINE_RATE.multiply(BigDecimal.valueOf(overdueDays));
        return calculatedFine.compareTo(MAX_FINE_AMOUNT) > 0 ?
                MAX_FINE_AMOUNT : calculatedFine;
    }

    /**
     * 冻结用户账户
     */
    private void freezeUserAccount(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        user.setStatus(UserStatus.frozen);
        userRepository.save(user);
    }

    /**
     * 解冻用户账户
     */
    private void unfreezeUserAccount(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        user.setStatus(UserStatus.normal);
        userRepository.save(user);
    }

    /**
     * 验证记录归属权（防止越权）
     */
    private void validateRecordPermission(BorrowRecord record, Integer userId) {
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException("无权操作他人的逾期记录");
        }
    }

    /**
     * 验证记录是否为逾期状态
     */
    private void validateRecordIsOverdue(BorrowRecord record) {
        if (record.getBorrowStatus() != BorrowRecord.BorrowStatus.overdue) {
            throw new BusinessException("该记录不是逾期状态，无需缴纳罚款");
        }
    }

    /**
     * 验证罚款是否未缴纳
     */
    private void validateFineNotPaid(BorrowRecord record) {
        // 实际场景中可添加finePaid字段，此处简化为判断状态
        if (record.getBorrowStatus() == BorrowRecord.BorrowStatus.returned) {
            throw new BusinessException("该罚款已缴纳，无需重复操作");
        }
    }

    @Override
    public BorrowRecord getBorrowRecordById(Integer recordId) {
        return borrowRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException("借阅记录不存在：" + recordId));
    }

}




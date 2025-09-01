package com.q.library_management_system.service.impl;

import com.q.library_management_system.entity.Book;
import com.q.library_management_system.entity.ReserveRecord;
import com.q.library_management_system.entity.User;
import com.q.library_management_system.exception.BusinessException;
import com.q.library_management_system.repository.BookRepository;
import com.q.library_management_system.repository.ReserveRecordRepository;
import com.q.library_management_system.repository.UserRepository;
import com.q.library_management_system.service.ReserveService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReserveServiceImpl implements ReserveService {

    private final ReserveRecordRepository reserveRecordRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    // 工具方法：给当前日期加指定天数（替代 LocalDateTime.plus(days, ChronoUnit.DAYS)）
    private LocalDateTime addDaysToCurrentDate(int days) {
        return LocalDateTime.now().plusSeconds(days * 86400L);
    }

    // 工具方法：计算两个LocalDateTime的天数差（替代 ChronoUnit.DAYS.between()）
    private long calculateOverdueDays(LocalDateTime dueDate, LocalDateTime returnDate) {
        long dueSeconds = dueDate.toEpochSecond(ZoneOffset.UTC);
        long returnSeconds = returnDate.toEpochSecond(ZoneOffset.UTC);

        if (returnSeconds <= dueSeconds) {
            return 0;
        }

        long diffSeconds = returnSeconds - dueSeconds;
        long overdueDays = diffSeconds / 86400L;

        if (diffSeconds % 86400L != 0) {
            overdueDays++;
        }

        return overdueDays;
    }

    //预约图书
    @Override
    @Transactional
    public ReserveRecord reserveBook(Integer userId, Integer bookId, int validDays) {
        // 校验用户状态
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (user.getStatus() == User.UserStatus.frozen) {
            throw new BusinessException("用户账号已冻结，无法预约");
        }

        // 校验图书存在
        if (!bookRepository.existsById(bookId)) {
            throw new BusinessException("图书不存在");
        }

        // 检查是否已存在有效预约
        List<ReserveRecord.ReserveStatus> validStatuses = Arrays.asList(
                ReserveRecord.ReserveStatus.waiting,
                ReserveRecord.ReserveStatus.reserved
        );
        if (reserveRecordRepository.existsByBookIdAndUserIdAndReserveStatusIn(
                bookId, userId, validStatuses)) {
            throw new BusinessException("您已预约过该图书");
        }

        // 创建预约记录
        ReserveRecord reserve = new ReserveRecord();
        reserve.setUserId(userId);
        reserve.setBookId(bookId);
        reserve.setReserveDate(LocalDateTime.now());
        reserve.setExpireDate(addDaysToCurrentDate(validDays));
        reserve.setReserveStatus(ReserveRecord.ReserveStatus.waiting);

        return reserveRecordRepository.save(reserve);
    }

    // 取消预约
    @Override
    @Transactional
    public void cancelReservation(Integer reserveId, Integer userId) {
        ReserveRecord reserve = reserveRecordRepository.findById(reserveId)
                .orElseThrow(() -> new BusinessException("预约记录不存在"));

        // 检查是否是本人预约
        if (!reserve.getUserId().equals(userId)) {
            throw new BusinessException("无权取消他人预约");
        }

        // 检查状态是否可取消
        if (reserve.getReserveStatus() == ReserveRecord.ReserveStatus.cancelled) {
            throw new BusinessException("该预约已取消");
        }

        reserve.setReserveStatus(ReserveRecord.ReserveStatus.cancelled);
        reserveRecordRepository.save(reserve);
    }

    // 确认预约
    @Override
    @Transactional
    public void confirmReservation(Integer reserveId) {
        ReserveRecord reserve = reserveRecordRepository.findById(reserveId)
                .orElseThrow(() -> new BusinessException("预约记录不存在"));

        // 检查状态是否可确认
        if (reserve.getReserveStatus() != ReserveRecord.ReserveStatus.waiting) {
            throw new BusinessException("只有等待中的预约可确认");
        }

        // 检查是否过期
        if (LocalDateTime.now().isAfter(reserve.getExpireDate())) {
            throw new BusinessException("预约已过期");
        }

        reserve.setReserveStatus(ReserveRecord.ReserveStatus.reserved);
        reserveRecordRepository.save(reserve);
    }

    // 查询用户预约
    @Override
    public List<ReserveRecord> getUserReservations(Integer userId, ReserveRecord.ReserveStatus status) {
        if (status == null) {
            return reserveRecordRepository.findByUserId(userId);
        } else {
            return reserveRecordRepository.findByUserIdAndReserveStatus(userId, status);
        }
    }

    // 查询图书预约
    @Override
    public List<ReserveRecord> getBookReservations(Integer bookId, ReserveRecord.ReserveStatus status) {
        if (status == null) {
            return reserveRecordRepository.findByBookId(bookId);
        } else {
            return reserveRecordRepository.findByBookIdAndReserveStatus(bookId, status);
        }
    }

    /**
     * 处理到期预约记录：将已过期的预约标记为取消状态
     * @param reserveId 单个预约ID（传null则批量处理所有到期预约）
     */
    @Override
    @Transactional
    public void handleExpiredReserves(Integer reserveId) {
        List<ReserveRecord> records = getRecordsToProcess(reserveId);

        for (ReserveRecord record : records) {
            // 只处理等待中且已过期的预约
            if (isValidExpiredReserve(record)) {
                // 将到期未处理的预约标记为取消
                record.setReserveStatus(ReserveRecord.ReserveStatus.cancelled);
                reserveRecordRepository.save(record);
            }
        }
    }

    /**
     * 查询即将到期的预约（可用于提醒用户）
     * @param hours 小时数，查询未来N小时内将要过期的预约
     * @return 即将到期的预约列表
     */
    @Override
    public List<ReserveRecord> getUpcomingExpiredReserves(int hours) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusHours(hours);

        // 查询状态为等待中，且过期时间在当前时间到未来N小时内的预约
        return reserveRecordRepository.findByReserveStatusAndExpireDateBetween(
                ReserveRecord.ReserveStatus.waiting,
                now,
                deadline
        );
    }

    /**
     * 检查预约是否已过期
     * @param reserveId 预约ID
     * @return 已过期返回true，否则返回false
     */
    @Override
    public boolean isReserveExpired(Integer reserveId) {
        ReserveRecord record = reserveRecordRepository.findById(reserveId)
                .orElseThrow(() -> new BusinessException("预约记录不存在"));

        return LocalDateTime.now().isAfter(record.getExpireDate())
                && record.getReserveStatus() == ReserveRecord.ReserveStatus.waiting;
    }

    // -------------------------- 辅助方法 --------------------------

    /**
     * 获取需要处理的预约记录（单个或批量）
     */
    private List<ReserveRecord> getRecordsToProcess(Integer reserveId) {
        if (reserveId != null) {
            // 处理单个预约记录
            return List.of(reserveRecordRepository.findById(reserveId)
                    .orElseThrow(() -> new BusinessException("预约记录不存在")));
        } else {
            // 批量处理：查询所有状态为等待中且已过期的预约
            return reserveRecordRepository.findByReserveStatusAndExpireDateBefore(
                    ReserveRecord.ReserveStatus.waiting,
                    LocalDateTime.now()
            );
        }
    }

    /**
     * 判断是否为有效的过期预约（状态为等待中且已过过期时间）
     */
    private boolean isValidExpiredReserve(ReserveRecord record) {
        return record.getReserveStatus() == ReserveRecord.ReserveStatus.waiting
                && LocalDateTime.now().isAfter(record.getExpireDate());
    }

    @Override
    public ReserveRecord getReserveById(Integer reserveId) {
        return reserveRecordRepository.findById(reserveId)
                .orElseThrow(() -> new BusinessException("预约记录不存在：" + reserveId));
    }

}



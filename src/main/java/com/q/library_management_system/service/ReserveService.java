package com.q.library_management_system.service;

import com.q.library_management_system.entity.ReserveRecord;
import com.q.library_management_system.exception.BusinessException;
import java.util.List;

public interface ReserveService {
    // 预约图书
    ReserveRecord reserveBook(Integer userId, Integer bookId, int validDays);

    // 取消预约
    void cancelReservation(Integer reserveId, Integer userId);

    // 图书到馆后确认预约
    void confirmReservation(Integer reserveId);

    // 查询用户的预约记录
    List<ReserveRecord> getUserReservations(Integer userId, ReserveRecord.ReserveStatus status);

    // 查询图书的预约记录
    List<ReserveRecord> getBookReservations(Integer bookId, ReserveRecord.ReserveStatus status);

    /**
     * 处理到期预约记录
     * @param reserveId 单个预约ID（传null则批量处理所有到期预约）
     */
    void handleExpiredReserves(Integer reserveId);

    /**
     * 查询即将到期的预约
     * @param hours 小时数，查询未来N小时内将要过期的预约
     * @return 即将到期的预约列表
     */
    List<ReserveRecord> getUpcomingExpiredReserves(int hours);

    /**
     * 检查预约是否已过期
     * @param reserveId 预约ID
     * @return 已过期返回true，否则返回false
     * @throws BusinessException 当预约记录不存在时抛出
     */
    boolean isReserveExpired(Integer reserveId);

    ReserveRecord getReserveById(Integer reserveId);
}


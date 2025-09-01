package com.q.library_management_system.dto.response;

import com.q.library_management_system.entity.ReserveRecord;
import java.util.Date;

/**
 * 预约记录列表响应DTO
 * 返回用户预约记录的关键信息，用于列表展示
 */
public class ReserveRecordListResponseDTO {

    /** 预约记录ID */
    private Integer reserveId;

    /** 图书ID */
    private Integer bookId;

    /** 图书名称 */
    private String bookTitle;

    /** 预约时间 */
    private Date reserveDate;

    /** 过期时间 */
    private Date expireDate;

    /** 预约状态（waiting=等待中，reserved=已预约，cancelled=已取消） */
    private ReserveRecord.ReserveStatus reserveStatus;

    /** 剩余有效时间（单位：小时，仅对等待中状态有效） */
    private Integer remainingHours;

    // 手动生成getter和setter
    public Integer getReserveId() {
        return reserveId;
    }

    public void setReserveId(Integer reserveId) {
        this.reserveId = reserveId;
    }

    public Integer getBookId() {
        return bookId;
    }

    public void setBookId(Integer bookId) {
        this.bookId = bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public Date getReserveDate() {
        return reserveDate;
    }

    public void setReserveDate(Date reserveDate) {
        this.reserveDate = reserveDate;
    }

    public Date getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(Date expireDate) {
        this.expireDate = expireDate;
    }

    public ReserveRecord.ReserveStatus getReserveStatus() {
        return reserveStatus;
    }

    public void setReserveStatus(ReserveRecord.ReserveStatus reserveStatus) {
        this.reserveStatus = reserveStatus;
    }

    public Integer getRemainingHours() {
        return remainingHours;
    }

    public void setRemainingHours(Integer remainingHours) {
        this.remainingHours = remainingHours;
    }
}


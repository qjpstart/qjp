package com.q.library_management_system.dto.response;

import com.q.library_management_system.entity.ReserveRecord;
import java.util.Date;

/**
 * 预约详情响应DTO
 * 返回单个预约记录的完整信息，用于详情页展示
 */
public class ReserveDetailResponseDTO {

    /** 预约记录ID */
    private Integer reserveId;

    /** 图书ID */
    private Integer bookId;

    /** 图书名称 */
    private String bookTitle;

    /** 图书ISBN */
    private String bookIsbn;

    /** 图书分类 */
    private String categoryName;

    /** 用户ID */
    private Integer userId;

    /** 用户名（脱敏处理） */
    private String userName;

    /** 预约时间 */
    private Date reserveDate;

    /** 过期时间 */
    private Date expireDate;

    /** 预约状态 */
    private ReserveRecord.ReserveStatus reserveStatus;

    /** 预约备注 */
    private String remark;

    /** 处理时间（状态变更为reserved或cancelled的时间） */
    private Date handleTime;

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

    public String getBookIsbn() {
        return bookIsbn;
    }

    public void setBookIsbn(String bookIsbn) {
        this.bookIsbn = bookIsbn;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Date getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(Date handleTime) {
        this.handleTime = handleTime;
    }
}


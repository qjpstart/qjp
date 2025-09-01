package com.q.library_management_system.dto.request;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

/**
 * 消息通知发送请求DTO
 * 用于系统发送通知（如预约到期提醒、逾期提醒）
 */
public class NotificationSendRequestDTO {
    /** 接收用户ID */
    @NotNull(message = "用户ID不能为空")
    @Min(value = 1, message = "用户ID必须为正数")
    private Integer userId;

    /** 消息标题 */
    @NotNull(message = "消息标题不能为空")
    @Length(min = 2, max = 50, message = "消息标题需为2-50位")
    private String title;

    /** 消息内容 */
    @NotNull(message = "消息内容不能为空")
    @Length(min = 5, max = 500, message = "消息内容需为5-500位")
    private String content;

    /** 消息类型（如"reserve_expire"=预约到期，"overdue"=逾期） */
    @NotNull(message = "消息类型不能为空")
    private String type;

    // getter/setter
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}


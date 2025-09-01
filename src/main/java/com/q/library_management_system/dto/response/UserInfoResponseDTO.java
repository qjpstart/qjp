package com.q.library_management_system.dto.response;

import com.q.library_management_system.entity.User;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户信息响应DTO
 * 返回用户信息，隐藏敏感字段（如密码），仅暴露前端需展示的内容。
 */
@Data
public class UserInfoResponseDTO {
    /** 用户ID */
    private Integer userId;

    /** 用户名 */
    private String userName;

    /** 真实姓名 */
    private String realName;

    /** 手机号（脱敏：中间4位替换为*） */
    private String phone;

    /** 邮箱（脱敏：@前保留前3位，其余替换为*） */
    private String email;

    /** 用户类型（reader=读者，admin=管理员） */
    private String userType;

    /** 用户状态（normal=正常，frozen=冻结） */
    private String status;

    /** 信用分 */
    private Integer creditScore;

    /** 注册时间 */
    private LocalDateTime registerTime;

    // -------------------------- 脱敏处理（工具方法） --------------------------
    /** 手机号脱敏：138****1234 */
    public void setPhone(String phone) {
        if (phone != null && phone.length() == 11) {
            this.phone = phone.substring(0, 3) + "****" + phone.substring(7);
        } else {
            this.phone = phone;
        }
    }

    /** 邮箱脱敏：abc****@xxx.com */
    public void setEmail(String email) {
        if (email != null && email.contains("@")) {
            String prefix = email.split("@")[0];
            if (prefix.length() > 3) {
                prefix = prefix.substring(0, 3) + "****";
            }
            this.email = prefix + "@" + email.split("@")[1];
        } else {
            this.email = email;
        }
    }
}


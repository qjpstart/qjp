package com.q.library_management_system.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user") // 数据库表 user
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId; // 对应 user_id 字段

    @Column(name = "user_name", nullable = false, unique = true)
    private String userName; // 对应 user_name 字段

    @Column(name = "password", nullable = false)
    private String password; // 对应 password 字段

    @Column(name = "real_name", nullable = false)
    private String realName; // 对应 real_name 字段

    @Column(name = "phone", nullable = false, unique = true)
    private String phone; // 对应 phone 字段

    @Column(name = "email")
    private String email; // 对应 email 字段

    @Column(name = "user_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private UserType userType = UserType.reader; // 对应 user_type 字段，默认值 reader

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.normal; // 对应 status 字段，默认值 normal

    @Column(name = "register_time", nullable = false)
    private LocalDateTime registerTime; // 对应 register_time 字段

    @Column(name = "credit_score", nullable = false)
    private Integer creditScore = 100; // 对应 credit_score 字段，默认值 100

    // 用户类型枚举
    public enum UserType {
        reader,
        admin
    }

    // 用户状态枚举
    public enum UserStatus {
        normal,
        frozen,
        deleted
    }
}

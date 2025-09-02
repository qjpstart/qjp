package com.q.library_management_system.service;

import com.q.library_management_system.dto.request.*;
import com.q.library_management_system.dto.response.*;
import com.q.library_management_system.entity.User;
import com.q.library_management_system.exception.BusinessException;
import org.springframework.transaction.annotation.Transactional;

public interface UserService {
    // 用户注册
    User register(User user);

    // 用户登录（返回用户信息，不含密码）
    User login(String userName, String password);

    // 根据ID查询用户
    User getUserById(Integer id);

    // 根据用户名查询用户
    User findByUsername(String userName);

    // 更新用户信息
    User updateUser(Integer id, User user);

    // 更新密码
    void updatePassword(Integer id, String oldPassword, String newPassword);

    // 调整用户信用分
    void updateCreditScore(Integer userId, int score);

    // 用户注册，参数为DTO
    UserInfoResponseDTO register(UserRegisterRequestDTO registerDTO);

    // 用户登录，参数为DTO对象
    UserLoginResponseDTO login(UserLoginRequestDTO loginDTO);

    //根据用户ID查询用户信息
    UserInfoResponseDTO getUserInfo(Integer userId);

    // 修改信用分接收UserCreditAdjustRequestDTO参数
    Integer adjustCredit(UserCreditAdjustRequestDTO requestDTO);

    // 修改密码统一使用DTO作为参数
    void updatePassword(UserPasswordUpdateRequestDTO requestDTO);

    // 更新用户信息，声明用户信息更新方法
    UserInfoResponseDTO updateUserInfo(UserInfoUpdateRequestDTO requestDTO);

    // 用户状态变更方法声明，DTO作为参数
    void changeUserStatus(UserStatusChangeRequestDTO requestDTO);

    /**
     * JWT登出逻辑：将令牌加入黑名单，有效期与原令牌一致
     */
    void logout(String token);

    /**
     * 分页查询用户列表
     * 支持按关键词、用户类型、状态等条件筛选
     */
    PageResultDTO<UserItemDTO> getUserPage(UserPageQueryDTO queryDTO);

    // 用户自我删除（验证密码和用户ID匹配）
    void deleteSelfAccount(Integer userId, String password);

    // 管理员删除用户（验证管理员权限）
     CommonResponseDTO<?> deleteUserByAdmin(UserDeleteRequestDTO requestDTO, String currentUsername);

    // 带用户ID的密码修改方法（替换原有的无ID方法）
    void updatePassword(Integer userId, UserPasswordUpdateRequestDTO requestDTO);

    // 带userId 参数的方法（替换原无参方法）
    UserInfoResponseDTO updateUserInfo(Integer userId, UserInfoUpdateRequestDTO requestDTO);

    // 修改为仅接收用户ID的方法（更简洁）
    void deleteUserByAdmin(Integer userId);

    // 获取用户统计信息的方法声明
    String getUserStatistics();

    /**
     * 管理员重置用户密码为默认值（123456）
     * @param userId 目标用户ID
     */
    void resetPasswordByAdmin(Integer userId);
}

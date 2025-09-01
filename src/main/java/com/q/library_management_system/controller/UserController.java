package com.q.library_management_system.controller;

import com.q.library_management_system.dto.request.*;
import com.q.library_management_system.dto.response.CommonResponseDTO;
import com.q.library_management_system.dto.response.PageResultDTO;
import com.q.library_management_system.dto.response.UserInfoResponseDTO;
import com.q.library_management_system.dto.response.UserItemDTO;
import com.q.library_management_system.dto.response.UserLoginResponseDTO;
import com.q.library_management_system.entity.User;
import com.q.library_management_system.exception.BusinessException;
import com.q.library_management_system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;

/**
 * 用户模块控制层
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理接口", description = "包含用户注册、登录、信息管理等功能")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 获取当前登录用户信息
     */
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.findByUsername(username);
    }

    /**
     * 验证当前用户是否为管理员
     */
    private void checkAdminPermission() {
        User currentUser = getCurrentUser();
        if (!User.UserType.admin.equals(currentUser.getUserType())) {
            throw new BusinessException("权限不足：仅管理员可执行此操作");
        }
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @ResponseBody
    @Operation(summary = "用户注册", description = "普通用户注册接口，无需登录")
    public CommonResponseDTO<Integer> register(
            @Valid @RequestBody UserRegisterRequestDTO requestDTO,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return CommonResponseDTO.fail(bindingResult.getFieldError().getDefaultMessage());
        }

        Integer userId = userService.register(requestDTO).getUserId();
        return CommonResponseDTO.success(userId, "注册成功");
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @ResponseBody
    @Operation(summary = "用户登录", description = "用户登录并获取令牌")
    public CommonResponseDTO<UserLoginResponseDTO> login(
            @Valid @RequestBody UserLoginRequestDTO requestDTO,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return CommonResponseDTO.fail(bindingResult.getFieldError().getDefaultMessage());
        }

        UserLoginResponseDTO loginResult = userService.login(requestDTO);
        return CommonResponseDTO.success(loginResult, "登录成功");
    }

    /**
     * 获取用户信息
     * 普通用户只能查询自己，管理员可以查询所有用户
     */
    @GetMapping("/{userId}")
    @ResponseBody
    @Operation(summary = "获取用户信息", description = "普通用户只能查询自己，管理员可以查询所有用户")
    public CommonResponseDTO<UserInfoResponseDTO> getUserInfo(
            @PathVariable @Min(value = 1, message = "用户ID必须为正数") Integer userId) {
        User currentUser = getCurrentUser();

        // 权限校验：非管理员只能查询自己的信息
        if (!User.UserType.admin.equals(currentUser.getUserType()) &&
                !currentUser.getUserId().equals(userId)) {
            throw new BusinessException("权限不足：只能查询自己的信息");
        }

        UserInfoResponseDTO userInfo = userService.getUserInfo(userId);
        return CommonResponseDTO.success(userInfo, "查询成功");
    }

    /**
     * 调整用户信用分（仅管理员）
     */
    @PostMapping("/admin/credit/adjust")
    @ResponseBody
    @Operation(summary = "调整用户信用分", description = "仅管理员可操作")
    public CommonResponseDTO<Integer> adjustCredit(
            @Valid @RequestBody UserCreditAdjustRequestDTO requestDTO,
            BindingResult bindingResult) {
        // 权限校验
        checkAdminPermission();

        // 参数校验
        if (bindingResult.hasErrors()) {
            return CommonResponseDTO.fail(bindingResult.getFieldError().getDefaultMessage());
        }

        Integer newCreditScore = userService.adjustCredit(requestDTO);
        return CommonResponseDTO.success(newCreditScore, "信用分调整成功");
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    @ResponseBody
    @Operation(summary = "修改密码", description = "用户修改自己的密码")
    public CommonResponseDTO<Void> modifyPassword(
            @Valid @RequestBody UserPasswordUpdateRequestDTO requestDTO,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return CommonResponseDTO.fail(bindingResult.getFieldError().getDefaultMessage());
        }

        // 确保只能修改当前登录用户的密码
        User currentUser = getCurrentUser();
        userService.updatePassword(currentUser.getUserId(), requestDTO);
        return CommonResponseDTO.success(null, "密码修改成功，请重新登录");
    }

    /**
     * 更新用户基本信息
     */
    @PutMapping("/info")
    @ResponseBody
    @Operation(summary = "更新用户基本信息", description = "用户更新自己的基本信息")
    public CommonResponseDTO<UserInfoResponseDTO> updateUserInfo(
            @Valid @RequestBody UserInfoUpdateRequestDTO requestDTO,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return CommonResponseDTO.fail(bindingResult.getFieldError().getDefaultMessage());
        }

        // 确保只能更新当前登录用户的信息
        User currentUser = getCurrentUser();
        UserInfoResponseDTO updatedInfo = userService.updateUserInfo(currentUser.getUserId(), requestDTO);
        return CommonResponseDTO.success(updatedInfo, "信息更新成功");
    }

    /**
     * 用户自我删除账号（需验证自身密码）
     */
    @DeleteMapping("/self")
    @ResponseBody
    @Operation(summary = "用户自我删除", description = "用户删除自己的账号，需验证密码")
    public CommonResponseDTO<Void> deleteSelfAccount(
            @Valid @RequestBody UserDeleteRequestDTO requestDTO,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return CommonResponseDTO.fail(bindingResult.getFieldError().getDefaultMessage());
        }

        // 获取当前登录用户ID（确保只能删除自己）
        User currentUser = getCurrentUser();
        userService.deleteSelfAccount(currentUser.getUserId(), requestDTO.getPassword());
        return CommonResponseDTO.success(null, "账号删除成功");
    }

    /**
     * 管理员禁用/启用用户
     */
    @PutMapping("/admin/status")
    @ResponseBody
    @Operation(summary = "修改用户状态", description = "管理员禁用或启用用户账号")
    public CommonResponseDTO<Void> changeUserStatus(
            @Valid @RequestBody UserStatusChangeRequestDTO requestDTO,
            BindingResult bindingResult) {
        // 权限校验
        checkAdminPermission();

        if (bindingResult.hasErrors()) {
            return CommonResponseDTO.fail(bindingResult.getFieldError().getDefaultMessage());
        }

        userService.changeUserStatus(requestDTO);
        return CommonResponseDTO.success(null, "用户状态更新成功");
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    @ResponseBody
    @Operation(summary = "用户登出", description = "用户退出登录")
    public CommonResponseDTO<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new BusinessException("未登录，无需登出");
        }

        userService.logout(token);
        return CommonResponseDTO.success(null, "登出成功");
    }

    /**
     * 分页查询用户列表（仅管理员）
     */
    @GetMapping("/admin/page")
    @ResponseBody
    @Operation(summary = "分页查询用户列表", description = "仅管理员可查询所有用户")
    public CommonResponseDTO<PageResultDTO<UserItemDTO>> getUserPage(
            @Valid UserPageQueryDTO queryDTO,
            BindingResult bindingResult) {
        // 权限校验
        checkAdminPermission();

        if (bindingResult.hasErrors()) {
            return CommonResponseDTO.fail(bindingResult.getFieldError().getDefaultMessage());
        }

        PageResultDTO<UserItemDTO> pageResult = userService.getUserPage(queryDTO);
        return CommonResponseDTO.success(pageResult, "查询成功");
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/current")
    @ResponseBody
    @Operation(summary = "获取当前登录用户信息", description = "获取当前登录用户的基本信息")
    public CommonResponseDTO<UserInfoResponseDTO> getCurrentUserInfo() {
        User currentUser = getCurrentUser();
        UserInfoResponseDTO userInfo = userService.getUserInfo(currentUser.getUserId());
        return CommonResponseDTO.success(userInfo, "查询成功");
    }

    /**
     * 管理员删除用户
     */
    @DeleteMapping("/admin")
    @ResponseBody
    @Operation(summary = "管理员删除用户", description = "仅管理员可删除用户")
    public CommonResponseDTO<Void> deleteUserByAdmin(
            @Validated @RequestBody UserDeleteRequestDTO requestDTO) {
        // 权限校验
        checkAdminPermission();

        userService.deleteUserByAdmin(requestDTO.getUserId());
        return CommonResponseDTO.success(null, "用户删除成功");
    }

    /**
     * 管理员获取用户统计信息
     */
    @GetMapping("/admin/statistics")
    @ResponseBody
    @Operation(summary = "获取用户统计信息", description = "仅管理员可查看用户统计数据")
    public CommonResponseDTO<String> getAdminStatistics() {
        // 权限校验
        checkAdminPermission();

        // 业务逻辑（示例：返回统计数据）
        String statistics = userService.getUserStatistics();
        return CommonResponseDTO.success(statistics, "查询成功");
    }
}



package com.q.library_management_system.controller;

import com.q.library_management_system.dto.response.CommonResponseDTO;
import com.q.library_management_system.entity.ReserveRecord;
import com.q.library_management_system.entity.User;
import com.q.library_management_system.exception.BusinessException;
import com.q.library_management_system.service.ReserveService;
import com.q.library_management_system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;

/**
 * 图书预约管理控制层
 * 权限划分：
 * 1. 普通用户：管理自己的预约（预约图书、取消自己的预约、查询自己的预约记录）
 * 2. 管理员：所有操作权限（含确认预约、处理过期预约、查询所有用户/图书的预约记录）
 */
@RestController
@RequestMapping("/api/reserves")
@Tag(name = "图书预约管理接口", description = "提供图书预约、取消预约、确认预约及过期处理等功能")
public class ReserveController {

    @Autowired
    private ReserveService reserveService;

    @Autowired
    private UserService userService;


    // -------------------------- 权限控制工具方法 --------------------------
    /**
     * 获取当前登录用户信息
     */
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.findByUsername(username);
    }

    /**
     * 校验是否为管理员
     */
    private void checkAdminPermission() {
        User currentUser = getCurrentUser();
        if (!User.UserType.admin.equals(currentUser.getUserType())) {
            throw new BusinessException("权限不足：仅管理员可执行此操作");
        }
    }

    /**
     * 校验预约记录归属权（普通用户只能操作自己的预约）
     * @param reserveId 预约记录ID
     */
    private void checkReservationOwnership(Integer reserveId) {
        // 实际项目中需要在ReserveService添加查询单条预约记录的方法
        ReserveRecord record = reserveService.getReserveById(reserveId);
        User currentUser = getCurrentUser();

        // 非管理员且不是预约所有者，拒绝操作
        if (!User.UserType.admin.equals(currentUser.getUserType())
                && !currentUser.getUserId().equals(record.getUserId())) {
            throw new BusinessException("权限不足：仅能操作自己的预约记录");
        }
    }


    // -------------------------- 普通用户与管理员共用接口 --------------------------
    /**
     * 预约图书
     * 普通用户：只能预约给自己
     * 管理员：可代其他用户预约（通过userId参数指定）
     */
    @PostMapping
    @Operation(summary = "预约图书", description = "普通用户默认预约给自己，管理员可指定用户ID")
    public CommonResponseDTO<ReserveRecord> reserveBook(
            @Parameter(description = "用户ID（管理员可选，普通用户无需填写）")
            @RequestParam(required = false) Integer userId,
            @Parameter(description = "图书ID", required = true)
            @RequestParam @Min(1) Integer bookId,
            @Parameter(description = "预约有效期（1-7天）", required = true)
            @RequestParam @Min(1) @Max(7) int validDays
    ) {
        User currentUser = getCurrentUser();
        // 确定实际预约用户ID（管理员可指定，普通用户只能是自己）
        Integer actualUserId = (User.UserType.admin.equals(currentUser.getUserType()) && userId != null)
                ? userId
                : currentUser.getUserId();

        ReserveRecord reserveRecord = reserveService.reserveBook(actualUserId, bookId, validDays);
        return CommonResponseDTO.success(
                reserveRecord,
                "图书预约成功，预约有效期至：" + reserveRecord.getExpireDate()
        );
    }

    /**
     * 取消预约
     * 普通用户：只能取消自己的预约
     * 管理员：可取消任意用户的预约
     */
    @DeleteMapping("/{reserveId}")
    @Operation(summary = "取消预约", description = "取消指定ID的预约记录")
    public CommonResponseDTO<Void> cancelReservation(
            @Parameter(description = "预约记录ID", required = true)
            @PathVariable @Min(1) Integer reserveId
    ) {
        // 校验权限（普通用户只能取消自己的预约）
        checkReservationOwnership(reserveId);

        User currentUser = getCurrentUser();
        reserveService.cancelReservation(reserveId, currentUser.getUserId());
        return CommonResponseDTO.success(null, "预约取消成功");
    }

    /**
     * 查询用户的预约记录
     * 普通用户：只能查询自己的记录
     * 管理员：可查询任意用户的记录
     */
    @GetMapping("/user")
    @Operation(summary = "查询用户预约记录", description = "可按状态筛选，管理员可指定用户ID")
    public CommonResponseDTO<List<ReserveRecord>> getUserReservations(
            @Parameter(description = "用户ID（管理员可选，普通用户无需填写）")
            @RequestParam(required = false) Integer userId,
            @Parameter(description = "预约状态（waiting/reserved/cancelled，可选）")
            @RequestParam(required = false) ReserveRecord.ReserveStatus status
    ) {
        User currentUser = getCurrentUser();
        Integer actualUserId = (User.UserType.admin.equals(currentUser.getUserType()) && userId != null)
                ? userId
                : currentUser.getUserId();

        List<ReserveRecord> records = reserveService.getUserReservations(actualUserId, status);
        return CommonResponseDTO.success(records, "查询成功，共" + records.size() + "条记录");
    }


    // -------------------------- 管理员专属接口 --------------------------
    /**
     * 确认预约（图书到馆后）
     * 仅管理员可操作，用于通知用户取书
     */
    @PutMapping("/{reserveId}/confirm")
    @Operation(summary = "确认预约", description = "图书到馆后确认预约，仅管理员可操作")
    public CommonResponseDTO<Void> confirmReservation(
            @Parameter(description = "预约记录ID", required = true)
            @PathVariable @Min(1) Integer reserveId
    ) {
        checkAdminPermission();
        reserveService.confirmReservation(reserveId);
        return CommonResponseDTO.success(null, "预约确认成功，请通知用户取书");
    }

    /**
     * 查询图书的预约记录
     * 仅管理员可操作，用于查看指定图书的所有预约情况
     */
    @GetMapping("/book/{bookId}")
    @Operation(summary = "查询图书的预约记录", description = "仅管理员可查询，可按状态筛选")
    public CommonResponseDTO<List<ReserveRecord>> getBookReservations(
            @Parameter(description = "图书ID", required = true)
            @PathVariable @Min(1) Integer bookId,
            @Parameter(description = "预约状态（waiting/reserved/cancelled，可选）")
            @RequestParam(required = false) ReserveRecord.ReserveStatus status
    ) {
        checkAdminPermission();
        List<ReserveRecord> records = reserveService.getBookReservations(bookId, status);
        return CommonResponseDTO.success(records, "查询成功，共" + records.size() + "条记录");
    }

    /**
     * 处理到期预约记录
     * 仅管理员可操作，用于将过期未处理的预约标记为失效
     */
    @PostMapping("/admin/handle-expired")
    @Operation(summary = "处理到期预约", description = "处理单个或所有到期预约，仅管理员可操作")
    public CommonResponseDTO<String> handleExpiredReserves(
            @Parameter(description = "预约记录ID（可选，不填则处理所有到期预约）")
            @RequestParam(required = false) Integer reserveId
    ) {
        checkAdminPermission();
        reserveService.handleExpiredReserves(reserveId);
        String message = (reserveId != null)
                ? "单个到期预约处理完成"
                : "所有到期预约处理完成";
        return CommonResponseDTO.success(null, message);
    }

    /**
     * 查询即将到期的预约
     * 仅管理员可操作，用于提前提醒用户
     */
    @GetMapping("/admin/upcoming-expired")
    @Operation(summary = "查询即将到期的预约", description = "查询未来N小时内将要过期的预约，仅管理员可操作")
    public CommonResponseDTO<List<ReserveRecord>> getUpcomingExpiredReserves(
            @Parameter(description = "未来小时数（1-72小时）", required = true)
            @RequestParam @Min(1) @Max(72) int hours
    ) {
        checkAdminPermission();
        List<ReserveRecord> records = reserveService.getUpcomingExpiredReserves(hours);
        return CommonResponseDTO.success(records, "查询成功，未来" + hours + "小时内有" + records.size() + "条即将到期的预约");
    }

    /**
     * 检查预约是否已过期
     * 仅管理员可操作，用于查询特定预约的过期状态
     */
    @GetMapping("/admin/check-expired/{reserveId}")
    @Operation(summary = "检查预约是否过期", description = "查询指定预约是否已过期，仅管理员可操作")
    public CommonResponseDTO<Boolean> checkReserveExpired(
            @Parameter(description = "预约记录ID", required = true)
            @PathVariable @Min(1) Integer reserveId
    ) {
        checkAdminPermission();
        boolean isExpired = reserveService.isReserveExpired(reserveId);
        return CommonResponseDTO.success(isExpired, isExpired ? "该预约已过期" : "该预约未过期");
    }
}

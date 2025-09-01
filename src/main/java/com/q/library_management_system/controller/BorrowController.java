package com.q.library_management_system.controller;

import com.q.library_management_system.dto.response.CommonResponseDTO;
import com.q.library_management_system.entity.BorrowRecord;
import com.q.library_management_system.entity.User;
import com.q.library_management_system.exception.BusinessException;
import com.q.library_management_system.service.BorrowService;
import com.q.library_management_system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.List;

/**
 * 借阅管理控制层
 * 权限划分：
 * 1. 普通用户：仅能操作自己的借阅记录（借书、还书、续借、查自己的记录、缴自己的罚款）
 * 2. 管理员：拥有所有权限（含查询所有用户/图书记录、处理逾期、批量操作等）
 */
@RestController
@RequestMapping("/api/borrows")
@Tag(name = "借阅管理接口", description = "提供图书借还、续借、逾期处理及罚款缴纳功能")
public class BorrowController {

    @Autowired
    private BorrowService borrowService;

    @Autowired
    private UserService userService;


    // -------------------------- 权限控制工具方法 --------------------------
    /**
     * 获取当前登录用户（从Spring Security上下文）
     */
    private User getCurrentLoginUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.findByUsername(username);
    }

    /**
     * 校验是否为管理员
     */
    private void checkAdminPermission() {
        User currentUser = getCurrentLoginUser();
        if (!User.UserType.admin.equals(currentUser.getUserType())) {
            throw new BusinessException("权限不足：仅管理员可执行此操作");
        }
    }

    /**
     * 校验用户是否有权操作目标记录（自己的记录或管理员）
     * @param targetUserId 目标记录的用户ID
     */
    private void checkRecordPermission(Integer targetUserId) {
        User currentUser = getCurrentLoginUser();
        // 管理员或记录所属用户可操作
        if (!User.UserType.admin.equals(currentUser.getUserType())
                && !currentUser.getUserId().equals(targetUserId)) {
            throw new BusinessException("权限不足：仅能操作自己的借阅记录");
        }
    }


    // -------------------------- 普通用户+管理员共用接口 --------------------------
    /**
     * 借书（普通用户：只能借给自己；管理员：可代借）
     */
    @PostMapping("/borrow")
    @Operation(summary = "图书借阅", description = "普通用户只能借给自己；管理员可指定userId代借（不填则默认当前用户）")
    public CommonResponseDTO<BorrowRecord> borrowBook(
            @Parameter(description = "用户ID（管理员可选填，普通用户无需填，默认当前用户）")
            @RequestParam(required = false) Integer userId,
            @Parameter(description = "图书ID", required = true)
            @RequestParam @Min(value = 1, message = "图书ID必须为正数") Integer bookId,
            @Parameter(description = "借阅天数（1-90天）", required = true)
            @RequestParam @Min(1) @Max(90) Integer days
    ) {
        User currentUser = getCurrentLoginUser();
        // 普通用户只能借给自己，管理员可指定userId
        Integer actualUserId = (User.UserType.admin.equals(currentUser.getUserType()) && userId != null)
                ? userId
                : currentUser.getUserId();

        // 调用服务层借书
        BorrowRecord borrowRecord = borrowService.borrowBook(actualUserId, bookId, days);
        return CommonResponseDTO.success(borrowRecord, "图书借阅成功，到期日：" + borrowRecord.getDueDate());
    }

    /**
     * 还书（普通用户：只能还自己的书；管理员：可代还）
     */
    @PostMapping("/return/{recordId}")
    @Operation(summary = "图书归还", description = "需传入借阅记录ID，自动计算是否逾期及罚款")
    public CommonResponseDTO<BorrowRecord> returnBook(
            @Parameter(description = "借阅记录ID", required = true)
            @PathVariable @Min(value = 1, message = "记录ID必须为正数") Integer recordId
    ) {
        // 先查询记录确认归属，校验权限
        BorrowRecord record = borrowService.getBorrowRecordById(recordId); // 需在Service新增查询单条记录的方法
        checkRecordPermission(record.getUserId());

        // 调用服务层还书
        BorrowRecord updatedRecord = borrowService.returnBook(recordId);
        String msg = updatedRecord.getFineAmount().compareTo(BigDecimal.ZERO) > 0
                ? "图书归还成功，产生逾期罚款：" + updatedRecord.getFineAmount() + "元"
                : "图书归还成功，无逾期罚款";
        return CommonResponseDTO.success(updatedRecord, msg);
    }

    /**
     * 续借（普通用户：只能续自己的书；管理员：可代续）
     */
    @PostMapping("/renew/{recordId}")
    @Operation(summary = "图书续借", description = "需传入借阅记录ID，续借天数1-30天，续借次数受限制")
    public CommonResponseDTO<BorrowRecord> renewBook(
            @Parameter(description = "借阅记录ID", required = true)
            @PathVariable @Min(value = 1, message = "记录ID必须为正数") Integer recordId,
            @Parameter(description = "续借天数（1-30天）", required = true)
            @RequestParam @Min(1) @Max(30) Integer days
    ) {
        // 校验权限
        BorrowRecord record = borrowService.getBorrowRecordById(recordId);
        checkRecordPermission(record.getUserId());

        // 调用服务层续借
        BorrowRecord updatedRecord = borrowService.renewBook(recordId, days);
        return CommonResponseDTO.success(updatedRecord, "续借成功，新到期日：" + updatedRecord.getDueDate());
    }

    /**
     * 缴纳罚款（普通用户：只能缴自己的罚款；管理员：可代缴）
     */
    @PostMapping("/pay/{recordId}")
    @Operation(summary = "缴纳逾期罚款", description = "需传入借阅记录ID，缴纳后罚款状态更新")
    public CommonResponseDTO<String> payPenalty(
            @Parameter(description = "借阅记录ID", required = true)
            @PathVariable @Min(value = 1, message = "记录ID必须为正数") Integer recordId
    ) {
        User currentUser = getCurrentLoginUser();
        // 校验权限（当前用户或管理员）
        BorrowRecord record = borrowService.getBorrowRecordById(recordId);
        checkRecordPermission(record.getUserId());

        // 调用服务层缴罚款
        String result = borrowService.payPenalty(recordId, currentUser.getUserId());
        return CommonResponseDTO.success(result, "罚款缴纳成功");
    }


    // -------------------------- 记录查询接口（权限区分） --------------------------
    /**
     * 查询用户的借阅记录（普通用户：只能查自己的；管理员：可查任意用户的）
     */
    @GetMapping("/user")
    @Operation(summary = "查询用户借阅记录", description = "status可选填（unreturned/returned/overdue），不填则查所有状态")
    public CommonResponseDTO<List<BorrowRecord>> getUserBorrowRecords(
            @Parameter(description = "用户ID（管理员可选填，普通用户无需填，默认当前用户）")
            @RequestParam(required = false) Integer userId,
            @Parameter(description = "借阅状态（unreturned/returned/overdue）")
            @RequestParam(required = false) BorrowRecord.BorrowStatus status
    ) {
        User currentUser = getCurrentLoginUser();
        // 普通用户只能查自己，管理员可指定userId
        Integer actualUserId = (User.UserType.admin.equals(currentUser.getUserType()) && userId != null)
                ? userId
                : currentUser.getUserId();

        // 调用服务层查询
        List<BorrowRecord> records = borrowService.getUserBorrowRecords(actualUserId, status);
        return CommonResponseDTO.success(records, "查询成功，共" + records.size() + "条记录");
    }

    /**
     * 查询图书的借阅记录（仅管理员可查）
     */
    @GetMapping("/book/{bookId}")
    @Operation(summary = "查询图书借阅记录", description = "仅管理员可查，status可选填，不填则查所有状态")
    public CommonResponseDTO<List<BorrowRecord>> getBookBorrowRecords(
            @Parameter(description = "图书ID", required = true)
            @PathVariable @Min(value = 1, message = "图书ID必须为正数") Integer bookId,
            @Parameter(description = "借阅状态（unreturned/returned/overdue）")
            @RequestParam(required = false) BorrowRecord.BorrowStatus status
    ) {
        // 仅管理员可查
        checkAdminPermission();

        // 调用服务层查询
        List<BorrowRecord> records = borrowService.getBookBorrowRecords(bookId, status);
        return CommonResponseDTO.success(records, "查询成功，共" + records.size() + "条记录");
    }

    /**
     * 查询用户的逾期未缴罚款记录（普通用户：查自己的；管理员：查任意用户的）
     */
    @GetMapping("/user/unpaid")
    @Operation(summary = "查询逾期未缴罚款记录")
    public CommonResponseDTO<List<BorrowRecord>> getUnpaidOverdueRecords(
            @Parameter(description = "用户ID（管理员可选填，普通用户无需填）")
            @RequestParam(required = false) Integer userId
    ) {
        User currentUser = getCurrentLoginUser();
        Integer actualUserId = (User.UserType.admin.equals(currentUser.getUserType()) && userId != null)
                ? userId
                : currentUser.getUserId();

        List<BorrowRecord> records = borrowService.getUnpaidOverdueRecords(actualUserId);
        return CommonResponseDTO.success(records, "查询成功，共" + records.size() + "条未缴罚款记录");
    }


    // -------------------------- 管理员专属接口 --------------------------
    /**
     * 处理逾期记录（仅管理员可执行）
     * 1. 传recordId：处理单个未逾期的未归还记录（判断是否逾期并更新状态/罚款）
     * 2. 不传recordId：批量处理所有未逾期的未归还记录
     */
    @PostMapping("/admin/handle-overdue")
    @Operation(summary = "处理逾期记录（管理员专属）", description = "批量或单个处理未逾期的未归还记录")
    public CommonResponseDTO<String> handleOverdueRecords(
            @Parameter(description = "借阅记录ID（可选，不传则处理所有）")
            @RequestParam(required = false) Integer recordId
    ) {
        checkAdminPermission();
        borrowService.handleOverdueRecords(recordId);
        String msg = (recordId != null)
                ? "单个逾期记录处理完成"
                : "所有未逾期的未归还记录处理完成";
        return CommonResponseDTO.success(null, msg);
    }
}


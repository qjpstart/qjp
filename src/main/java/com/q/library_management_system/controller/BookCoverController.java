package com.q.library_management_system.controller;

import com.q.library_management_system.dto.response.CommonResponseDTO;
import com.q.library_management_system.entity.BookCover;
import com.q.library_management_system.entity.User;
import com.q.library_management_system.exception.BusinessException;
import com.q.library_management_system.service.BookCoverService;
import com.q.library_management_system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;



/**
 * 图书封面管理控制器
 */
@RestController
@RequestMapping("/api/covers")
@RequiredArgsConstructor
@Tag(name = "图书封面管理接口", description = "提供图书封面的上传、查询和删除功能")
public class BookCoverController {

    private final BookCoverService bookCoverService;
    private final UserService userService;

    /**
     * 上传或更新图书封面（仅管理员）
     */
    @PostMapping("/{bookId}")
    @Operation(summary = "上传或更新图书封面", description = "仅管理员可操作，支持JPG/PNG格式，大小不超过5MB")
    public CommonResponseDTO<BookCover> uploadCover(
            @Parameter(description = "图书ID", required = true)
            @PathVariable @Min(1) Integer bookId,
            @Parameter(description = "封面图片文件", required = true)
            @RequestParam("file") MultipartFile file) {

        // 权限校验：仅管理员可操作
        checkAdminPermission();

        BookCover bookCover = bookCoverService.uploadOrUpdateCover(bookId, file);
        return CommonResponseDTO.success(bookCover, "封面上传成功");
    }

    /**
     * 获取图书封面（所有登录用户可访问）
     */
    @GetMapping("/book/{bookId}")
    @Operation(summary = "获取图书封面", description = "查询指定图书的封面信息")
    public CommonResponseDTO<BookCover> getBookCover(
            @Parameter(description = "图书ID", required = true)
            @PathVariable @Min(1) Integer bookId) {

        BookCover cover = bookCoverService.getCoverByBookId(bookId);
        return CommonResponseDTO.success(cover, cover != null ? "查询成功" : "该图书暂无封面");
    }

    /**
     * 删除图书封面（仅管理员）
     */
    @DeleteMapping("/{bookId}")
    @Operation(summary = "删除图书封面", description = "仅管理员可操作，删除指定图书的封面")
    public CommonResponseDTO<Void> deleteCover(
            @Parameter(description = "图书ID", required = true)
            @PathVariable @Min(1) Integer bookId) {

        // 权限校验：仅管理员可操作
        checkAdminPermission();

        bookCoverService.deleteCover(bookId);
        return CommonResponseDTO.success(null, "封面删除成功");
    }

    /**
     * 校验当前用户是否为管理员
     */
    private void checkAdminPermission() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userService.findByUsername(username);

        if (!User.UserType.admin.equals(currentUser.getUserType())) {
            throw new BusinessException("权限不足：仅管理员可管理图书封面");
        }
    }
}


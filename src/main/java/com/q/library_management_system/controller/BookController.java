package com.q.library_management_system.controller;

import com.q.library_management_system.dto.request.*;
import com.q.library_management_system.dto.response.BookDetailResponseDTO;
import com.q.library_management_system.dto.response.BookListResponseDTO;
import com.q.library_management_system.dto.response.CommonResponseDTO;
import com.q.library_management_system.dto.response.PageResultDTO;
import com.q.library_management_system.entity.User;
import com.q.library_management_system.exception.BusinessException;
import com.q.library_management_system.service.BookService;
import com.q.library_management_system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.List;

/**
 * 图书模块控制层
 */
@RestController
@RequestMapping("/api/books")
@Tag(name = "图书管理接口", description = "包含图书的增删改查、库存调整、分类管理等功能")
public class BookController {

    @Autowired
    private BookService bookService;

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
     * 新增图书（仅管理员）
     */
    @PostMapping
    @Operation(summary = "新增图书", description = "仅管理员可新增图书，需提供完整图书信息")
    public CommonResponseDTO<Integer> addBook(
            @Valid @RequestBody BookAddRequestDTO requestDTO,
            BindingResult bindingResult) {
        checkAdminPermission();

        if (bindingResult.hasErrors()) {
            return CommonResponseDTO.fail(bindingResult.getFieldError().getDefaultMessage());
        }

        Integer bookId = bookService.addBook(requestDTO);
        return CommonResponseDTO.success(bookId, "图书新增成功");
    }

    /**
     * 批量新增图书（仅管理员）
     */
    @PostMapping("/batch")
    @Operation(summary = "批量新增图书", description = "仅管理员可批量新增图书，适用于批量入库场景")
    public CommonResponseDTO<Integer> batchAddBooks(
            @Valid @RequestBody List<BookAddRequestDTO> requestDTOList,
            BindingResult bindingResult) {
        checkAdminPermission();

        if (bindingResult.hasErrors()) {
            return CommonResponseDTO.fail(bindingResult.getFieldError().getDefaultMessage());
        }
        if (requestDTOList.isEmpty()) {
            return CommonResponseDTO.fail("图书列表不能为空");
        }

        int successCount = bookService.batchAddBooks(requestDTOList);
        return CommonResponseDTO.success(successCount, "批量新增成功，共添加" + successCount + "本图书");
    }

    /**
     * 更新图书信息（仅管理员）
     */
    @PutMapping("/{bookId}")
    @Operation(summary = "更新图书信息", description = "仅管理员可更新图书信息，支持部分字段更新")
    public CommonResponseDTO<Void> updateBook(
            @PathVariable @Min(value = 1, message = "图书ID必须为正数") Integer bookId,
            @Valid @RequestBody BookUpdateRequestDTO requestDTO,
            BindingResult bindingResult) {
        checkAdminPermission();

        if (bindingResult.hasErrors()) {
            return CommonResponseDTO.fail(bindingResult.getFieldError().getDefaultMessage());
        }

        bookService.updateBook(bookId, requestDTO);
        return CommonResponseDTO.success(null, "图书信息更新成功");
    }

    /**
     * 分页查询图书列表（所有登录用户可访问）
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询图书列表", description = "所有登录用户可查询，支持基本条件筛选")
    public CommonResponseDTO<PageResultDTO<BookListResponseDTO>> getBookPage(
            BookSearchRequestDTO searchDTO) {
        PageResultDTO<BookListResponseDTO> pageResult = bookService.getBookList(searchDTO);
        return CommonResponseDTO.success(pageResult, "图书列表查询成功");
    }

    /**
     * 高级搜索图书（所有登录用户可访问）
     */
    @PostMapping("/search/advanced")
    @Operation(summary = "高级搜索图书", description = "所有登录用户可使用，支持多条件组合搜索")
    public CommonResponseDTO<PageResultDTO<BookListResponseDTO>> advancedSearchBooks(
            @Valid @RequestBody BookSearchRequestDTO searchRequest,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return CommonResponseDTO.fail(bindingResult.getFieldError().getDefaultMessage());
        }

        PageResultDTO<BookListResponseDTO> result = bookService.searchBooks(searchRequest);
        return CommonResponseDTO.success(result, "高级搜索成功");
    }

    /**
     * 获取图书详情（所有登录用户可访问）
     */
    @GetMapping("/{bookId}")
    @Operation(summary = "获取图书详情", description = "所有登录用户可查询图书详细信息")
    public CommonResponseDTO<BookDetailResponseDTO> getBookDetail(
            @PathVariable @Min(value = 1, message = "图书ID必须为正数") Integer bookId) {
        BookDetailResponseDTO bookDetail = bookService.getBookDetail(bookId);
        return CommonResponseDTO.success(bookDetail, "查询成功");
    }

    /**
     * 调整图书库存（仅管理员）
     */
    @PostMapping("/stock/adjust")
    @Operation(summary = "调整图书库存", description = "仅管理员可操作，支持入库、出库、盘点、报损等类型")
    public CommonResponseDTO<Integer> adjustStock(
            @Valid @RequestBody BookStockAdjustRequestDTO requestDTO,
            BindingResult bindingResult) {
        checkAdminPermission();

        if (bindingResult.hasErrors()) {
            return CommonResponseDTO.fail(bindingResult.getFieldError().getDefaultMessage());
        }

        int newStock = bookService.adjustStock(requestDTO).getTotalStock();
        return CommonResponseDTO.success(newStock, "库存调整成功，当前库存：" + newStock);
    }

    /**
     * 批量调整图书库存（仅管理员）
     */
    @PostMapping("/stock/batch-adjust")
    @Operation(summary = "批量调整图书库存", description = "仅管理员可操作，适用于批量盘点或入库场景")
    public CommonResponseDTO<Void> batchAdjustStock(
            @Valid @RequestBody List<BookStockAdjustRequestDTO> requestDTOList,
            BindingResult bindingResult) {
        checkAdminPermission();

        if (bindingResult.hasErrors()) {
            return CommonResponseDTO.fail(bindingResult.getFieldError().getDefaultMessage());
        }
        if (requestDTOList.isEmpty()) {
            return CommonResponseDTO.fail("库存调整列表不能为空");
        }

        bookService.batchAdjustStock(requestDTOList);
        return CommonResponseDTO.success(null, "批量库存调整成功");
    }

    /**
     * 为图书绑定分类（仅管理员）
     */
    @PostMapping("/{bookId}/categories")
    @Operation(summary = "绑定图书分类", description = "仅管理员可操作，为图书关联分类")
    public CommonResponseDTO<Void> bindBookCategory(
            @PathVariable @Min(value = 1, message = "图书ID必须为正数") Integer bookId,
            @Valid @RequestBody BookCategoryBindRequestDTO requestDTO,
            BindingResult bindingResult) {
        checkAdminPermission();

        if (bindingResult.hasErrors()) {
            return CommonResponseDTO.fail(bindingResult.getFieldError().getDefaultMessage());
        }

        bookService.bindCategory(bookId, requestDTO.getCategoryId());
        return CommonResponseDTO.success(null, "图书分类绑定成功");
    }

    /**
     * 删除图书（仅管理员）
     */
    @DeleteMapping("/{bookId}")
    @Operation(summary = "删除图书", description = "仅管理员可操作，删除前会校验是否存在未归还的借阅")
    public CommonResponseDTO<Void> deleteBook(
            @PathVariable @Min(value = 1, message = "图书ID必须为正数") Integer bookId) {
        checkAdminPermission();

        bookService.deleteBook(bookId);
        return CommonResponseDTO.success(null, "图书删除成功");
    }

    /**
     * 批量删除图书（仅管理员）
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除图书", description = "仅管理员可操作，适用于批量清理图书")
    public CommonResponseDTO<Void> batchDeleteBooks(
            @Valid @RequestBody BookBatchDeleteRequestDTO requestDTO,
            BindingResult bindingResult) {
        checkAdminPermission();

        // 校验参数（此时能捕获“列表为空”“ID为负数”等错误）
        if (bindingResult.hasErrors()) {
            return CommonResponseDTO.fail(bindingResult.getFieldError().getDefaultMessage());
        }

        // 从 DTO 中获取合法的 bookIds
        List<Integer> bookIds = requestDTO.getBookIds();
        bookService.batchDeleteBooks(bookIds);

        // 注意：这里建议用实际删除成功的数量（而非传入的数量），避免“部分删除失败”的误导
        return CommonResponseDTO.success(null, "批量删除成功，共删除" + bookIds.size() + "本图书");
    }
}



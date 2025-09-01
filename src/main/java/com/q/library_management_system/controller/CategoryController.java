package com.q.library_management_system.controller;

import com.q.library_management_system.dto.request.CategoryAddRequestDTO;
import com.q.library_management_system.dto.request.CategoryBatchDeleteRequestDTO;
import com.q.library_management_system.dto.request.CategoryUpdateRequestDTO;
import com.q.library_management_system.dto.response.CategoryResponseDTO;
import com.q.library_management_system.dto.response.CategoryTreeResponseDTO;
import com.q.library_management_system.dto.response.CommonResponseDTO;
import com.q.library_management_system.entity.User;
import com.q.library_management_system.exception.BusinessException;
import com.q.library_management_system.service.CategoryService;
import com.q.library_management_system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.List;

/**
 * 图书分类控制层
 * 核心功能：分类新增、修改、删除（批量）、查询（详情/树形/列表）
 * 权限控制：写操作（增删改）仅管理员可访问，读操作（查询）所有登录用户可访问
 */
@RestController
@RequestMapping("/api/categories")
@Tag(name = "图书分类管理接口", description = "提供图书分类的增删改查及树形结构查询功能")
public class CategoryController {

    // 注入业务层和用户服务（用于权限校验）
    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserService userService;


    // -------------------------- 权限控制工具方法 --------------------------
    /**
     * 获取当前登录用户信息
     */
    private User getCurrentUser() {
        // 从Spring Security上下文获取登录用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.findByUsername(username);
    }

    /**
     * 校验当前用户是否为管理员（非管理员抛出权限不足异常）
     */
    private void checkAdminPermission() {
        User currentUser = getCurrentUser();
        // 假设User实体的UserType枚举中定义了"admin"角色（需与你的User类保持一致）
        if (!User.UserType.admin.equals(currentUser.getUserType())) {
            throw new BusinessException("权限不足：仅管理员可执行此操作");
        }
    }


    // -------------------------- 分类写操作（仅管理员） --------------------------
    /**
     * 新增图书分类（仅管理员）
     */
    @PostMapping
    @Operation(summary = "新增图书分类", description = "仅管理员可新增分类，支持一级/二级分类（parentId=0为一级）")
    public CommonResponseDTO<Integer> addCategory(
            @Valid @RequestBody CategoryAddRequestDTO requestDTO,
            BindingResult bindingResult
    ) {
        // 1. 校验管理员权限
        checkAdminPermission();

        // 2. 校验请求参数（如分类名称长度、父ID合法性）
        if (bindingResult.hasErrors()) {
            return CommonResponseDTO.fail(bindingResult.getFieldError().getDefaultMessage());
        }

        // 3. 调用服务层新增分类，返回新增分类ID
        Integer newCategoryId = categoryService.addCategory(requestDTO);
        return CommonResponseDTO.success(newCategoryId, "分类新增成功，分类ID：" + newCategoryId);
    }

    /**
     * 修改图书分类（仅管理员）
     */
    @PutMapping
    @Operation(summary = "修改图书分类", description = "仅管理员可修改分类，需传入完整分类信息（含分类ID）")
    public CommonResponseDTO<Void> updateCategory(
            @Valid @RequestBody CategoryUpdateRequestDTO requestDTO,
            BindingResult bindingResult
    ) {
        // 1. 校验管理员权限
        checkAdminPermission();

        // 2. 校验请求参数（如分类ID为正数、名称非空）
        if (bindingResult.hasErrors()) {
            return CommonResponseDTO.fail(bindingResult.getFieldError().getDefaultMessage());
        }

        // 3. 调用服务层修改分类
        categoryService.updateCategory(requestDTO);
        return CommonResponseDTO.success(null, "分类修改成功，分类ID：" + requestDTO.getCategoryId());
    }

    /**
     * 删除单个图书分类（仅管理员）
     */
    @DeleteMapping("/{categoryId}")
    @Operation(summary = "删除单个分类", description = "仅管理员可删除，删除前会校验：1.分类是否存在 2.是否有子分类 3.是否被图书引用")
    public CommonResponseDTO<Void> deleteCategory(
            @Parameter(description = "分类ID，必须为正数")
            @PathVariable @Min(value = 1, message = "分类ID必须为正数") Integer categoryId
    ) {
        // 1. 校验管理员权限
        checkAdminPermission();

        // 2. 调用服务层删除分类
        categoryService.deleteCategory(categoryId);
        return CommonResponseDTO.success(null, "分类删除成功，分类ID：" + categoryId);
    }

    /**
     * 批量删除图书分类（仅管理员）
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除分类", description = "仅管理员可批量删除，支持同时删除多个分类，校验逻辑同单个删除")
    public CommonResponseDTO<Integer> batchDeleteCategories(
            @Valid @RequestBody CategoryBatchDeleteRequestDTO requestDTO,
            BindingResult bindingResult
    ) {
        // 1. 校验管理员权限
        checkAdminPermission();

        // 2. 校验请求参数（如分类ID列表非空、ID为正数）
        if (bindingResult.hasErrors()) {
            return CommonResponseDTO.fail(bindingResult.getFieldError().getDefaultMessage());
        }

        // 3. 调用服务层批量删除，返回成功删除数量
        int successCount = categoryService.batchDeleteCategories(requestDTO);
        return CommonResponseDTO.success(successCount, "批量删除成功，共删除" + successCount + "个分类");
    }


    // -------------------------- 分类读操作（所有登录用户） --------------------------
    /**
     * 查询分类详情（所有登录用户）
     */
    @GetMapping("/{categoryId}")
    @Operation(summary = "查询分类详情", description = "所有登录用户可查询，返回分类完整信息（含父分类名称）")
    public CommonResponseDTO<CategoryResponseDTO> getCategoryDetail(
            @Parameter(description = "分类ID，必须为正数")
            @PathVariable @Min(value = 1, message = "分类ID必须为正数") Integer categoryId
    ) {
        // 无需权限校验（但需确保用户已登录，可通过Spring Security全局配置控制）
        CategoryResponseDTO detailDTO = categoryService.getCategoryDetail(categoryId);
        return CommonResponseDTO.success(detailDTO, "分类详情查询成功");
    }

    /**
     * 查询所有分类列表（平级，所有登录用户）
     */
    @GetMapping("/list")
    @Operation(summary = "查询所有分类列表", description = "所有登录用户可查询，返回平级分类列表（含父分类ID，不含子分类）")
    public CommonResponseDTO<List<CategoryResponseDTO>> getAllCategories() {
        List<CategoryResponseDTO> categoryList = categoryService.getAllCategories();
        return CommonResponseDTO.success(categoryList, "分类列表查询成功，共" + categoryList.size() + "个分类");
    }

    /**
     * 查询分类树形结构（层级，所有登录用户）
     */
    @GetMapping("/tree")
    @Operation(summary = "查询分类树形结构", description = "所有登录用户可查询，返回层级结构（一级分类包含二级分类列表，便于前端展示）")
    public CommonResponseDTO<List<CategoryTreeResponseDTO>> getCategoryTree() {
        List<CategoryTreeResponseDTO> categoryTree = categoryService.getCategoryTree();
        return CommonResponseDTO.success(categoryTree, "分类树形结构查询成功");
    }
}


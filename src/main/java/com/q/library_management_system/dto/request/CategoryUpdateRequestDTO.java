package com.q.library_management_system.dto.request;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 图书分类修改请求DTO
 */
public class CategoryUpdateRequestDTO {

    /**
     * 分类ID（必须为正数）
     */
    @Min(value = 1, message = "分类ID必须为正数")
    private Integer categoryId;

    /**
     * 分类名称（非空，长度2-30位）
     */
    @NotBlank(message = "分类名称不能为空")
    @Size(min = 2, max = 30, message = "分类名称长度需为2-30位")
    private String categoryName;

    /**
     * 父分类ID（0表示一级分类，不能为负数）
     * 注意：修改时不建议变更父分类，如需变更需确保无循环依赖
     */
    @Min(value = 0, message = "父分类ID不能为负数")
    private Integer parentId = 0;

    /**
     * 分类描述（可选，最多200字）
     */
    @Size(max = 200, message = "分类描述不能超过200字")
    private String description;

    // 手动生成getter和setter
    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}


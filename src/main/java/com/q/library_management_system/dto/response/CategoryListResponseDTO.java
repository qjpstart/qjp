package com.q.library_management_system.dto.response;

import java.util.Date;
import java.util.List;

/**
 * 图书分类列表响应DTO
 * 用于分类列表展示，支持树形结构（包含子分类）
 */
public class CategoryListResponseDTO {

    /**
     * 分类ID
     */
    private Integer categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 父分类ID（0表示一级分类）
     */
    private Integer parentId;

    /**
     * 父分类名称（用于前端显示层级关系）
     */
    private String parentName;

    /**
     * 分类下的图书数量
     */
    private Integer bookCount;

    /**
     * 子分类列表（如果是一级分类，包含其下的二级分类）
     */
    private List<CategoryListResponseDTO> children;

    /**
     * 创建时间
     */
    private Date createTime;

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

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public Integer getBookCount() {
        return bookCount;
    }

    public void setBookCount(Integer bookCount) {
        this.bookCount = bookCount;
    }

    public List<CategoryListResponseDTO> getChildren() {
        return children;
    }

    public void setChildren(List<CategoryListResponseDTO> children) {
        this.children = children;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}


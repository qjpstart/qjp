package com.q.library_management_system.dto.request;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;
import javax.validation.constraints.Min;

/**
 * 图书分类新增请求DTO
 */
public class CategoryAddRequestDTO {

    /**
     * 分类名称（如“计算机科学”“文学小说”）
     * 非空，长度2-30位
     */
    @NotBlank(message = "分类名称不能为空")
    @Length(min = 2, max = 30, message = "分类名称长度需为2-30位")
    private String categoryName;

    /**
     * 父分类ID（0表示一级分类，非0表示二级分类）
     * 不能为负数
     */
    @Min(value = 0, message = "父分类ID不能为负数")
    private Integer parentId = 0;

    /**
     * 分类描述（可选，最多200字）
     */
    @Length(max = 200, message = "分类描述不能超过200字")
    private String description;

    // 手动生成getter和setter
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


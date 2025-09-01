package com.q.library_management_system.dto.response;

import lombok.Data;
import java.util.List;

/**
 * 分类树形结构DTO（用于层级展示，如“一级分类→二级分类”）
 */
@Data
public class CategoryTreeResponseDTO {
    private Integer categoryId;
    private String categoryName;
    private List<CategoryTreeResponseDTO> children; // 子分类列表
}

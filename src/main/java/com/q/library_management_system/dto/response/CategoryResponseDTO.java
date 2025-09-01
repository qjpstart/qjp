package com.q.library_management_system.dto.response;

import lombok.Data;

/**
 * 分类详情/列表响应DTO
 */
@Data
public class CategoryResponseDTO {
    private Integer categoryId; // 分类ID
    private String categoryName; // 分类名称
    private Integer parentId; // 父分类ID（Integer类型）
    private String parentName; // 父分类名称（用于前端展示，如“计算机科学”）
    private String descript; // 分类描述
}


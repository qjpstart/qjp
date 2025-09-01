package com.q.library_management_system.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;


/**
 * 分类批量删除请求DTO
 */
@Data
public class CategoryBatchDeleteRequestDTO {
    /**
     * 分类ID列表（非空，且ID为正数）
     */
    @NotEmpty(message = "分类ID列表不能为空")
    private List<@Min(value = 1, message = "分类ID必须为正数") Integer> categoryIds;
}

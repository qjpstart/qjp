package com.q.library_management_system.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import javax.validation.constraints.NotNull;

import java.util.List;

/**
 * 图书与分类绑定请求DTO
 * 用于接收图书ID和需要绑定的分类ID列表
 */
@Data
public class BookCategoryBindRequestDTO {

    /**
     * 图书ID（必须存在）
     */
    @NotNull(message = "图书ID不能为空")
    private Integer bookId;

    /**
     * 分类ID列表（至少绑定一个分类）
     */
    @NotEmpty(message = "分类ID列表不能为空")
    private Integer categoryId;
}

package com.q.library_management_system.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import javax.validation.Valid;
import java.util.List;

@Data
public class BookBatchDeleteRequestDTO {
    @NotEmpty(message = "图书ID列表不能为空")
    @Valid
    private List<@Min(value = 1, message = "图书ID必须为正数") Integer> bookIds;
    // getter/setter
}


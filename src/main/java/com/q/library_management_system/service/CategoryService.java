package com.q.library_management_system.service;

import com.q.library_management_system.dto.request.CategoryAddRequestDTO;
import com.q.library_management_system.dto.request.CategoryBatchDeleteRequestDTO;
import com.q.library_management_system.dto.request.CategoryUpdateRequestDTO;
import com.q.library_management_system.dto.response.CategoryResponseDTO;
import com.q.library_management_system.dto.response.CategoryTreeResponseDTO;
import com.q.library_management_system.entity.Category;
import java.util.List;

public interface CategoryService {
    // 新增分类
    Integer addCategory(CategoryAddRequestDTO requestDTO);

    // 根据ID查询分类
    Category getCategoryById(Integer id);

    // 查询分类详情
    CategoryResponseDTO getCategoryDetail(Integer categoryId);

    // 查询所有分类列表
    List<CategoryResponseDTO> getAllCategories();

    // 查询分类树形结构
    List<CategoryTreeResponseDTO> getCategoryTree();

    // 修改分类
    void updateCategory(CategoryUpdateRequestDTO requestDTO);

    // 删除单个分类（需检查是否有关联图书）
    void deleteCategory(Integer categoryId);

    // 批量删除分类
    int batchDeleteCategories(CategoryBatchDeleteRequestDTO requestDTO);
}


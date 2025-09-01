package com.q.library_management_system.service.impl;

import com.q.library_management_system.dto.request.CategoryAddRequestDTO;
import com.q.library_management_system.dto.request.CategoryBatchDeleteRequestDTO;
import com.q.library_management_system.dto.request.CategoryUpdateRequestDTO;
import com.q.library_management_system.dto.response.CategoryResponseDTO;
import com.q.library_management_system.dto.response.CategoryTreeResponseDTO;
import com.q.library_management_system.entity.Category;
import com.q.library_management_system.exception.BusinessException;
import com.q.library_management_system.repository.BookRepository;
import com.q.library_management_system.repository.CategoryRepository;
import com.q.library_management_system.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository; // 用于检查关联图书


    @Override
    public Category getCategoryById(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("分类不存在：" + id));
    }


    // -------------------------- 新增分类 --------------------------
    @Override
    @Transactional(rollbackFor = Exception.class) // 事务控制：任何异常回滚
    public Integer addCategory(CategoryAddRequestDTO requestDTO) {
        // 1. 校验分类名称是否重复（同一层级不允许重名）
        if (categoryRepository.existsByCategoryNameAndParentId(
                requestDTO.getCategoryName(), requestDTO.getParentId())) {
            throw new BusinessException("当前层级已存在同名分类：" + requestDTO.getCategoryName());
        }

        // 2. 校验父分类是否存在（非一级分类时）
        Integer parentId = requestDTO.getParentId();
        if (parentId != 0 && !categoryRepository.existsById(parentId)) {
            throw new BusinessException("父分类不存在：" + parentId);
        }

        // 3. DTO转换为实体（注意：实体字段需与DTO匹配，如description对应）
        Category category = new Category();
        BeanUtils.copyProperties(requestDTO, category); // 自动映射同名字段

        // 4. 保存分类并返回新增ID
        Category savedCategory = categoryRepository.save(category);
        return savedCategory.getCategoryId();
    }


    // -------------------------- 修改分类 --------------------------
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(CategoryUpdateRequestDTO requestDTO) {
        // 1. 校验当前分类是否存在
        Integer categoryId = requestDTO.getCategoryId();
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException("分类不存在：" + categoryId));

        // 2. 校验分类名称是否重复（排除自身，且同一层级不允许重名）
        if (!category.getCategoryName().equals(requestDTO.getCategoryName()) // 名称变更时才校验
                && categoryRepository.existsByCategoryNameAndParentId(
                requestDTO.getCategoryName(), requestDTO.getParentId())) {
            throw new BusinessException("当前层级已存在同名分类：" + requestDTO.getCategoryName());
        }

        // 3. 校验父分类合法性（防循环依赖：不能将自己设为父分类，也不能设为子分类的子分类）
        Integer newParentId = requestDTO.getParentId();
        if (newParentId.equals(categoryId)) {
            throw new BusinessException("不能将分类自身设为父分类");
        }
        if (newParentId != 0 && hasChild(categoryId, newParentId)) {
            throw new BusinessException("不能将子分类设为父分类（会导致循环依赖）");
        }

        // 4. 校验父分类是否存在（非一级分类时）
        if (newParentId != 0 && !categoryRepository.existsById(newParentId)) {
            throw new BusinessException("父分类不存在：" + newParentId);
        }

        // 5. 更新分类信息
        BeanUtils.copyProperties(requestDTO, category); // 覆盖修改的字段
        categoryRepository.save(category);
    }


    // -------------------------- 删除单个分类 --------------------------
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Integer categoryId) {
        // 1. 校验分类是否存在
        if (!categoryRepository.existsById(categoryId)) {
            throw new BusinessException("分类不存在：" + categoryId);
        }

        // 2. 校验是否有子分类（有子分类不允许删除，需先删子分类）
        if (categoryRepository.countByParentId(categoryId) > 0) {
            throw new BusinessException("分类存在子分类，无法删除（请先删除子分类）");
        }

        // 3. 校验是否被图书引用（被引用不允许删除，避免数据关联异常）
        if (categoryRepository.isUsedByBook(categoryId)) {
            throw new BusinessException("分类已被图书引用，无法删除（请先修改图书分类）");
        }

        // 4. 执行删除
        categoryRepository.deleteById(categoryId);
    }


    // -------------------------- 批量删除分类 --------------------------
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchDeleteCategories(CategoryBatchDeleteRequestDTO requestDTO) {
        // 1. 校验分类ID列表非空（前端已校验，此处双重保险）
        List<Integer> categoryIds = requestDTO.getCategoryIds();
        if (categoryIds.isEmpty()) {
            throw new BusinessException("分类ID列表不能为空");
        }

        // 2. 筛选存在的分类ID（排除无效ID）
        List<Integer> existingIds = categoryRepository.findExistingIds(categoryIds);
        if (existingIds.isEmpty()) {
            throw new BusinessException("所选分类均不存在");
        }

        // 3. 筛选不允许删除的分类（有子分类/被图书引用）
        List<Integer> forbiddenIds = new ArrayList<>();
        for (Integer id : existingIds) {
            if (categoryRepository.countByParentId(id) > 0) {
                forbiddenIds.add(id);
            } else if (categoryRepository.isUsedByBook(id)) {
                forbiddenIds.add(id);
            }
        }
        if (!forbiddenIds.isEmpty()) {
            throw new BusinessException("以下分类无法删除：" + forbiddenIds + "（存在子分类或被图书引用）");
        }

        // 4. 批量删除并返回成功数量
        categoryRepository.deleteAllById(existingIds);
        return existingIds.size();
    }


    // -------------------------- 查询分类详情 --------------------------
    @Override
    public CategoryResponseDTO getCategoryDetail(Integer categoryId) {
        // 1. 校验分类是否存在
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException("分类不存在：" + categoryId));

        // 2. 转换为响应DTO，并补充父分类名称（便于前端展示）
        CategoryResponseDTO detailDTO = new CategoryResponseDTO();
        BeanUtils.copyProperties(category, detailDTO);

        // 3. 若为二级分类，查询父分类名称
        Integer parentId = category.getParentId();
        if (parentId != 0) {
            categoryRepository.findById(parentId)
                    .ifPresent(parentCategory -> detailDTO.setParentName(parentCategory.getCategoryName()));
        }

        return detailDTO;
    }


    // -------------------------- 查询所有分类列表 --------------------------
    @Override
    public List<CategoryResponseDTO> getAllCategories() {
        // 1. 查询所有分类（平级列表）
        List<Category> allCategories = categoryRepository.findAll();

        // 2. 构建父分类名称映射（避免循环查询数据库，提升性能）
        Map<Integer, String> parentNameMap = allCategories.stream()
                .collect(Collectors.toMap(
                        Category::getCategoryId,  // key：分类ID
                        Category::getCategoryName // value：分类名称
                ));

        // 3. 转换为响应DTO列表
        return allCategories.stream().map(category -> {
            CategoryResponseDTO dto = new CategoryResponseDTO();
            BeanUtils.copyProperties(category, dto);
            // 补充父分类名称（一级分类父名称为null或空串）
            Integer parentId = category.getParentId();
            if (parentId != 0) {
                dto.setParentName(parentNameMap.get(parentId));
            }
            return dto;
        }).collect(Collectors.toList());
    }


    // -------------------------- 查询分类树形结构 --------------------------
    @Override
    public List<CategoryTreeResponseDTO> getCategoryTree() {
        // 1. 查询所有分类
        List<Category> allCategories = categoryRepository.findAll();

        // 2. 按父分类ID分组（key：父分类ID，value：子分类列表）
        Map<Integer, List<Category>> parentGroup = allCategories.stream()
                .collect(Collectors.groupingBy(Category::getParentId));

        // 3. 从一级分类（parentId=0）开始递归构建树形结构
        return buildCategoryTree(0, parentGroup);
    }


    // -------------------------- 工具方法 --------------------------
    /**
     * 递归构建分类树形结构
     * @param parentId 父分类ID（初始为0，即一级分类）
     * @param parentGroup 按父ID分组的分类集合
     */
    private List<CategoryTreeResponseDTO> buildCategoryTree(Integer parentId, Map<Integer, List<Category>> parentGroup) {
        List<CategoryTreeResponseDTO> treeNodes = new ArrayList<>();

        // 获取当前父分类下的所有子分类
        List<Category> children = parentGroup.getOrDefault(parentId, new ArrayList<>());

        // 遍历子分类，递归构建下级树形
        for (Category child : children) {
            CategoryTreeResponseDTO treeNode = new CategoryTreeResponseDTO();
            BeanUtils.copyProperties(child, treeNode);
            // 递归查询当前分类的子分类
            treeNode.setChildren(buildCategoryTree(child.getCategoryId(), parentGroup));
            treeNodes.add(treeNode);
        }

        return treeNodes;
    }

    /**
     * 递归检查分类是否为目标ID的子分类（防循环依赖）
     * @param targetId 目标分类ID（当前要修改的分类）
     * @param currentParentId 待校验的父分类ID
     */
    private boolean hasChild(Integer targetId, Integer currentParentId) {
        // 终止条件：父分类为一级分类（parentId=0），不可能是子分类
        if (currentParentId == 0) {
            return false;
        }

        // 找到目标ID，说明currentParentId是targetId的子分类
        if (currentParentId.equals(targetId)) {
            return true;
        }

        // 递归查询上级父分类
        Category parentCategory = categoryRepository.findById(currentParentId)
                .orElseThrow(() -> new BusinessException("父分类不存在：" + currentParentId));
        return hasChild(targetId, parentCategory.getParentId());
    }
}


package com.q.library_management_system.repository;

import com.q.library_management_system.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    // 根据分类名称查询
    Optional<Category> findByCategoryName(String categoryName);

    // 根据父分类ID查询子分类（用于多级分类）
    List<Category> findByParentId(String parentId);

    // 检查分类名称是否已存在
    boolean existsByCategoryName(String categoryName);

    // 检查分类id是否已存在
    boolean existsByCategoryId(Integer categoryId);

    /**
     * 校验同一层级是否存在同名分类（新增/修改用）
     */
    boolean existsByCategoryNameAndParentId(String categoryName, Integer parentId);

    /**
     * 统计子分类数量（删除校验用）
     */
    long countByParentId(Integer parentId);

    /**
     * 批量查询存在的分类ID（批量删除用）
     */
    @Query("SELECT c.categoryId FROM Category c WHERE c.categoryId IN :ids")
    List<Integer> findExistingIds(@Param("ids") List<Integer> categoryIds);

    /**
     * 检查分类是否被图书引用（删除校验用）
     * 注：需确保Book实体中有categoryId字段关联分类
     */
    @Query("SELECT COUNT(b) > 0 FROM Book b WHERE b.categoryId = :categoryId")
    boolean isUsedByBook(@Param("categoryId") Integer categoryId);
}

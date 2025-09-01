package com.q.library_management_system.dto.response;

import java.util.List;

/**
 * 通用分页结果DTO
 * 用于所有列表查询接口的分页数据封装
 */
public class PageResultDTO<T> {
    // 总数据条数
    private Long totalCount;

    // 总页数
    private Integer totalPages;

    // 当前页码（前端传递的页码，从1开始）
    private Integer currentPage;

    // 每页条数
    private Integer pageSize;

    // 当前页的数据列表
    private List<T> list;

    // 构造方法（全参数）
    public PageResultDTO(Long totalCount, Integer totalPages,
                         Integer currentPage, Integer pageSize, List<T> list) {
        this.totalCount = totalCount;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.list = list;
    }

    // 无参构造函数
    public PageResultDTO() {
    }

    // 所有字段的getter和setter
    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public Integer getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    /**
     * 静态构建方法：创建分页结果DTO
     * @param list 当前页数据列表
     * @param total 总记录数
     * @param currentPage 当前页码（1-based）
     * @param pageSize 每页条数
     * @param <T> 数据类型
     * @return 构建好的分页结果DTO
     */
    public static <T> PageResultDTO<T> build(List<T> list, long total, int currentPage, int pageSize) {
        PageResultDTO<T> result = new PageResultDTO<>();
        result.list = list;
        result.totalCount = total;
        result.currentPage = currentPage;
        result.pageSize = pageSize;
        // 计算总页数（向上取整）
        result.totalPages = (int) Math.ceil((double) total / pageSize);
        return result;
    }
}

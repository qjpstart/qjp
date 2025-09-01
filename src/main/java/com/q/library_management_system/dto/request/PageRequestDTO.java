package com.q.library_management_system.dto.request;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 通用分页查询请求DTO
 * 兼容Java 7环境，不依赖Java 8及以上特性
 * 所有分页查询接口共用，接收页码、页大小和关键字搜索参数。
 */
public class PageRequestDTO {
    /** 页码（默认1，最小为1） */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;

    /** 每页条数（默认10，最小为1） */
    @NotNull(message = "页大小不能为空")
    @Min(value = 1, message = "页大小最小为1")
    private Integer pageSize = 10;

    /** 关键字搜索（可选，如书名、用户名等） */
    private String keyword;

    // Java 7不支持lombok的@Data，需手动生成getter和setter
    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}

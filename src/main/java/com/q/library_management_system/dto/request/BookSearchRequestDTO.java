package com.q.library_management_system.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;


/**
 * 图书高级搜索请求DTO
 * 支持多条件组合查询（如按分类、作者、出版社、出版日期范围）
 */
public class BookSearchRequestDTO extends PageRequestDTO { // 继承分页DTO
    /** 分类ID（可选） */
    @Min(value = 0, message = "分类ID不能为负数")
    private Integer categoryId;

    /** 作者（可选，模糊匹配） */
    private String author;

    /** 出版社（可选，模糊匹配） */
    private String publisher;

    /** 出版日期起始（格式：yyyy-MM-dd，可选） */
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$|^$", message = "出版日期起始格式应为yyyy-MM-dd")
    private String publishDateStart;

    /** 出版日期结束（格式：yyyy-MM-dd，可选） */
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$|^$", message = "出版日期结束格式应为yyyy-MM-dd")
    private String publishDateEnd;

    /** 是否可借（true=只查可借库存>0的图书，可选） */
    private Boolean available;

    // getter/setter
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    public String getPublishDateStart() { return publishDateStart; }
    public void setPublishDateStart(String publishDateStart) { this.publishDateStart = publishDateStart; }
    public String getPublishDateEnd() { return publishDateEnd; }
    public void setPublishDateEnd(String publishDateEnd) { this.publishDateEnd = publishDateEnd; }
    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }
}

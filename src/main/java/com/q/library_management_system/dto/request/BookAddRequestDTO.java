package com.q.library_management_system.dto.request;

import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import java.time.LocalDate;

/**
 * 图书新增请求DTO
 * 接收新增图书的参数，关联分类 ID（而非分类实体），确保参数合法性。
 */
@Data
public class BookAddRequestDTO {

    /** 书名 */
    @NotBlank(message = "书名不能为空")
    @Length(max = 100, message = "书名长度不能超过100位")
    private String bookName;

    /** 作者 */
    @NotBlank(message = "作者不能为空")
    @Length(max = 50, message = "作者名称不能超过50位")
    private String author;

    /** ISBN编号（唯一） */
    @NotBlank(message = "ISBN不能为空")
    @Pattern(regexp = "^[0-9-]{10,17}$", message = "ISBN格式不正确（10-17位数字或短横线）")
    private String isbn;

    /** 出版社 */
    @NotBlank(message = "出版社不能为空")
    @Length(max = 50, message = "出版社名称不能超过50位")
    private String publisher;

    // 出版日期：名称必须是publisherDate，与实体类匹配
    @NotNull(message = "出版日期不能为空")
    private LocalDate publisherDate;

    /** 分类ID（关联分类表） */
    @NotNull(message = "分类ID不能为空")
    @Min(value = 1, message = "图书ID必须为正数")
    private Integer categoryId;

    /** 总库存 */
    @NotNull(message = "总库存不能为空")
    @Min(value = 1, message = "总库存必须为正数")
    private Integer totalStock;

    /** 可借库存（默认等于总库存，不能超过总库存） */
    @NotNull(message = "可借库存不能为空")
    @Min(value = 0, message = "可借库存不能为负数")
    private Integer availableStock;

    /** 馆藏位置（如A区1架） */
    @Length(max = 20, message = "馆藏位置不能超过20位")
    private String location;

    /** 图书简介（可选，最多500字） */
    @Length(max = 500, message = "图书简介不能超过500字")
    private String description;

    // 必须提供getter方法（否则服务层无法获取字段值）
    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    // 关键：添加bookName的getter方法
    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public LocalDate getPublisherDate() {
        return publisherDate;
    }

    public void setPublisherDate(LocalDate publisherDate) {
        this.publisherDate = publisherDate;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getTotalStock() {
        return totalStock;
    }

    public void setTotalStock(Integer totalStock) {
        this.totalStock = totalStock;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}


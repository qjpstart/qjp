package com.q.library_management_system.dto.request;

import java.time.LocalDate;

public class BookUpdateRequestDTO {
    private String bookName;        // 图书名称
    private String author;          // 作者
    private String publisher;       // 出版社
    private LocalDate publisherDate;// 出版日期
    private Integer categoryId;     // 分类ID
    private String location;        // 馆藏位置

    // 默认构造函数
    public BookUpdateRequestDTO() {
    }

    // 带参数的构造函数
    public BookUpdateRequestDTO(String bookName, String author, String publisher,
                                LocalDate publisherDate, Integer categoryIde, String location) {
        this.bookName = bookName;
        this.author = author;
        this.publisher = publisher;
        this.publisherDate = publisherDate;
        this.categoryId = categoryIde;
        this.location = location;
    }

    // Getter 和 Setter 方法（严格遵循JavaBean规范）
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    // toString方法（便于日志打印和调试）
    @Override
    public String toString() {
        return "BookUpdateRequestDTO{" +
                "bookName='" + bookName + '\'' +
                ", author='" + author + '\'' +
                ", publisher='" + publisher + '\'' +
                ", publisherDate=" + publisherDate +
                ", categoryId=" + categoryId +
                ", location='" + location + '\'' +
                '}';
    }
}
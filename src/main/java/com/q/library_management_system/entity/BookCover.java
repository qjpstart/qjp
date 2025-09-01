package com.q.library_management_system.entity;

import jakarta.persistence.*;
import lombok.Data;


/**
 * 图书封面关联实体
 */
@Data
@Entity
@Table(name = "book_cover")
public class BookCover {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** 关联的图书ID（一对一） */
    @Column(name = "book_id", unique = true, nullable = false)
    private Integer bookId;

    /** 封面图片访问URL（给前端展示用，如：/uploads/books/123.jpg） */
    @Column(name = "cover_url", nullable = false)
    private String coverUrl;

    /** 封面图片在服务器的绝对存储路径（如：/var/www/library/uploads/books/123.jpg） */
    @Column(name = "file_path", nullable = false)
    private String filePath;

}

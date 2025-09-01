package com.q.library_management_system.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "book")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer bookId;
    @Column(name = "isbn", nullable = false, unique = true)
    private String isbn;
    @Column(name = "book_name", nullable = false)
    private String bookName;
    @Column(name = "author", nullable = false)
    private String author;
    @Column(name = "publisher", nullable = false)
    private String publisher;
    @Column(name = "publisher_date", nullable = false)
    private LocalDate publisherDate;
    @Column(name = "category_id", nullable = false)
    private Integer categoryId;
    @Column(name = "total_stock", nullable = false)
    private Integer totalStock = 0;
    @Column(name = "available_count", nullable = false)
    private Integer availableCount = 0;
    @Column(name = "location")
    private String location;
}


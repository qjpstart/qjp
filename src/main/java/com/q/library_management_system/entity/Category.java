package com.q.library_management_system.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "category")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer categoryId;
    @Column(name = "category_name", nullable = false, unique = true)
    private String categoryName;
    @Column(name = "parent_id")
    private Integer parentId;
    @Column(name = "descript")
    private String descript;
}
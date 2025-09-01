package com.q.library_management_system.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 图书详情响应DTO
 * 返回图书完整信息（比列表 DTO 多简介、出版社等），用于图书详情页。
 */
@Data
public class BookDetailResponseDTO {
    /** 图书ID */
    private Integer bookId;

    /** 书名 */
    private String bookName;

    /** 作者 */
    private String author;

    /** ISBN编号 */
    private String isbn;

    /** 出版社 */
    private String publisher;

    /** 出版日期 */
    private LocalDate publishDate;

    /** 分类名称 */
    private Integer categoryId;

    /** 总库存 */
    private Integer totalStock;

    /** 可借库存 */
    private Integer availableCount;

    /** 馆藏位置 */
    private String location;

    /** 新增时间 */
    private LocalDateTime createTime;

    /** 最后修改时间 */
    private LocalDateTime updateTime;
}


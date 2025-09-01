package com.q.library_management_system.dto.response;

import lombok.Data;
import java.time.LocalDate;

/**
 * 图书列表响应DTO
 * 返回图书列表时的简化信息，包含分类名称（而非分类 ID），便于前端展示。
 */
@Data
public class BookListResponseDTO {
    /** 图书ID */
    private Integer bookId;

    /** 书名 */
    private String bookName;

    /** 作者 */
    private String author;

    /** ISBN编号 */
    private String isbn;

    /** 分类名称（关联分类表查询结果） */
    private String categoryName;

    /** 总库存 */
    private Integer totalStock;

    /** 可借库存 */
    private Integer availableCount;

    /** 馆藏位置 */
    private String location;

    /** 出版日期 */
    private LocalDate publishDate;

    /** 出版社 */
    private String publisher;
}

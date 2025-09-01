package com.q.library_management_system.service;

import com.q.library_management_system.dto.request.BookUpdateRequestDTO;
import com.q.library_management_system.entity.Book;
import com.q.library_management_system.exception.BusinessException;
import com.q.library_management_system.dto.request.BookSearchRequestDTO;
import com.q.library_management_system.dto.response.BookListResponseDTO;
import com.q.library_management_system.dto.request.BookAddRequestDTO;
import com.q.library_management_system.dto.request.BookStockAdjustRequestDTO;
import com.q.library_management_system.dto.response.PageResultDTO;
import com.q.library_management_system.dto.response.BookDetailResponseDTO;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BookService {
    // 添加图书
    Integer addBook(BookAddRequestDTO bookAddDTO);

    // 删除图书
    void deleteBook(Integer bookId);

    //批量删除图书
    void batchDeleteBooks(List<Integer> bookIds);

    // 根据ID查询图书
    Book getBookById(Integer id);

    // 根据ISBN查询图书
    Book getBookByIsbn(String isbn);

    // 分页查询图书（支持按名称、分类筛选）
    List<Book> searchBooks(String keyword, Integer categoryId, int page, int size);

    // 更新图书信息
    Book updateBook(Integer id, Book book);

    /**
     * 更新图书信息
     * @param bookId 图书ID
     * @param requestDTO 图书更新参数（DTO类型）
     */
    void updateBook(Integer bookId, BookUpdateRequestDTO requestDTO);

    // 统计图书总数
    long countAllBooks(Integer bookId);

    // 统计可借阅图书数量
    long countAvailableBooks(Integer bookId);

    // 查询图书的借阅状态（是否可借）
    boolean isBookAvailable(Integer bookId) throws BusinessException;

    // 增加图书库存（入库操作）
    void increaseStock(Integer bookId, int quantity);

    // 减少图书库存（出库/报废等操作）
    public void decreaseStock(Integer bookId, int quantity);

    // 借阅图书时减少可借库存（不影响总库存）
    public void reduceAvailableStock(Integer bookId);

    // 归还图书时增加可借库存（不影响总库存）
    public void increaseAvailableStock(Integer bookId);

    // 图书列表查询（支持分页+条件）
    PageResultDTO<BookListResponseDTO> getBookList(BookSearchRequestDTO searchDTO);

    // 使用DTO封装所有参数
    PageResultDTO<BookListResponseDTO> searchBooks(BookSearchRequestDTO searchDTO);

    /**
     * 根据图书ID查询图书详情
     * @param bookId 图书ID
     * @return 图书详情DTO
     */
    BookDetailResponseDTO getBookDetail(Integer bookId);

    /**
     * 调整图书库存
     * @param requestDTO 库存调整参数（图书ID、调整数量等）
     * @return 调整后的图书详情
     */
    BookDetailResponseDTO adjustStock(BookStockAdjustRequestDTO requestDTO);

    // 批量库存调整（参数为DTO列表，关键方法）
    void batchAdjustStock(List<BookStockAdjustRequestDTO> requestDTOList);

    /**
     * 批量新增图书
     * 支持批量导入图书，自动跳过重复ISBN的图书，返回成功新增的数量
     */
     int batchAddBooks(List<BookAddRequestDTO> requestDTOList);

    //图书与分类绑定
    void bindCategory(Integer bookId, Integer categoryId);

    /**
     * 检查图书是否存在
     * @param bookId 图书ID
     * @return 存在返回true，否则返回false
     */
    boolean existsById(Integer bookId);
}


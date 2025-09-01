package com.q.library_management_system.repository;

import com.q.library_management_system.entity.Book;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Integer>, JpaSpecificationExecutor<Book> {
    // 根据ISBN查询图书（ISBN唯一）
    Optional<Book> findByIsbn(String isbn);

    // 单个检查ISBN是否已存在
    boolean existsByIsbn(String isbn);

    // 单个检查图书id是否已存在
    boolean existsByBookId(Integer bookId);

    // 批量查询存在的图书ID
    @Query("SELECT b.bookId FROM Book b WHERE b.bookId IN :bookIds")
    List<Integer> findExistingBookIds(@Param("bookIds") List<Integer> bookIds);

    // 批量ISBN查询（返回数据库中已存在的ISBN列表）
    @Query("SELECT b.isbn FROM Book b WHERE b.isbn IN :isbns")
    List<String> findAllExistingIsbns(@Param("isbns") List<String> isbns);

    // 根据书名模糊查询 + 分类ID精确查询
    // 功能：查询某分类下书名包含关键词的图书
    List<Book> findByBookNameContainingAndCategoryId(String bookName, Integer categoryId);

    // 根据分类ID查询图书
    List<Book> findByCategoryId(Integer categoryId);

    // 根据书名模糊查询
    List<Book> findByBookNameContaining(String keyword);

    // 支持分页的组合查询方法
    // 参数：书名关键词、分类ID、分页参数（Pageable）
    // 返回值：Page<Book>（包含分页信息和当前页数据）
    Page<Book> findByBookNameContainingAndCategoryId(String bookName, Integer categoryId, Pageable pageable);

    // 仅按书名模糊查询（分页）
    Page<Book> findByBookNameContaining(String bookName, Pageable pageable);

    // 仅按分类查询（分页）
    Page<Book> findByCategoryId(Integer categoryId, Pageable pageable);

    // 查询可借数量大于0的图书
    List<Book> findByAvailableCountGreaterThan(Integer count);

    // 检查是否存在指定分类 ID
    boolean existsByCategoryId(Integer categoryId);

    // 按分类统计图书数量（返回分类名称和对应数量）
    @Query("SELECT c.categoryName, COUNT(b) FROM Book b " +
            "JOIN b.category c " +
            "GROUP BY c.categoryName")
    List<Object[]> countByCategoryGroup();

    // 统计所有可借阅图书数量（sum(available_stock)）
    @Query("SELECT SUM(b.availableStock) FROM Book b")
    Long sumAvailableBooks();

    // 根据图书ID查询可借数量（返回单个图书的可借数量）
    @Query("SELECT b.availableStock FROM Book b WHERE b.bookId = :bookId")
    Integer findAvailableStockByBookId(Integer bookId);

    // 统计所有图书的总可借数量
    @Query("SELECT SUM(b.availableStock) FROM Book b")
    Long sumAllAvailableStocks();

    // 添加带悲观锁的查询方法（用于并发控制）
    @Lock(LockModeType.PESSIMISTIC_WRITE) // 加写锁，防止其他事务修改
    @Query("SELECT b FROM Book b WHERE b.bookId = :bookId")
    Optional<Book> findByIdWithLock(@Param("bookId") Integer bookId);
}

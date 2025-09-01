package com.q.library_management_system;

import com.q.library_management_system.entity.*;
import com.q.library_management_system.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

@SpringBootTest
public class AllTablesTest {

    // 注入所有Repository
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private BorrowRecordRepository borrowRecordRepository;
    @Autowired
    private ReserveRecordRepository reserveRecordRepository;


    // 1. 测试分类表
    @Test
    public void testCategory() {
        // 新增分类
        Category category = new Category();
        category.setCategoryName("计算机科学");
        category.setDescript("包含编程、数据库等相关图书");
        Category savedCategory = categoryRepository.save(category);
        System.out.println("新增分类ID：" + savedCategory.getCategoryId());

        // 查询分类
        categoryRepository.findById(savedCategory.getCategoryId())
                .ifPresentOrElse(
                        c -> System.out.println("查询到分类：" + c.getCategoryName()),
                        () -> System.out.println("未查询到分类")
                );
    }


    // 2. 测试图书表（依赖分类表）
    @Test
    public void testBook() {
        // 先确保有一个分类（无数据则创建）
        Category category = categoryRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> {
                    Category newCategory = new Category();
                    newCategory.setCategoryName("默认分类");
                    return categoryRepository.save(newCategory);
                });

        // 新增图书
        Book book = new Book();
        book.setIsbn("9787111641247");
        book.setBookName("Java编程思想");
        book.setAuthor("Bruce Eckel");
        book.setPublisher("机械工业出版社");
        book.setPublisherDate(LocalDate.of(2020, 1, 1));
        book.setCategoryId(category.getCategoryId());
        book.setTotalStock(10);
        book.setAvailableCount(10);
        book.setLocation("A区3架");
        Book savedBook = bookRepository.save(book);
        System.out.println("新增图书ID：" + savedBook.getBookId());

        // 查询图书
        bookRepository.findByIsbn("9787111641247")
                .ifPresentOrElse(
                        b -> System.out.println("查询到图书：" + b.getBookName()),
                        () -> System.out.println("未查询到图书")
                );
    }


    // 3. 测试用户表
    @Test
    public void testUser() {
        // 新增用户
        User user = new User();
        user.setUserName("test_user");
        user.setPassword("123456"); // 实际需加密
        user.setRealName("测试用户");
        user.setPhone("13800138000");
        user.setEmail("test@example.com");
        user.setRegisterTime(LocalDateTime.now());
        user.setCreditScore(100);
        User savedUser = userRepository.save(user);
        System.out.println("新增用户ID：" + savedUser.getUserId());

        // 查询用户
        userRepository.findByUserName("test_user")
                .ifPresentOrElse(
                        u -> System.out.println("查询到用户：" + u.getRealName()),
                        () -> System.out.println("未查询到用户")
                );
    }


    // 4. 测试借阅记录表（依赖用户表和图书表）
    @Test
    public void testBorrowRecord() {
        // 先获取测试用户和测试图书
        User user = userRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("请先运行testUser()创建用户数据"));

        Book book = bookRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("请先运行testBook()创建图书数据"));

        // 新增借阅记录（使用Duration替代ChronoUnit，设置30天有效期）
        LocalDateTime now = LocalDateTime.now();
        BorrowRecord record = new BorrowRecord();
        record.setBookId(book.getBookId());
        record.setUserId(user.getUserId());
        record.setBorrowDate(now);
        record.setDueDate(now.plus(Duration.ofDays(30))); // 30天后到期
        record.setBorrowStatus(BorrowRecord.BorrowStatus.unreturned);
        BorrowRecord savedRecord = borrowRecordRepository.save(record);
        System.out.println("新增借阅记录ID：" + savedRecord.getRecordId());

        // 查询用户的借阅记录
        borrowRecordRepository.findByUserId(user.getUserId()).forEach(
                r -> System.out.println("用户借阅记录：" + r.getRecordId())
        );
    }


    // 5. 测试预约记录表（依赖用户表和图书表）
    @Test
    public void testReservationRecord() {
        // 先获取测试用户和测试图书
        User user = userRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("请先运行testUser()创建用户数据"));

        Book book = bookRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("请先运行testBook()创建图书数据"));

        // 新增预约记录（使用Duration替代ChronoUnit，设置7天有效期）
        LocalDateTime now = LocalDateTime.now();
        ReserveRecord reserve = new ReserveRecord();
        reserve.setBookId(book.getBookId());
        reserve.setUserId(user.getUserId());
        reserve.setReserveDate(now);
        reserve.setExpireDate(now.plus(Duration.ofDays(7))); // 7天后过期（已修正笔误）
        reserve.setReserveStatus(ReserveRecord.ReserveStatus.waiting);
        ReserveRecord savedReservation = reserveRecordRepository.save(reserve);
        System.out.println("新增预约记录ID：" + savedReservation.getReserveId());

        // 查询图书的预约记录
        reserveRecordRepository.findByBookId(book.getBookId()).forEach(
                r -> System.out.println("图书预约记录：" + r.getReserveId())
        );
    }
}



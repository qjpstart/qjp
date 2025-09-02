package com.q.library_management_system.service.impl;

import com.q.library_management_system.dto.request.BookAddRequestDTO;
import com.q.library_management_system.dto.request.BookSearchRequestDTO;
import com.q.library_management_system.dto.request.BookStockAdjustRequestDTO;
import com.q.library_management_system.dto.request.BookUpdateRequestDTO;
import com.q.library_management_system.dto.response.BookListResponseDTO;
import com.q.library_management_system.dto.response.BookDetailResponseDTO;
import com.q.library_management_system.dto.response.PageResultDTO;
import com.q.library_management_system.entity.Book;
import com.q.library_management_system.entity.BorrowRecord;
import com.q.library_management_system.entity.Category;
import com.q.library_management_system.exception.BusinessException;
import com.q.library_management_system.repository.BookRepository;
import com.q.library_management_system.repository.CategoryRepository;
import com.q.library_management_system.repository.BorrowRecordRepository;
import com.q.library_management_system.service.BookService;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import jakarta.persistence.criteria.Predicate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final ConcurrentHashMap<Integer, Object> bookLocks = new ConcurrentHashMap<>(); //按bookId的细粒度锁

    // 获取当前图书的专属锁对象
    private Object getLock(Integer bookId) {
        return bookLocks.computeIfAbsent(bookId, k -> new Object());
    }

    @Override
    @Transactional
    public Integer addBook(BookAddRequestDTO bookAddDTO) {
        // 1. 检查ISBN唯一性
        if (bookRepository.existsByIsbn(bookAddDTO.getIsbn())) {
            throw new BusinessException("ISBN已存在：" + bookAddDTO.getIsbn());
        }

        // 2. 检查分类是否存在
        Integer categoryId = bookAddDTO.getCategoryId();
        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw new BusinessException("分类不存在：" + categoryId);
        }

        // 3. DTO转换为Entity（严格匹配实体类字段名）
        Book book = new Book();
        // 基础信息映射（修正字段名不匹配问题）
        book.setIsbn(bookAddDTO.getIsbn());
        book.setBookName(bookAddDTO.getBookName()); // 实体类中是bookName而非title
        book.setAuthor(bookAddDTO.getAuthor());     // 实体类中是author（注意拼写）
        book.setPublisher(bookAddDTO.getPublisher());
        // 出版日期：实体类用LocalDate，需确保DTO传递的类型匹配
        book.setPublisherDate(bookAddDTO.getPublisherDate());
        book.setCategoryId(categoryId);
        book.setLocation(bookAddDTO.getLocation()); // 图书位置

        // 4. 库存相关设置
        book.setTotalStock(bookAddDTO.getTotalStock());
        book.setAvailableCount(bookAddDTO.getTotalStock()); // 可借数量=总库存

        // 5. 保存实体并返回ID
        Book savedBook = bookRepository.save(book);
        return savedBook.getBookId(); // 实体类中主键是bookId而非id
    }

    @Override
    public Book getBookById(Integer id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException("图书不存在：" + id));
    }

    @Override
    public Book getBookByIsbn(String isbn) {
        return bookRepository.findByIsbn(isbn)
                .orElseThrow(() -> new BusinessException("图书不存在：" + isbn));
    }

    @Override
    public List<Book> searchBooks(String keyword, Integer categoryId, int page, int size) {
        Page<Book> bookPage;
        PageRequest pageRequest = PageRequest.of(page, size);

        // 构建查询条件
        if (keyword != null && categoryId != null) {
            bookPage = bookRepository.findByBookNameContainingAndCategoryId(keyword, categoryId, pageRequest);
        } else if (keyword != null) {
            bookPage = bookRepository.findByBookNameContaining(keyword, pageRequest);
        } else if (categoryId != null) {
            bookPage = bookRepository.findByCategoryId(categoryId, pageRequest);
        } else {
            bookPage = bookRepository.findAll(pageRequest);
        }
        return bookPage.getContent();
    }

    @Override
    @Transactional
    public Book updateBook(Integer id, Book book) {
        Book existing = getBookById(id);

        // 若修改ISBN，需检查新ISBN是否重复
        if (!existing.getIsbn().equals(book.getIsbn()) &&
                bookRepository.existsByIsbn(book.getIsbn())) {
            throw new BusinessException("ISBN已存在：" + book.getIsbn());
        }

        // 检查分类是否存在
        if (book.getCategoryId() != null && !categoryRepository.existsById(book.getCategoryId())) {
            throw new BusinessException("分类不存在：" + book.getCategoryId());
        }

        // 更新字段
        existing.setIsbn(book.getIsbn());
        existing.setBookName(book.getBookName());
        existing.setAuthor(book.getAuthor());
        existing.setPublisher(book.getPublisher());
        existing.setPublisherDate(book.getPublisherDate());
        existing.setCategoryId(book.getCategoryId());
        existing.setTotalStock(book.getTotalStock());
        existing.setLocation(book.getLocation());

        // 调整可借数量（不允许超过总库存）
        existing.setAvailableCount(Math.min(existing.getAvailableCount(), book.getTotalStock()));
        return bookRepository.save(existing);
    }

    @Override
    @Transactional
    public void updateBook(Integer bookId, BookUpdateRequestDTO requestDTO) { // 参数类型与接口一致
        // 1. 校验图书是否存在
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException("图书不存在：" + bookId));

        // 2. 从DTO提取参数，更新到实体（仅更新非空字段，支持部分更新）
        if (requestDTO.getBookName() != null) {
            book.setBookName(requestDTO.getBookName());
        }
        if (requestDTO.getAuthor() != null) {
            book.setAuthor(requestDTO.getAuthor());
        }
        if (requestDTO.getPublisher() != null) {
            book.setPublisher(requestDTO.getPublisher());
        }
        if (requestDTO.getPublisherDate() != null) {
            book.setPublisherDate(requestDTO.getPublisherDate());
        }
        if (requestDTO.getCategoryId() != null) {
            book.setCategoryId(requestDTO.getCategoryId());
        }
        if (requestDTO.getLocation() != null) {
            book.setLocation(requestDTO.getLocation());
        }

        bookRepository.save(book);
    }

    /**
     * 删除图书前检查是否存在未归还的借阅记录
     */
    @Override
    @Transactional
    public void deleteBook(Integer bookId) {
        // 检查图书是否存在
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException("图书不存在"));

        // 检查是否有未归还的借阅记录（核心验证）
        boolean hasUnreturned = borrowRecordRepository
                .existsByBookIdAndBorrowStatus(bookId, BorrowRecord.BorrowStatus.unreturned);

        if (hasUnreturned) {
            throw new BusinessException("该图书存在未归还的借阅记录，无法删除");
        }

        // 执行删除（物理删除，若需保留可改为逻辑删除）
        bookRepository.delete(book);
    }

    //查询图书的借阅状态（是否可借）
    public boolean isBookAvailable(Integer bookId) {
        // 使用 synchronized 锁定图书ID对应的对象，确保同一本书的操作串行执行
        synchronized (getLock(bookId)) {
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new BusinessException("图书不存在"));
            return book.getAvailableCount() > 0;
        }
    }

    //统计指定图书的总数量（单本图书的总库存）
    @Override
    public long countAllBooks(Integer bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException("图书不存在"));
        return book.getTotalStock();
    }

    // 统计指定图书的可借阅数量
    @Override
    public long countAvailableBooks(Integer bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException("图书不存在"));
        return book.getAvailableCount();
    }

    /**
     * 增加图书库存（入库操作）
     * @param bookId 图书ID
     * @param quantity 增加的数量（必须为正数）
     */
    @Override
    @Transactional
    public void increaseStock(Integer bookId, int quantity) {
        // 校验数量合法性
        if (quantity <= 0) {
            throw new BusinessException("增加的库存数量必须大于0");
        }

        // 查询图书并更新库存
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException("图书不存在"));

        // 同时增加总库存和可借库存
        book.setTotalStock(book.getTotalStock() + quantity);
        book.setAvailableCount(book.getAvailableCount() + quantity);

        bookRepository.save(book);
    }

    /**
     * 减少图书库存（出库/报废等操作）
     * @param bookId 图书ID
     * @param quantity 减少的数量（必须为正数）
     */
    @Override
    @Transactional
    public void decreaseStock(Integer bookId, int quantity) {
        // 校验数量合法性
        if (quantity <= 0) {
            throw new BusinessException("减少的库存数量必须大于0");
        }

        // 查询图书
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException("图书不存在"));

        // 校验库存是否充足
        if (book.getTotalStock() < quantity) {
            throw new BusinessException("库存不足，当前总库存：" + book.getTotalStock());
        }

        // 同时减少总库存和可借库存
        book.setTotalStock(book.getTotalStock() - quantity);
        book.setAvailableCount(book.getAvailableCount() - quantity);

        bookRepository.save(book);
    }

    /**
     * 借阅图书时减少可借库存（不影响总库存）
     * @param bookId 图书ID
     */
    @Override
    @Transactional
    public void reduceAvailableStock(Integer bookId) {
        synchronized (getLock(bookId)) { // 加锁，确保并发安全
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new BusinessException("图书不存在"));

             if (book.getAvailableCount() <= 0) {
                    throw new BusinessException("图书已无可用库存，无法借阅");
             }

            // 仅减少可借库存，总库存不变
            book.setAvailableCount(book.getAvailableCount() - 1);
            bookRepository.save(book);
        }
    }

    /**
     * 归还图书时增加可借库存（不影响总库存）
     * @param bookId 图书ID
     */
    @Override
    @Transactional
    public void increaseAvailableStock(Integer bookId) {
        synchronized (getLock(bookId)) { // 加锁
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new BusinessException("图书不存在"));

            // 防止可借库存超过总库存（异常保护）
            if (book.getAvailableCount() >= book.getTotalStock()) {
                throw new BusinessException("可借库存已达上限，无需增加");
            }

            // 仅增加可借库存，总库存不变
            book.setAvailableCount(book.getAvailableCount() + 1);
            bookRepository.save(book);
        }
    }

    // 2. 实现：图书列表查询（分页+多条件）
    @Override
    public PageResultDTO<BookListResponseDTO> getBookList(BookSearchRequestDTO searchDTO) {
        // ① 构建分页参数（修复：定义currentPage，区分前端原始页码和JPA用的页码）
        Integer currentPage = searchDTO.getPageNum(); // 前端传递的原始页码（从1开始，用户视角）
        int pageNum = currentPage - 1; // 转换为JPA需要的页码（从0开始，框架视角）
        int pageSize = searchDTO.getPageSize();
        Pageable pageable = PageRequest.of(pageNum, pageSize);

        // ② 构建查询条件（动态拼接：分类ID、作者、出版社等）
        Specification<Book> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 条件1：按分类ID查询（非空才加条件）
            if (searchDTO.getCategoryId() != null && searchDTO.getCategoryId() > 0) {
                predicates.add(cb.equal(root.get("categoryId"), searchDTO.getCategoryId()));
            }
            // 条件2：按作者模糊查询（非空才加条件）
            if (searchDTO.getAuthor() != null && !searchDTO.getAuthor().trim().isEmpty()) {
                predicates.add(cb.like(root.get("author"), "%" + searchDTO.getAuthor().trim() + "%"));
            }
            // 条件3：按出版社模糊查询（非空才加条件）
            if (searchDTO.getPublisher() != null && !searchDTO.getPublisher().trim().isEmpty()) {
                predicates.add(cb.like(root.get("publisher"), "%" + searchDTO.getPublisher().trim() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // ③ 调用DAO层查询分页数据
        Page<Book> bookPage = bookRepository.findAll(spec, pageable);

        // ④ 转换：Entity → DTO（只返回前端需要的字段）
        List<BookListResponseDTO> dtoList = bookPage.getContent().stream()
                .map(book -> {
                    BookListResponseDTO dto = new BookListResponseDTO();
                    dto.setBookId(book.getBookId());
                    dto.setBookName(book.getBookName());
                    dto.setIsbn(book.getIsbn());
                    dto.setAuthor(book.getAuthor());
                    dto.setPublisher(book.getPublisher());
                    dto.setAvailableCount(book.getAvailableCount()); // 可借数量
                    return dto;
                })
                .collect(Collectors.toList());

        // ⑤ 封装分页结果（总条数、总页数、当前页数据）
        return new PageResultDTO<BookListResponseDTO>(
                bookPage.getTotalElements(),  // 总条数
                bookPage.getTotalPages(),     // 总页数
                currentPage,                  // 当前页码
                pageSize,                     // 每页条数
                dtoList                       // 当前页数据列表（类型为 List<BookListResponseDTO>）
        );
    }

    // 从DTO中提取参数
    @Override
    public PageResultDTO<BookListResponseDTO> searchBooks(BookSearchRequestDTO searchDTO) {
        // 1. 分页参数校验与处理（增强边界保护）
        if (searchDTO == null) {
            throw new BusinessException("查询参数不能为空");
        }

        int pageNum = searchDTO.getPageNum();
        int pageSize = searchDTO.getPageSize();

        // 校验页码合法性（必须为正数）
        if (pageNum < 1) {
            throw new BusinessException("页码必须大于等于1");
        }
        // 转换为JPA页码（0开始）
        int jpaPageNum = pageNum - 1;

        // 校验页大小合法性（1-100之间）
        if (pageSize < 1 || pageSize > 100) {
            throw new BusinessException("页大小必须在1-100之间");
        }

        // 构建分页参数
        Pageable pageable = PageRequest.of(jpaPageNum, pageSize);

        // 2. 提取查询参数并校验
        String keyword = searchDTO.getKeyword();
        Integer categoryId = searchDTO.getCategoryId();
        String author = searchDTO.getAuthor();
        String publisher = searchDTO.getPublisher();
        String publishDateStart = searchDTO.getPublishDateStart();
        String publishDateEnd = searchDTO.getPublishDateEnd();
        Boolean available = searchDTO.getAvailable();

        // 分类ID校验（若不为空则必须为正数）
        if (categoryId != null && categoryId <= 0) {
            throw new BusinessException("分类ID必须为正数");
        }

        // 3. 构建查询条件
        Specification<Book> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 关键词模糊查询（书名/作者/ISBN）
            if (StringUtils.hasText(keyword)) {
                String likePattern = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("bookName"), likePattern),
                        cb.like(root.get("author"), likePattern),
                        cb.like(root.get("isbn"), likePattern)
                ));
            }

            // 分类ID查询
            if (categoryId != null && categoryId > 0) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }

            // 作者筛选
            if (StringUtils.hasText(author)) {
                predicates.add(cb.like(root.get("author"), "%" + author.trim() + "%"));
            }

            // 出版社筛选
            if (StringUtils.hasText(publisher)) {
                predicates.add(cb.like(root.get("publisher"), "%" + publisher.trim() + "%"));
            }

            // 出版日期范围筛选（格式校验）
            try {
                if (StringUtils.hasText(publishDateStart)) {
                    LocalDate startDate = LocalDate.parse(publishDateStart.trim());
                    predicates.add(cb.greaterThanOrEqualTo(root.get("publisherDate"), startDate));
                }

                if (StringUtils.hasText(publishDateEnd)) {
                    LocalDate endDate = LocalDate.parse(publishDateEnd.trim());
                    predicates.add(cb.lessThanOrEqualTo(root.get("publisherDate"), endDate));
                }
            } catch (DateTimeParseException e) {
                throw new BusinessException("出版日期格式错误，应为yyyy-MM-dd");
            }

            // 开始日期不能晚于结束日期
            if (StringUtils.hasText(publishDateStart) && StringUtils.hasText(publishDateEnd)) {
                try {
                    LocalDate start = LocalDate.parse(publishDateStart.trim());
                    LocalDate end = LocalDate.parse(publishDateEnd.trim());
                    if (start.isAfter(end)) {
                        throw new BusinessException("开始日期不能晚于结束日期");
                    }
                } catch (DateTimeParseException e) {
                    // 已在上方捕获格式错误，此处无需重复处理
                }
            }

            // 可借状态筛选
            if (available != null && available) {
                predicates.add(cb.greaterThan(root.get("availableCount"), 0));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 4. 执行查询
        Page<Book> bookPage = bookRepository.findAll(spec, pageable);

        // 5. 转换结果（示例，实际需根据项目完善）
        List<BookListResponseDTO> dtoList = convertToDTO(bookPage.getContent());

        // 6. 返回分页结果
        return new PageResultDTO<>(
                bookPage.getTotalElements(),
                bookPage.getTotalPages(),
                pageNum, // 前端原始页码
                pageSize,
                dtoList
        );
    }

    private List<BookListResponseDTO> convertToDTO(List<Book> books) {
        List<BookListResponseDTO> dtos = new ArrayList<>();
        for (Book book : books) {
            BookListResponseDTO dto = new BookListResponseDTO();
            // 映射图书基本信息
            dto.setBookId(book.getBookId());
            dto.setBookName(book.getBookName());
            dto.setAuthor(book.getAuthor());
            dto.setIsbn(book.getIsbn());
            dto.setPublisher(book.getPublisher());
            dto.setPublishDate(book.getPublisherDate()); // 注意DTO字段是publishDate，实体类是publisherDate
            dto.setTotalStock(book.getTotalStock());
            dto.setAvailableCount(book.getAvailableCount());
            dto.setLocation(book.getLocation());

            // 处理分类名称（需要从分类ID查询        // 这里需要通过categoryId查询分类名称，假设通过categoryRepository获取
            if (book.getCategoryId() != null) {
                String categoryName = categoryRepository.findById(book.getCategoryId())
                        .map(category -> category.getCategoryName()) // 假设分类实体有getCategoryName()方法
                        .orElse("未知分类"); // 处理分类不存在的情况
                dto.setCategoryName(categoryName);
            } else {
                dto.setCategoryName("未分类");
            }

            dtos.add(dto);
        }
        return dtos;
    }


    @Override
    public BookDetailResponseDTO getBookDetail(Integer bookId) {
        // 1. 校验参数
        if (bookId == null || bookId <= 0) {
            throw new BusinessException("图书ID无效");
        }

        // 2. 查询图书信息（如果不存在则抛出异常）
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException("未找到该图书"));

        // 3. 转换为详情DTO（包含更多字段，如简介、目录等）
        BookDetailResponseDTO dto = new BookDetailResponseDTO();
        dto.setBookId(book.getBookId());
        dto.setBookName(book.getBookName());
        dto.setIsbn(book.getIsbn());
        dto.setAuthor(book.getAuthor());
        dto.setPublisher(book.getPublisher());
        dto.setPublishDate(book.getPublisherDate());
        dto.setTotalStock(book.getTotalStock()); // 总藏书量
        dto.setAvailableCount(book.getAvailableCount()); // 可借数量
        dto.setCategoryId(book.getCategoryId());//分类id
        dto.setLocation(book.getLocation());//位置

        return dto;
    }

    /**
     * 调整图书库存（事务保证原子性）
     */
    @Override
    @Transactional // 库存调整需要事务保证
    public BookDetailResponseDTO adjustStock(BookStockAdjustRequestDTO requestDTO) {
        // 1. 参数校验
        Integer bookId = requestDTO.getBookId();
        Integer adjustCount = requestDTO.getAdjustNum();

        if (bookId == null || bookId <= 0) {
            throw new BusinessException("图书ID无效");
        }
        if (adjustCount == 0) {
            throw new BusinessException("调整数量不能为0");
        }

        // 2. 查询图书信息
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException("未找到该图书"));

        // 3. 计算新库存（确保库存不会为负数）
        int newTotalCount = book.getTotalStock() + adjustCount;
        if (newTotalCount < 0) {
            throw new BusinessException("库存不足，无法减少");
        }

        // 4. 更新库存（总库存和可借库存同步调整）
        book.setTotalStock(newTotalCount);
        book.setAvailableCount(book.getAvailableCount() + adjustCount); // 可借库存同步调整
        Book updatedBook = bookRepository.save(book);

        // 5. 转换为详情DTO返回
        return convertToDetailDTO(updatedBook);
    }

    /**
     * 批量调整图书库存（简化版）
     * 仅实现库存修改功能，不记录调整日志，单本失败会导致整体回滚
     * @param requestDTOList 库存调整请求列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAdjustStock(List<BookStockAdjustRequestDTO> requestDTOList) {
        // 1. 校验入参
        if (requestDTOList == null || requestDTOList.isEmpty()) {
            throw new BusinessException("批量调整的库存列表不能为空");
        }

        LocalDateTime now = LocalDateTime.now();

        for (BookStockAdjustRequestDTO dto : requestDTOList) {
            // 为每个图书ID单独加锁，避免批量操作中的并发冲突
            synchronized (getLock(dto.getBookId())) {
                // 2. 基础参数校验
                if (dto.getBookId() == null || dto.getAdjustNum() == null) {
                    throw new BusinessException("图书ID和调整数量不能为空");
                }

                // 3. 查询图书
                Book book = bookRepository.findById(dto.getBookId())
                        .orElseThrow(() -> new BusinessException("图书不存在：" + dto.getBookId()));

                // 4. 计算新库存（防止负数）
                int newStock = book.getTotalStock() + dto.getAdjustNum();
                if (newStock < 0) {
                    throw new BusinessException(
                            String.format("图书【%s】库存不足，当前库存：%d，调整数量：%d",
                                    book.getBookName(), book.getTotalStock(), dto.getAdjustNum())
                    );
                }

                // 5. 更新库存（同步调整可借阅数量）
                book.setTotalStock(newStock);
                if (dto.getAdjustNum() > 0) {
                    book.setAvailableCount(book.getAvailableCount() + dto.getAdjustNum());
                }

                bookRepository.save(book);
            }
        }
    }


    // 复用已有的DTO转换方法
    private BookDetailResponseDTO convertToDetailDTO(Book book) {
        BookDetailResponseDTO dto = new BookDetailResponseDTO();
        dto.setBookId(book.getBookId());
        dto.setBookName(book.getBookName());
        dto.setIsbn(book.getIsbn());
        dto.setAuthor(book.getAuthor());
        dto.setPublisher(book.getPublisher());
        dto.setPublishDate(book.getPublisherDate());
        dto.setTotalStock(book.getTotalStock()); // 总藏书量
        dto.setAvailableCount(book.getAvailableCount()); // 可借数量
        dto.setCategoryId(book.getCategoryId());//分类id
        dto.setLocation(book.getLocation());//位置
        return dto;
    }

    /**
     * 批量新增图书
     * 支持批量导入图书，自动跳过重复ISBN的图书，返回成功新增的数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchAddBooks(List<BookAddRequestDTO> requestDTOList) {
        // 1. 校验入参非空
        if (requestDTOList == null || requestDTOList.isEmpty()) {
            throw new BusinessException("批量新增的图书列表不能为空");
        }

        // 2. 提取所有ISBN，批量查询已存在的ISBN（减少数据库交互次数）
        List<String> allIsbns = requestDTOList.stream()
                .map(BookAddRequestDTO::getIsbn)
                .filter(StringUtils::hasText) // 过滤空ISBN
                .collect(Collectors.toList());

        List<String> existingIsbns = new ArrayList<>();
        if (!allIsbns.isEmpty()) {
            existingIsbns = bookRepository.findAllExistingIsbns(allIsbns);
        }

        // 3. 构建待新增的图书实体列表（跳过重复ISBN）
        List<Book> booksToSave = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(); // 统一时间，避免循环中重复创建

        for (BookAddRequestDTO dto : requestDTOList) {
            // 跳过ISBN为空的无效数据
            if (!StringUtils.hasText(dto.getIsbn())) {
                continue;
            }

            // 跳过已存在的ISBN
            if (existingIsbns.contains(dto.getIsbn())) {
                continue;
            }

            // 构建图书实体
            Book book = new Book();
            book.setBookName(dto.getBookName());
            book.setAuthor(dto.getAuthor());
            book.setPublisher(dto.getPublisher());
            book.setIsbn(dto.getIsbn());
            book.setPublisherDate(dto.getPublisherDate());
            book.setTotalStock(dto.getTotalStock() == null ? 0 : dto.getTotalStock()); // 默认为0
            book.setAvailableCount(dto.getAvailableStock() == null ? 0 : dto.getAvailableStock()); // 默认为0

            booksToSave.add(book);
        }

        // 4. 批量保存图书（仅1次数据库交互，高效）
        if (!booksToSave.isEmpty()) {
            bookRepository.saveAll(booksToSave);
        }

        // 5. 返回成功新增的数量
        return booksToSave.size();
    }

    //图书与分类绑定
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindCategory(Integer bookId, Integer categoryId) {
        // 1. 校验图书存在
        Book book = bookRepository.findById (bookId)
                .orElseThrow (() -> new BusinessException ("图书不存在：" + bookId));

        // 2. 校验分类ID非空 + 分类存在
        if (categoryId == null) {
            throw new BusinessException("分类ID不能为空");
        }
        boolean categoryExists = categoryRepository.existsByCategoryId(categoryId);
        if (!categoryExists) {
            throw new BusinessException("分类不存在：" + categoryId);
        }

        // 3. 直接在图书表中更新分类 ID（绑定分类）
        book.setCategoryId (categoryId);

        bookRepository.save (book);
    }

    //批量删除图书
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteBooks(List<Integer> bookIds) {
        // 1. 校验参数
        if (bookIds == null || bookIds.isEmpty()) {
            throw new BusinessException("请选择要删除的图书");
        }

        // 2. 校验图书是否存在（可选：根据业务需求决定是否校验）
        List<Integer> existingBookIds = bookRepository.findExistingBookIds(bookIds);
        if (existingBookIds.isEmpty()) {
            throw new BusinessException("所选图书均不存在");
        }
        /* 可选：检查是否有部分图书不存在
        if (existingBookIds.size() < bookIds.size()) {
            bookIds.removeAll(existingBookIds);
            log.warn("以下图书不存在，已跳过删除：" + bookIds);
        }*/

        // 3. 检查是否有关联数据（如已借出、有库存记录等，根据业务需求添加）
        List<Integer> borrowedBookIds = borrowRecordRepository.findBorrowedBookIds(existingBookIds);
        if (!borrowedBookIds.isEmpty()) {
            throw new BusinessException("以下图书存在借出记录，无法删除：" + borrowedBookIds);
        }

        // 4. 批量删除图书
        bookRepository.deleteAllById(existingBookIds);

    }

    /**
     * 检查图书是否存在
     * @param bookId 图书ID
     * @return 存在返回true，否则返回false
     */
    @Override
    public boolean existsById(Integer bookId) {
        // 调用Repository的existsById方法检查图书是否存在
        return bookRepository.existsById(bookId);
    }
}


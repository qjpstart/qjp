package com.q.library_management_system.repository;

import com.q.library_management_system.entity.BookCover;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * 图书封面数据访问接口
 */
public interface BookCoverRepository extends JpaRepository<BookCover, Integer> {

    /**
     * 根据图书ID查询封面
     * @param bookId 图书ID
     * @return 封面信息或空
     */
    Optional<BookCover> findByBookId(Integer bookId);

    /**
     * 检查图书是否已有封面
     * @param bookId 图书ID
     * @return 存在返回true，否则false
     */
    boolean existsByBookId(Integer bookId);

    /**
     * 根据图书ID删除封面
     * @param bookId 图书ID
     */
    void deleteByBookId(Integer bookId);
}


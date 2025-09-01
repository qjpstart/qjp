package com.q.library_management_system.service;

import com.q.library_management_system.entity.BookCover;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图书封面服务接口
 */
public interface BookCoverService {

    /**
     * 上传或更新图书封面（仅管理员）
     * @param bookId 图书ID
     * @param file 图片文件
     * @return 封面信息
     */
    BookCover uploadOrUpdateCover(Integer bookId, MultipartFile file);

    /**
     * 根据图书ID获取封面信息
     * @param bookId 图书ID
     * @return 封面信息或null
     */
    BookCover getCoverByBookId(Integer bookId);

    /**
     * 删除图书封面（仅管理员）
     * @param bookId 图书ID
     */
    void deleteCover(Integer bookId);

}


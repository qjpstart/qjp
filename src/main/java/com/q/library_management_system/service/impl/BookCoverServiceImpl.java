package com.q.library_management_system.service.impl;

import com.q.library_management_system.entity.BookCover;
import com.q.library_management_system.exception.BusinessException;
import com.q.library_management_system.repository.BookCoverRepository;
import com.q.library_management_system.service.BookCoverService;
import com.q.library_management_system.service.BookService;
import com.q.library_management_system.util.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class BookCoverServiceImpl implements BookCoverService {

    private final BookCoverRepository bookCoverRepository;
    private final BookService bookService;
    private final FileUploadUtil fileUploadUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookCover uploadOrUpdateCover(Integer bookId, MultipartFile file) {
        // 1. 校验文件是否为空
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的封面图片");
        }

        // 2. 校验图书是否存在
        if (!bookService.existsById(bookId)) {
            throw new BusinessException("图书不存在：" + bookId);
        }

        // 3. 校验文件格式和大小
        validateFile(file);

        try {
            // 4. 处理原始文件名（关键：解决可能为null的问题）
            String originalFilename = file.getOriginalFilename();
            // 如果原始文件名为null，手动指定一个默认名称
            if (originalFilename == null || originalFilename.trim().isEmpty()) {
                originalFilename = "cover_default";
            }

            // 5. 生成唯一文件名（此时参数已确保非空）
            String fileName = fileUploadUtil.generateFileName(originalFilename);

            // 6. 获取文件存储路径和访问URL
            String absoluteFilePath = fileUploadUtil.getAbsoluteFilePath(fileName);
            String coverUrl = fileUploadUtil.getAccessUrl(fileName);

            // 7. 保存文件到服务器
            saveFileToServer(file, absoluteFilePath);

            // 8. 保存或更新数据库记录
            BookCover bookCover = bookCoverRepository.findByBookId(bookId)
                    .orElse(new BookCover());
            bookCover.setBookId(bookId);
            bookCover.setCoverUrl(coverUrl);
            bookCover.setFilePath(absoluteFilePath);

            return bookCoverRepository.save(bookCover);

        } catch (IOException e) {
            throw new BusinessException("封面上传失败：" + e.getMessage());
        }
    }

    @Override
    public BookCover getCoverByBookId(Integer bookId) {
        return bookCoverRepository.findByBookId(bookId).orElse(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCover(Integer bookId) {
        // 1. 查询封面记录
        BookCover bookCover = bookCoverRepository.findByBookId(bookId)
                .orElseThrow(() -> new BusinessException("该图书未上传封面"));

        // 2. 删除服务器文件
        File file = new File(bookCover.getFilePath());
        if (file.exists() && !file.delete()) {
            throw new BusinessException("服务器文件删除失败，请手动清理");
        }

        // 3. 删除数据库记录
        bookCoverRepository.delete(bookCover);
    }

    /**
     * 校验文件格式和大小
     */
    private void validateFile(MultipartFile file) {
        // 校验文件大小（不超过5MB）
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            throw new BusinessException("封面图片大小不能超过5MB");
        }

        // 校验文件格式（仅支持JPG/PNG）
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
            throw new BusinessException("仅支持JPG、PNG格式的图片");
        }
    }

    /**
     * 保存文件到服务器（确保目录存在）
     */
    private void saveFileToServer(MultipartFile file, String absoluteFilePath) throws IOException {
        File destFile = new File(absoluteFilePath);
        // 自动创建父目录
        File parentDir = destFile.getParentFile();
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            throw new BusinessException("无法创建文件存储目录，请检查服务器权限");
        }
        // 保存文件
        file.transferTo(destFile);
    }
}



package com.q.library_management_system.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.io.File;

@Component
public class FileUploadUtil {
    @Value("${file.upload.path}")
    private String uploadPath;  // 服务器存储根路径

    @Value("${file.upload.access-path}")
    private String accessPath;  // 前端访问根路径

    /** 生成唯一文件名（避免重复） */
    public String generateFileName(String originalFilename) {
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        return UUID.randomUUID().toString() + suffix;
    }

    /** 获取服务器绝对存储路径 */
    public String getAbsoluteFilePath(String fileName) {
        // 移除 uploadPath 末尾可能存在的分隔符，统一通过 File.separator 拼接
        String normalizedUploadPath = uploadPath.replaceAll(File.separator + "$", "");
        // 使用 File.separator 自动适配系统分隔符
        return uploadPath + File.separator + fileName;
    }

    /** 获取前端访问URL */
    public String getAccessUrl(String fileName) {
        return accessPath + fileName;
    }
}


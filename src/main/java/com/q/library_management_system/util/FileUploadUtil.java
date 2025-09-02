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
        // 处理原始文件名为null的情况
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            originalFilename = "default_file"; // 默认文件名
        }
        // 提取扩展名（若没有扩展名则默认用 .dat）
        int lastDotIndex = originalFilename.lastIndexOf(".");
        String suffix = lastDotIndex == -1 ? ".dat" : originalFilename.substring(lastDotIndex);
        return UUID.randomUUID().toString() + suffix;
    }


    /** 获取服务器绝对存储路径 */
    public String getAbsoluteFilePath(String fileName) {
        // 移除 uploadPath 末尾可能存在的分隔符，统一拼接
        String normalizedUploadPath = uploadPath.replaceAll(File.separator + "$", "");
        // 使用处理后的路径拼接文件名
        return normalizedUploadPath + File.separator + fileName;
    }


    /** 获取前端访问URL */
    public String getAccessUrl(String fileName) {
        return accessPath + fileName;
    }
}


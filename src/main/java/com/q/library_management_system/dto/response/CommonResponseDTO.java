package com.q.library_management_system.dto.response;

import lombok.Data;

/**
 * 通用接口响应DTO
 * @param <T> 响应数据类型（列表、详情等）
 * 所有接口统一返回格式，包含状态码、消息和业务数据，前端可统一解析。
 */
@Data
public class CommonResponseDTO<T> {
    /** 状态码：200=成功，400=业务错误，401=未登录，403=无权限，500=系统异常 */
    private Integer code;

    /** 消息：成功/失败描述 */
    private String message;

    /** 业务数据：成功时返回，失败时为null */
    private T data;

    // -------------------------- 静态工厂方法（简化调用） --------------------------
    /** 成功（无数据） */
    public static <T> CommonResponseDTO<T> successWithoutData(String message) {
        CommonResponseDTO<T> response = new CommonResponseDTO<>();
        response.setCode(200);
        response.setMessage(message);
        response.setData(null); // 显式设为null，避免类型推断问题
        return response;
    }

    /** 成功（有数据） */
    public static <T> CommonResponseDTO<T> success(T data, String message) {
        CommonResponseDTO<T> response = new CommonResponseDTO<>();
        response.setCode(200);
        response.setData(data);
        response.setMessage(message);
        return response;
    }

    /** 失败（业务错误） */
    public static <T> CommonResponseDTO<T> fail(String message) {
        CommonResponseDTO<T> response = new CommonResponseDTO<>();
        response.setCode(400);
        response.setMessage(message);
        response.setData(null); // 显式设为null，支持任意T类型
        return response;
    }

    /** 失败（自定义状态码） */
    public static <T> CommonResponseDTO<T> fail(Integer code, String message) {
        CommonResponseDTO<T> response = new CommonResponseDTO<>();
        response.setCode(code);
        response.setMessage(message);
        response.setData(null); // 显式设为null，支持任意T类型
        return response;
    }

}



package com.q.library_management_system.dto.request;

import lombok.Data;
import javax.validation.constraints.Min;

/**
 * 用户分页查询请求DTO
 * 封装分页查询的条件参数
 */
@Data
public class UserPageQueryDTO {

    /** 当前页码（默认第1页） */
    @Min(value = 1, message = "页码不能小于1")
    private Integer currentPage = 1;

    /** 每页条数（默认10条，最大50条） */
    @Min(value = 1, message = "每页条数不能小于1")
    private Integer pageSize = 10;

    /** 关键词查询（可匹配用户名、真实姓名、手机号） */
    private String keyword;

    /** 用户类型筛选（可选：reader-读者，admin-管理员） */
    private String userType;

    /** 用户状态筛选（可选：normal-正常，frozen-冻结，deleted-已删除） */
    private String status;

}


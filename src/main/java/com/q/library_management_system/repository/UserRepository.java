package com.q.library_management_system.repository;

import com.q.library_management_system.dto.request.UserPageQueryDTO;
import com.q.library_management_system.entity.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// JpaRepository<实体类, 主键类型>：泛型需与实体类匹配
public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {
    // 自定义查询方法（JPA 会根据方法名自动生成 SQL）
    // 根据用户名查询用户（用于登录验证）
    Optional<User> findByUserName(String userName);

    // 根据手机号查询用户（用于注册时判断手机号是否已存在）
    boolean existsByPhone(String phone);

    // 判断用户名是否已存在（用于注册时的唯一性校验）
    boolean existsByUserName(String userName);

    // 根据用户名查询（排除已注销用户）
    Optional<User> findByUserNameAndStatusNot(String userName, User.UserStatus status);

    // 检查用户名是否已存在（排除当前用户和已注销用户）
    boolean existsByUserNameAndUserIdNotAndStatusNot(
            String userName,
            Integer userId,
            User.UserStatus status
    );

    // 统计已注销用户数量
    long countByStatus(User.UserStatus status);

    /**
     * 静态工具方法：构建用户查询条件
     * @param queryDTO 查询参数
     * @return 动态查询条件
     */
    static Specification<User> buildSpecification(UserPageQueryDTO queryDTO) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 关键词筛选（用户名、真实姓名、手机号）
            if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
                String likePattern = "%" + queryDTO.getKeyword() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("userName"), likePattern),
                        cb.like(root.get("realName"), likePattern),
                        cb.like(root.get("phone"), likePattern)
                ));
            }

            // 用户类型筛选
            if (queryDTO.getUserType() != null && !queryDTO.getUserType().isEmpty()) {
                predicates.add(cb.equal(
                        root.get("userType"),
                        User.UserType.valueOf(queryDTO.getUserType())
                ));
            }

            // 用户状态筛选
            if (queryDTO.getStatus() != null && !queryDTO.getStatus().isEmpty()) {
                predicates.add(cb.equal(
                        root.get("status"),
                        User.UserStatus.valueOf(queryDTO.getStatus())
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // 按用户类型统计数量（如管理员数量）
    long countByUserType(User.UserType userType);

}


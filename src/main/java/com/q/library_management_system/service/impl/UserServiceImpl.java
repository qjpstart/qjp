package com.q.library_management_system.service.impl;

import com.q.library_management_system.dto.request.*;
import com.q.library_management_system.dto.response.*;
import com.q.library_management_system.entity.User;
import com.q.library_management_system.entity.BorrowRecord;
import com.q.library_management_system.exception.BusinessException;
import com.q.library_management_system.repository.BorrowRecordRepository;
import com.q.library_management_system.repository.UserRepository;
import com.q.library_management_system.service.UserService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value; // 导入@Value注解

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {


    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // 密码加密器
    private final BorrowRecordRepository borrowRecordRepository;

    private StringRedisTemplate redisTemplate;

    // 关键：从配置文件注入JWT密钥（定义jwtSecret变量）
    @Value("${jwt.secret}") // 对应配置文件中的key
    private String jwtSecret; // 声明变量，解决"无法解析符号"错误
    private static final int TOKEN_EXPIRE_HOURS = 2;    // 令牌有效期（2小时）
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    @Override
    @Transactional
    public User register(User user) {
        // 检查用户名是否已存在
        if (userRepository.existsByUserName(user.getUserName())) {
            throw new BusinessException("用户名已存在：" + user.getUserName());
        }

        // 密码加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // 设置默认值
        user.setRegisterTime(LocalDateTime.now());
        user.setStatus(User.UserStatus.normal);
        user.setCreditScore(100); // 初始信用分100
        return userRepository.save(user);
    }

    @Override
    public User login(String userName, String password) {
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 检查账号状态
        if (user.getStatus() == User.UserStatus.frozen) {
            throw new BusinessException("账号已禁用");
        }

        // 脱敏处理（清除密码）
        user.setPassword(null);
        return user;
    }

    @Override
    public User getUserById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在：" + id));
    }


    @Override
    @Transactional
    public User updateUser(Integer id, User user) {
        User existing = getUserById(id);

        // 不允许修改用户名（如需修改需额外校验）
        if (!existing.getUserName().equals(user.getUserName())) {
            throw new BusinessException("不允许修改用户名");
        }

        // 更新可修改字段
        existing.setRealName(user.getRealName());
        existing.setPhone(user.getPhone());
        existing.setEmail(user.getEmail());
        return userRepository.save(existing);
    }

    @Override
    @Transactional
    public void updatePassword(Integer id, String oldPassword, String newPassword) {
        User user = getUserById(id);

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }

        // 加密新密码并保存
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateCreditScore(Integer userId, int score) {
        User user = getUserById(userId);
        int newScore = user.getCreditScore() + score;

        // 信用分范围限制（0-100）
        newScore = Math.max(0, Math.min(100, newScore));
        user.setCreditScore(newScore);

        // 信用分过低禁用账号
        if (newScore < 60) {
            user.setStatus(User.UserStatus.frozen);
        } else {
            user.setStatus(User.UserStatus.normal);
        }
        userRepository.save(user);
    }

    /**
     * 用户注册实现（核心逻辑）
     * 1. 参数校验（用户名/手机号唯一性、必填字段）
     * 2. DTO转实体（填充默认值、处理枚举类型）
     * 3. 密码加密（明文密码→加密存储）
     * 4. 保存数据库并返回响应DTO
     */
    @Override
    @Transactional // 注册是原子操作，确保数据一致性
    public UserInfoResponseDTO register(UserRegisterRequestDTO registerDTO) {
        // -------------------------- 1. 业务参数校验 --------------------------
        // 1.1 校验用户名是否已存在（实体类userName字段加了unique=true，数据库也会校验，这里提前校验更友好）
        boolean usernameExists = userRepository.existsByUserName(registerDTO.getUserName());
        if (usernameExists) {
            throw new BusinessException("用户名已被注册，请更换");
        }

        // 1.2 校验手机号是否已存在（实体类phone字段加了unique=true，提前校验）
        boolean phoneExists = userRepository.existsByPhone(registerDTO.getPhone());
        if (phoneExists) {
            throw new BusinessException("手机号已被注册，请更换");
        }

        // 1.3 校验必填字段（若DTO未加@NotNull注解，这里补充校验）
        if (registerDTO.getRealName() == null || registerDTO.getRealName().trim().isEmpty()) {
            throw new BusinessException("真实姓名不能为空");
        }
        if (registerDTO.getPassword() == null || registerDTO.getPassword().length() < 6) {
            throw new BusinessException("密码不能为空且长度不能小于6位");
        }

        // -------------------------- 2. DTO 转换为 User 实体 --------------------------
        User user = new User();
        // 复制DTO中与实体类字段名一致的属性（如userName/realName/phone/email）
        BeanUtils.copyProperties(registerDTO, user);

        // 2.1 填充实体类默认值（覆盖枚举默认值或补充未在DTO中传递的字段）
        user.setUserType(User.UserType.reader); // 强制默认普通读者（即使DTO传了也覆盖，避免越权注册管理员）
        user.setStatus(User.UserStatus.normal); // 账号默认正常状态
        user.setCreditScore(100); // 信用分默认100（实体类已有默认值，这里可写可不写，写了更明确）
        user.setRegisterTime(LocalDateTime.now()); // 注册时间取当前时间

        // 2.2 密码加密（核心！绝对不能存明文）
        String encryptedPassword = passwordEncoder.encode(registerDTO.getPassword());
        user.setPassword(encryptedPassword); // 存储加密后的密码

        // -------------------------- 3. 保存用户到数据库 --------------------------
        User savedUser = userRepository.save(user);

        // -------------------------- 4. 实体类转换为响应DTO（过滤敏感字段） --------------------------
        return convertToResponseDTO(savedUser);
    }

    /**
     * 工具方法：User实体 → UserResponseDTO（过滤密码等敏感字段）
     */
    private UserInfoResponseDTO convertToResponseDTO(User user) {
        UserInfoResponseDTO responseDTO = new UserInfoResponseDTO();
        // 复制非敏感字段（排除password字段）
        responseDTO.setUserId(user.getUserId());
        responseDTO.setUserName(user.getUserName());
        responseDTO.setRealName(user.getRealName()); // 实体类有realName，DTO对应字段也加上
        responseDTO.setPhone(user.getPhone());
        responseDTO.setEmail(user.getEmail());
        // 枚举类型处理：返回枚举的字符串值（前端更容易解析，如"reader"而非"UserType(reader)"）
        responseDTO.setUserType(user.getUserType().name()); // 如返回"reader"
        responseDTO.setStatus(user.getStatus().name()); // 如返回"normal"
        responseDTO.setCreditScore(user.getCreditScore());
        responseDTO.setRegisterTime(user.getRegisterTime());

        return responseDTO;
    }

    // 用户登录，参数为DTO对象
    @Override
    public UserLoginResponseDTO login(UserLoginRequestDTO loginDTO) {
        // 从DTO中提取参数
        String userName = loginDTO.getUserName();
        String password = loginDTO.getPassword();

        // 1. 查询用户
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));

        // 2. 校验密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 3. 校验账号状态
        if (user.getStatus() != User.UserStatus.normal) {
            throw new BusinessException("账号已被冻结或删除，无法登录");
        }

        // 4. 构建登录响应DTO（包含用户信息和令牌等）
        UserLoginResponseDTO responseDTO = new UserLoginResponseDTO();
        responseDTO.setUserId(user.getUserId());
        responseDTO.setUserName(user.getUserName());
        responseDTO.setUserType(user.getUserType().name());
        // 生成并设置令牌（实际项目中使用JWT等方式）
        responseDTO.setToken(generateToken(user));

        return responseDTO;
    }

    // 在UserServiceImpl中修改generateToken方法
    private String generateToken(User user) {
        // 1. 创建签名密钥（Java 7 兼容方式）
        Key key = new SecretKeySpec(jwtSecret.getBytes(), SignatureAlgorithm.HS256.getJcaName());

        // 2. 设置令牌自定义信息
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("userName", user.getUserName());
        claims.put("userType", user.getUserType().name());

        // 3. 计算过期时间（Java 7 使用Calendar处理）
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.HOUR, 2); // 有效期2小时
        Date expirationDate = calendar.getTime();

        // 4. 生成令牌（jjwt 0.9.1 语法）
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date()) // 签发时间
                .setExpiration(expirationDate) // 过期时间
                .signWith(SignatureAlgorithm.HS256, key) // 签名算法和密钥
                .compact();
    }

    /**
     * 根据用户ID查询用户信息
     */
    @Override
    public UserInfoResponseDTO getUserInfo(Integer userId) {
        // 1. 查询用户（不存在则抛出异常）
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        // 2. 转换为响应DTO（过滤敏感字段）
        UserInfoResponseDTO dto = new UserInfoResponseDTO();
        dto.setUserId(user.getUserId());
        dto.setUserName(user.getUserName());
        dto.setRealName(user.getRealName());
        dto.setPhone(user.getPhone());
        dto.setEmail(user.getEmail());
        dto.setUserType(user.getUserType().name());
        dto.setStatus(user.getStatus().name());
        dto.setCreditScore(user.getCreditScore());
        dto.setRegisterTime(user.getRegisterTime());

        return dto;
    }

    /**
     * 调整用户信用分
     * 1. 校验用户和操作人是否存在
     * 2. 计算调整后的信用分（确保不低于0）
     */
    @Override
    @Transactional
    public Integer adjustCredit(UserCreditAdjustRequestDTO requestDTO) {
        // 1. 校验用户是否存在
        User user = userRepository.findById(requestDTO.getUserId())
                .orElseThrow(() -> new BusinessException("目标用户不存在"));

        // 2. 校验操作人是否存在（可选，根据业务需求）
        User operator = userRepository.findById(requestDTO.getOperatorId())
                .orElseThrow(() -> new BusinessException("操作人不存在"));

        // 3. 计算调整后的信用分
        int currentCredit = user.getCreditScore();
        int newCredit = currentCredit + requestDTO.getCreditAdjust();

        // 4. 校验信用分不能为负数
        if (newCredit < 0) {
            throw new BusinessException("调整后信用分不能为负数，当前信用分：" + currentCredit);
        }

        // 5. 更新用户信用分
        user.setCreditScore(newCredit);
        userRepository.save(user);

        // 6. 返回结果（包含调整后的信用分）
        return newCredit;
    }

    // 修改密码统一使用DTO作为参数
    @Override
    public void updatePassword(UserPasswordUpdateRequestDTO requestDTO) {
        // 1. 校验新密码与确认密码是否一致
        if (!requestDTO.getNewPassword().equals(requestDTO.getConfirmPassword())) {
            throw new BusinessException("新密码与确认密码不一致");
        }

        // 2. 查询用户
        User user = userRepository.findById(requestDTO.getUserId())
                .orElseThrow(() -> new BusinessException("用户不存在"));

        // 3. 校验原密码是否正确
        if (!passwordEncoder.matches(requestDTO.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }

        // 4. 加密并更新新密码
        user.setPassword(passwordEncoder.encode(requestDTO.getNewPassword()));
        userRepository.save(user);
    }

    /**
     * 更新用户基本信息
     * 1. 校验用户是否存在
     * 2. 更新非敏感字段（姓名、手机号、邮箱等）
     * 3. 返回更新后的用户信息
     */
    @Override
    @Transactional
    public UserInfoResponseDTO updateUserInfo(UserInfoUpdateRequestDTO requestDTO) {
        // 1. 校验用户是否存在
        User user = userRepository.findById(requestDTO.getUserId())
                .orElseThrow(() -> new BusinessException("用户不存在"));

        // 2. 更新字段（只更新非空的传入字段）
        if (requestDTO.getRealName() != null) {
            user.setRealName(requestDTO.getRealName());
        }
        if (requestDTO.getPhone() != null) {
            user.setPhone(requestDTO.getPhone());
        }
        if (requestDTO.getEmail() != null) {
            user.setEmail(requestDTO.getEmail());
        }
        // 可根据业务需求添加更多可更新字段（如头像、地址等）

        // 3. 保存更新
        User updatedUser = userRepository.save(user);

        // 4. 转换为响应DTO并返回
        UserInfoResponseDTO responseDTO = new UserInfoResponseDTO();
        responseDTO.setUserId(updatedUser.getUserId());
        responseDTO.setUserName(updatedUser.getUserName());
        responseDTO.setRealName(updatedUser.getRealName());
        responseDTO.setPhone(updatedUser.getPhone());
        responseDTO.setEmail(updatedUser.getEmail());
        responseDTO.setUserType(updatedUser.getUserType().name());
        responseDTO.setStatus(updatedUser.getStatus().name());
        responseDTO.setCreditScore(updatedUser.getCreditScore());
        responseDTO.setRegisterTime(updatedUser.getRegisterTime());

        return responseDTO;
    }

    /**
     * 变更用户状态（禁用/启用账号）
     * 仅管理员可操作，用于处理违规用户或恢复正常账号
     */
    @Override
    @Transactional
    public void changeUserStatus(UserStatusChangeRequestDTO requestDTO) {
        // 1. 校验目标用户是否存在
        User user = userRepository.findById(requestDTO.getUserId())
                .orElseThrow(() -> new BusinessException("目标用户不存在"));

        // 2. 校验操作人是否为管理员
        User operator = userRepository.findById(requestDTO.getOperatorId())
                .orElseThrow(() -> new BusinessException("操作人不存在"));

        // 检查操作人权限（必须是admin类型）
        if (!User.UserType.admin.equals(operator.getUserType())) {
            throw new BusinessException("权限不足，仅管理员可变更用户状态");
        }

        // 3. 校验状态是否重复（避免无效操作）
        if (requestDTO.getTargetStatus().equals(user.getStatus())) {
            throw new BusinessException("用户当前已处于" + requestDTO.getTargetStatus() + "状态");
        }

        // 4. 执行状态变更
        user.setStatus(requestDTO.getTargetStatus());
        userRepository.save(user);

        // 5. 可选：记录状态变更日志（用于审计）
        // statusChangeLogService.recordLog(requestDTO, user.getStatus());
    }

    /**
     * JWT登出逻辑：将令牌加入黑名单，有效期与原令牌一致
     */
    @Override
    public void logout(String token) {
        try {
            // 1. 验证令牌有效性（确保是未过期的合法令牌）
            Claims claims = Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .parseClaimsJws(token)
                    .getBody();

            // 2. 计算令牌剩余有效期（秒）
            long expireTime = claims.getExpiration().getTime();
            long currentTime = System.currentTimeMillis();
            long remainSeconds = (expireTime - currentTime) / 1000;

            if (remainSeconds <= 0) {
                throw new BusinessException("令牌已过期");
            }

            // 3. 将令牌加入Redis黑名单（有效期与剩余时间一致）
            String blacklistKey = BLACKLIST_PREFIX + token;
            redisTemplate.opsForValue().set(blacklistKey, "1", 7200 * 1000);

        } catch (Exception e) {
            throw new BusinessException("令牌无效或已过期");
        }
    }

    /**
     * 分页查询用户列表
     * 支持按关键词、用户类型、状态等条件筛选
     */
    @Override
    public PageResultDTO<UserItemDTO> getUserPage(UserPageQueryDTO queryDTO) {
        // 1. 构建分页参数
        Pageable pageable = PageRequest.of(
                queryDTO.getCurrentPage() - 1,
                queryDTO.getPageSize()
        );

        // 2. 构建动态查询条件（调用Repository的静态方法）
        Specification<User> spec = UserRepository.buildSpecification(queryDTO);

        // 3. 执行分页查询（使用JpaSpecificationExecutor的findAll方法）
        Page<User> userPage = userRepository.findAll(spec, pageable);

        // 4. 转换结果并返回（同上）
        List<UserItemDTO> userItemList = userPage.getContent().stream()
                .map(UserItemDTO::fromEntity)
                .collect(Collectors.toList());

        return PageResultDTO.build(
                userItemList,
                userPage.getTotalElements(),
                queryDTO.getCurrentPage(),
                queryDTO.getPageSize()
        );
    }

    // 根据用户名查询用户
    @Override
    public User findByUsername(String username) {
        return userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    // 用户自我删除（验证密码和用户ID匹配）
    public void deleteSelfAccount(Integer userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 验证密码（需和数据库中的加密密码比对）
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("密码错误，删除失败");
        }

        userRepository.delete(user);
    }

    // 管理员删除用户（验证管理员权限）
    public CommonResponseDTO<?> deleteUserByAdmin(UserDeleteRequestDTO requestDTO, String currentUsername) {
        User admin = findByUsername(currentUsername);
        if (!User.UserType.admin.equals(admin.getUserType())) {
            return CommonResponseDTO.fail("权限不足：仅管理员可删除用户");
        }

        userRepository.deleteById(requestDTO.getUserId());
        return CommonResponseDTO.successWithoutData("用户删除成功");
    }

    /**
     * 修改密码（带用户ID参数，确保只能修改指定用户）
     */
    @Override
    @Transactional
    public void updatePassword(Integer userId, UserPasswordUpdateRequestDTO requestDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        // 验证原密码
        if (!passwordEncoder.matches(requestDTO.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }

        // 验证新密码一致性
        if (!requestDTO.getNewPassword().equals(requestDTO.getConfirmPassword())) {
            throw new BusinessException("新密码与确认密码不一致");
        }

        // 加密并更新新密码
        user.setPassword(passwordEncoder.encode(requestDTO.getNewPassword()));
        userRepository.save(user);
    }

    //更新用户信息
    @Override
    public UserInfoResponseDTO updateUserInfo(Integer userId, UserInfoUpdateRequestDTO requestDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        // 更新用户信息逻辑...
        user.setRealName(requestDTO.getRealName());
        user.setPhone(requestDTO.getPhone());
        user.setEmail(requestDTO.getEmail());

        User updatedUser = userRepository.save(user);
        return getUserInfo(updatedUser.getUserId());
    }

    /**
     * 管理员删除用户（仅接收用户ID参数）
     */
    @Override
    @Transactional
    public void deleteUserByAdmin(Integer userId) {
        // 1. 查询目标用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        // 2. 获取当前管理员信息（用于校验是否删除自己）
        String currentAdminUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentAdmin = findByUsername(currentAdminUsername);

        // 3. 禁止删除自己
        if (user.getUserId().equals(currentAdmin.getUserId())) {
            throw new BusinessException("不能删除自己的管理员账号");
        }

        // 4. 执行逻辑删除
        user.setStatus(User.UserStatus.deleted);
        userRepository.save(user);
    }

    // 获取用户统计信息的方法声明
    @Override
    public String getUserStatistics() {
        long totalUserCount = userRepository.count();
        long adminCount = userRepository.countByUserType(User.UserType.admin);
        long normalUserCount = totalUserCount - adminCount;
        long frozenUserCount = userRepository.countByStatus(User.UserStatus.frozen);

        return String.format(
                "用户总数：%d，管理员：%d，普通用户：%d，冻结用户：%d",
                totalUserCount, adminCount, normalUserCount, frozenUserCount
        );
    }

    /**
     * 管理员重置用户密码为默认值（123456）
     */
    @Override
    @Transactional
    public void resetPasswordByAdmin(Integer userId) {
        // 1. 校验用户是否存在
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在：" + userId));

        // 2. 校验是否为管理员自身（避免误操作）
        if (User.UserType.admin.equals(user.getUserType())) {
            throw new BusinessException("不允许重置管理员账号密码");
        }

        // 3. 加密默认密码（123456）并更新
        String defaultPassword = "123456";
        user.setPassword(passwordEncoder.encode(defaultPassword));
        userRepository.save(user);
    }
}


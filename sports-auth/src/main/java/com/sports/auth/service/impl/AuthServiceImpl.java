package com.sports.auth.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.sports.auth.dto.LoginDTO;
import com.sports.auth.dto.LoginResponseDTO;
import com.sports.auth.dto.RegisterDTO;
import com.sports.auth.feign.UserFeignClient;
import com.sports.auth.service.AuthService;
import com.sports.auth.service.SmsService;
import com.sports.auth.strategy.LoginStrategy;
import com.sports.common.constant.CommonConstant;
import com.sports.common.constant.HttpStatusConstant;
import com.sports.common.constant.MessageConstant;
import com.sports.common.constant.RedisKeyConstant;
import com.sports.common.dto.UserDTO;
import com.sports.common.entity.Result;
import com.sports.common.enums.SmsTypeEnum;
import com.sports.common.enums.UserStatusEnum;
import com.sports.common.exception.BusinessException;
import com.sports.common.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 鉴权服务实现类
 * 实现用户登录、注册、退出登录等核心业务逻辑
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    // 用户服务Feign客户端，用于远程调用用户服务
    @Autowired
    private UserFeignClient userFeignClient;

    // 短信验证码服务，用于发送和验证验证码
    @Autowired
    private SmsService smsService;

    // JWT工具类，用于生成和解析令牌
    @Autowired
    private JwtUtil jwtUtil;

    // Redis操作模板，用于操作黑名单等
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 登录策略映射表
     * 使用策略模式，根据不同的登录类型选择不同的登录策略
     * key: 登录类型（username-用户名密码登录，phone-手机号验证码登录）
     * value: 对应的登录策略实现类
     */
    private final Map<String, LoginStrategy> loginStrategyMap = new ConcurrentHashMap<>();

    /**
     * 构造函数：初始化登录策略
     * Spring会自动注入所有实现LoginStrategy接口的Bean
     *
     * @param loginStrategies 所有登录策略实现类的列表
     */
    @Autowired
    public AuthServiceImpl(List<LoginStrategy> loginStrategies) {
        // 遍历所有登录策略，将其放入映射表中
        for (LoginStrategy strategy : loginStrategies) {
            // key为登录类型，value为策略实现
            loginStrategyMap.put(strategy.getLoginType(), strategy);
        }
        // 日志记录初始化完成的登录类型
        log.info("登录策略初始化完成，支持的登录类型: {}", loginStrategyMap.keySet());
    }

    /**
     * 1. 用户登录
     * 支持用户名密码登录和手机号验证码登录两种方式
     * 使用策略模式根据登录类型选择对应的登录策略
     *
     * @param loginDTO 登录参数，包含登录类型和对应凭证
     * @return 登录响应，包含JWT令牌和用户信息
     */
    @Override
    public LoginResponseDTO login(LoginDTO loginDTO) {
        // 从登录参数中获取登录类型
        String loginType = loginDTO.getLoginType();
        // 根据登录类型从映射表中获取对应的登录策略
        LoginStrategy strategy = loginStrategyMap.get(loginType);

        // 如果没有找到对应的登录策略，抛出业务异常
        if (strategy == null) {
            throw new BusinessException("不支持的登录类型: " + loginType);
        }

        // 日志记录当前使用的登录策略
        log.info("使用登录策略: {}", loginType);
        // 调用登录策略的login方法执行实际的登录逻辑
        return strategy.login(loginDTO);
    }

    /**
     * 2. 用户注册
     * 新用户注册流程：验证验证码 -> 检查用户名和手机号是否已存在 -> 加密密码 -> 保存用户
     *
     * @param registerDTO 注册参数，包含用户名、密码、手机号、验证码等
     * @return 是否注册成功
     */
    @Override
    public Boolean register(RegisterDTO registerDTO) {
        // 2.1 验证手机号验证码
        // 调用短信服务验证用户输入的验证码是否正确
        smsService.validateCode(
            registerDTO.getPhone(),  // 用户手机号
            registerDTO.getCode(),   // 用户输入的验证码
            SmsTypeEnum.REGISTER.getCode()  // 验证码类型为注册
        );

        // 2.2 检查用户名是否已存在
        // 通过Feign远程调用用户服务，根据用户名查询用户
        Result<UserDTO> usernameResult = userFeignClient.getByUsername(registerDTO.getUsername());
        // 如果查询结果不为null，说明用户名已存在
        if (usernameResult.getData() != null) {
            // 抛出业务异常，提示用户名已存在
            throw new BusinessException(MessageConstant.USERNAME_EXISTS);
        }

        // 2.3 检查手机号是否已存在
        // 通过Feign远程调用用户服务，根据手机号查询用户
        Result<UserDTO> phoneResult = userFeignClient.getByPhone(registerDTO.getPhone());
        // 如果查询结果不为null，说明手机号已存在
        if (phoneResult.getData() != null) {
            // 抛出业务异常，提示手机号已存在
            throw new BusinessException(MessageConstant.PHONE_EXISTS);
        }

        // 2.4 构建用户对象
        // 创建用户DTO对象
        UserDTO userDTO = new UserDTO();
        // 将注册参数的属性复制到用户DTO中
        BeanUtils.copyProperties(registerDTO, userDTO);
        // 使用BCrypt对密码进行加密存储
        userDTO.setPassword(BCrypt.hashpw(registerDTO.getPassword()));
        // 设置用户状态为激活状态
        userDTO.setStatus(UserStatusEnum.ACTIVE.getCode());
        // 2.5 如果用户没有提供昵称，设置默认昵称
        if (userDTO.getNickname() == null || userDTO.getNickname().trim().isEmpty()) {
            // 默认昵称格式：用户_手机号后4位
            userDTO.setNickname(
                CommonConstant.DEFAULT_NICKNAME_PREFIX +  // 前缀：用户_
                registerDTO.getPhone().substring(7)  // 手机号后4位
            );
        }

        // 2.6 保存用户信息
        // 通过Feign远程调用用户服务，保存用户信息
        Result<Boolean> result = userFeignClient.saveUser(userDTO);
        // 返回保存结果：HTTP状态码为200且返回值为true时表示成功
        return result.getCode() == HttpStatusConstant.SUCCESS && Boolean.TRUE.equals(result.getData());
    }

    /**
     * 3. 用户退出登录
     * 将当前令牌加入黑名单，使其失效
     *
     * @param token JWT令牌（带Bearer前缀）
     * @return 是否退出成功
     */
    @Override
    public Boolean logout(String token) {
        // 如果token为空或为null，直接返回成功（因为没有令牌需要失效）
        if (token == null || token.isEmpty()) {
            return true;
        }

        try {
            // 3.1 从请求头中提取JWT令牌
            // 去除Bearer前缀，获取实际的令牌字符串
            String actualToken = jwtUtil.extractToken(token);

            // 3.2 解析令牌获取用户ID
            Long userId = jwtUtil.getUserIdFromToken(actualToken);
            // 如果成功获取到用户ID
            if (userId != null) {
                // 3.3 计算令牌剩余有效期
                // 获取令牌过期时间的时间戳
                long expirationTime = jwtUtil.getExpirationDateFromToken(actualToken).getTime();
                // 计算剩余时间：过期时间戳 - 当前时间戳
                long remainingTime = expirationTime - System.currentTimeMillis();
                // 如果剩余时间大于0，说明令牌还未过期
                if (remainingTime > 0) {
                    // 3.4 将令牌加入黑名单（Redis中存储）
                    // 构建黑名单的Redis Key
                    String blacklistKey = RedisKeyConstant.getTokenBlacklistKey(actualToken);
                    // 将令牌存入Redis，value为用户ID，过期时间为令牌剩余有效期
                    stringRedisTemplate.opsForValue().set(
                        blacklistKey,           // Redis Key
                        String.valueOf(userId),  // Redis Value：用户ID
                        remainingTime,           // 过期时间：毫秒
                        TimeUnit.MILLISECONDS    // 时间单位：毫秒
                    );
                }
            }
        } catch (Exception e) {
            // 如果Token解析失败（可能已过期），记录警告日志，但不影响退出登录结果
            log.warn("Token解析失败，可能已过期: {}", e.getMessage());
        }

        // 无论是否成功加入黑名单，都返回退出成功
        return true;
    }
}

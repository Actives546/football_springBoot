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
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private SmsService smsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 登录策略映射表
     * key: 登录类型（username/phone）
     * value: 对应的登录策略实现
     */
    private final Map<String, LoginStrategy> loginStrategyMap = new ConcurrentHashMap<>();

    /**
     * 构造函数：初始化登录策略
     *
     * @param loginStrategies 所有登录策略实现
     */
    @Autowired
    public AuthServiceImpl(List<LoginStrategy> loginStrategies) {
        for (LoginStrategy strategy : loginStrategies) {
            loginStrategyMap.put(strategy.getLoginType(), strategy);
        }
        log.info("登录策略初始化完成，支持的登录类型: {}", loginStrategyMap.keySet());
    }

    /**
     * 1. 用户登录
     * 支持用户名密码登录和手机号验证码登录两种方式
     *
     * 1.1 根据登录类型选择对应的登录策略
     * 1.2 执行登录策略，获取登录响应
     *
     * @param loginDTO 登录参数
     * @return 登录响应，包含JWT令牌和用户信息
     */
    @Override
    public LoginResponseDTO login(LoginDTO loginDTO) {
        String loginType = loginDTO.getLoginType();
        LoginStrategy strategy = loginStrategyMap.get(loginType);

        if (strategy == null) {
            throw new BusinessException("不支持的登录类型: " + loginType);
        }

        log.info("使用登录策略: {}", loginType);
        return strategy.login(loginDTO);
    }

    /**
     * 2. 用户注册
     *
     * 2.1 验证手机号验证码
     * 2.2 检查用户名是否已存在
     * 2.3 检查手机号是否已存在
     * 2.4 加密密码
     * 2.5 设置默认昵称（如果未提供）
     * 2.6 保存用户信息
     *
     * @param registerDTO 注册参数
     * @return 是否注册成功
     */
    @Override
    public Boolean register(RegisterDTO registerDTO) {
        smsService.validateCode(registerDTO.getPhone(), registerDTO.getCode(), SmsTypeEnum.REGISTER.getCode());

        Result<UserDTO> usernameResult = userFeignClient.getByUsername(registerDTO.getUsername());
        if (usernameResult.getData() != null) {
            throw new BusinessException(MessageConstant.USERNAME_EXISTS);
        }

        Result<UserDTO> phoneResult = userFeignClient.getByPhone(registerDTO.getPhone());
        if (phoneResult.getData() != null) {
            throw new BusinessException(MessageConstant.PHONE_EXISTS);
        }

        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(registerDTO, userDTO);
        userDTO.setPassword(BCrypt.hashpw(registerDTO.getPassword()));
        userDTO.setStatus(UserStatusEnum.ACTIVE.getCode());
        if (userDTO.getNickname() == null || userDTO.getNickname().trim().isEmpty()) {
            userDTO.setNickname(CommonConstant.DEFAULT_NICKNAME_PREFIX + registerDTO.getPhone().substring(7));
        }

        Result<Boolean> result = userFeignClient.saveUser(userDTO);
        return result.getCode() == HttpStatusConstant.SUCCESS && Boolean.TRUE.equals(result.getData());
    }

    /**
     * 3. 用户退出登录
     *
     * 3.1 从请求头中提取JWT令牌
     * 3.2 解析令牌获取用户ID
     * 3.3 将令牌加入黑名单（Redis中存储）
     * 3.4 黑名单有效期与令牌剩余有效期一致
     *
     * @param token JWT令牌（带Bearer前缀）
     * @return 是否退出成功
     */
    @Override
    public Boolean logout(String token) {
        if (token == null || token.isEmpty()) {
            return true;
        }

        try {
            String actualToken = jwtUtil.extractToken(token);

            Long userId = jwtUtil.getUserIdFromToken(actualToken);
            if (userId != null) {
                long remainingTime = jwtUtil.getExpirationDateFromToken(actualToken).getTime() - System.currentTimeMillis();
                if (remainingTime > 0) {
                    stringRedisTemplate.opsForValue().set(
                            RedisKeyConstant.getTokenBlacklistKey(actualToken),
                            String.valueOf(userId),
                            remainingTime,
                            TimeUnit.MILLISECONDS
                    );
                }
            }
        } catch (Exception e) {
            log.warn("Token解析失败，可能已过期: {}", e.getMessage());
        }

        return true;
    }
}

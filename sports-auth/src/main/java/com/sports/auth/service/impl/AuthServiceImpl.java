package com.sports.auth.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.sports.auth.dto.LoginByPhoneDTO;
import com.sports.auth.dto.LoginByUsernameDTO;
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
import com.sports.common.enums.LoginTypeEnum;
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

    private final Map<String, LoginStrategy> loginStrategyMap = new ConcurrentHashMap<>();

    @Autowired
    public AuthServiceImpl(List<LoginStrategy> loginStrategies) {
        for (LoginStrategy strategy : loginStrategies) {
            loginStrategyMap.put(strategy.getLoginType(), strategy);
        }
        log.info("登录策略初始化完成，支持的登录类型: {}", loginStrategyMap.keySet());
    }

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

    @Override
    public LoginResponseDTO loginByUsername(LoginByUsernameDTO loginDTO) {
        LoginDTO login = new LoginDTO();
        login.setLoginType(LoginTypeEnum.USERNAME.getCode());
        login.setUsername(loginDTO.getUsername());
        login.setPassword(loginDTO.getPassword());
        return login(login);
    }

    @Override
    public LoginResponseDTO loginByPhone(LoginByPhoneDTO loginDTO) {
        LoginDTO login = new LoginDTO();
        login.setLoginType(LoginTypeEnum.PHONE.getCode());
        login.setPhone(loginDTO.getPhone());
        login.setCode(loginDTO.getCode());
        return login(login);
    }

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

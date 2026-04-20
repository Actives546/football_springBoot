package com.sports.auth.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.sports.auth.dto.LoginByPhoneDTO;
import com.sports.auth.dto.LoginByUsernameDTO;
import com.sports.auth.dto.LoginResponseDTO;
import com.sports.auth.dto.RegisterDTO;
import com.sports.auth.feign.UserFeignClient;
import com.sports.auth.service.AuthService;
import com.sports.auth.service.SmsService;
import com.sports.auth.util.JwtUtil;
import com.sports.common.dto.UserDTO;
import com.sports.common.entity.Result;
import com.sports.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    @Override
    public LoginResponseDTO loginByUsername(LoginByUsernameDTO loginDTO) {
        Result<UserDTO> result = userFeignClient.getByUsername(loginDTO.getUsername());
        if (result.getCode() != 200 || result.getData() == null) {
            throw new BusinessException("用户名或密码错误");
        }

        UserDTO user = result.getData();

        if (user.getStatus() != 1) {
            throw new BusinessException("用户已被禁用");
        }

        if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        return buildLoginResponse(user);
    }

    @Override
    public LoginResponseDTO loginByPhone(LoginByPhoneDTO loginDTO) {
        smsService.validateCode(loginDTO.getPhone(), loginDTO.getCode(), "login");

        Result<UserDTO> result = userFeignClient.getByPhone(loginDTO.getPhone());
        if (result.getCode() != 200 || result.getData() == null) {
            throw new BusinessException("该手机号未注册");
        }

        UserDTO user = result.getData();

        if (user.getStatus() != 1) {
            throw new BusinessException("用户已被禁用");
        }

        return buildLoginResponse(user);
    }

    @Override
    public Boolean register(RegisterDTO registerDTO) {
        smsService.validateCode(registerDTO.getPhone(), registerDTO.getCode(), "register");

        Result<UserDTO> usernameResult = userFeignClient.getByUsername(registerDTO.getUsername());
        if (usernameResult.getData() != null) {
            throw new BusinessException("用户名已存在");
        }

        Result<UserDTO> phoneResult = userFeignClient.getByPhone(registerDTO.getPhone());
        if (phoneResult.getData() != null) {
            throw new BusinessException("手机号已注册");
        }

        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(registerDTO, userDTO);
        userDTO.setPassword(BCrypt.hashpw(registerDTO.getPassword()));
        userDTO.setStatus(1);
        if (userDTO.getNickname() == null || userDTO.getNickname().trim().isEmpty()) {
            userDTO.setNickname("用户" + registerDTO.getPhone().substring(7));
        }

        Result<Boolean> result = userFeignClient.saveUser(userDTO);
        return result.getCode() == 200 && Boolean.TRUE.equals(result.getData());
    }

    @Override
    public Boolean logout(String token) {
        if (token == null || token.isEmpty()) {
            return true;
        }

        try {
            String actualToken = token;
            if (token.startsWith(jwtUtil.getPrefix() + " ")) {
                actualToken = token.substring(jwtUtil.getPrefix().length() + 1);
            }

            Long userId = jwtUtil.getUserIdFromToken(actualToken);
            if (userId != null) {
                long remainingTime = jwtUtil.getExpirationDateFromToken(actualToken).getTime() - System.currentTimeMillis();
                if (remainingTime > 0) {
                    stringRedisTemplate.opsForValue().set(
                            TOKEN_BLACKLIST_PREFIX + actualToken,
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

    private LoginResponseDTO buildLoginResponse(UserDTO user) {
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        LoginResponseDTO response = new LoginResponseDTO();
        response.setToken(token);
        response.setTokenType(jwtUtil.getPrefix());
        response.setExpiresIn(expiration);
        response.setUser(user);

        return response;
    }
}

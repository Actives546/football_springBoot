package com.sports.auth.strategy;

import cn.hutool.crypto.digest.BCrypt;
import com.sports.auth.dto.LoginDTO;
import com.sports.auth.dto.LoginResponseDTO;
import com.sports.auth.feign.UserFeignClient;
import com.sports.common.constant.HttpStatusConstant;
import com.sports.common.constant.MessageConstant;
import com.sports.common.dto.UserDTO;
import com.sports.common.entity.Result;
import com.sports.common.enums.LoginTypeEnum;
import com.sports.common.enums.UserStatusEnum;
import com.sports.common.exception.BusinessException;
import com.sports.common.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 用户名密码登录策略
 */
@Slf4j
@Component
public class UsernamePasswordLoginStrategy implements LoginStrategy {

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 1. 用户名密码登录
     *
     * 1.1 验证参数
     *     - 检查用户名是否为空
     *     - 检查密码是否为空
     *
     * 1.2 查询用户信息
     *     - 通过Feign调用用户服务，根据用户名查询用户
     *     - 如果用户不存在，抛出"用户名或密码错误"异常
     *
     * 1.3 检查用户状态
     *     - 检查用户是否被禁用
     *     - 如果被禁用，抛出"用户已被禁用"异常
     *
     * 1.4 验证密码
     *     - 使用BCrypt比对输入的密码和存储的密码
     *     - 如果密码不匹配，抛出"用户名或密码错误"异常
     *
     * 1.5 构建登录响应
     *     - 生成JWT令牌
     *     - 封装用户信息和令牌信息
     *
     * @param loginDTO 登录参数
     * @return 登录响应
     */
    @Override
    public LoginResponseDTO login(LoginDTO loginDTO) {
        log.info("执行用户名密码登录策略");

        if (!StringUtils.hasText(loginDTO.getUsername())) {
            throw new BusinessException(MessageConstant.USERNAME_NOT_BLANK);
        }
        if (!StringUtils.hasText(loginDTO.getPassword())) {
            throw new BusinessException(MessageConstant.PASSWORD_NOT_BLANK);
        }

        Result<UserDTO> result = userFeignClient.getByUsername(loginDTO.getUsername());
        if (result.getCode() != HttpStatusConstant.SUCCESS || result.getData() == null) {
            throw new BusinessException(MessageConstant.USERNAME_OR_PASSWORD_ERROR);
        }

        UserDTO user = result.getData();

        if (!UserStatusEnum.isActive(user.getStatus())) {
            throw new BusinessException(MessageConstant.USER_DISABLED);
        }

        if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(MessageConstant.USERNAME_OR_PASSWORD_ERROR);
        }

        return buildLoginResponse(user);
    }

    @Override
    public String getLoginType() {
        return LoginTypeEnum.USERNAME.getCode();
    }

    /**
     * 构建登录响应
     *
     * @param user 用户信息
     * @return 登录响应DTO
     */
    private LoginResponseDTO buildLoginResponse(UserDTO user) {
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        LoginResponseDTO response = new LoginResponseDTO();
        response.setToken(token);
        response.setTokenType(jwtUtil.getPrefix());
        response.setExpiresIn(jwtUtil.getExpiration());
        response.setUser(user);

        return response;
    }
}

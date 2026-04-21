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

@Slf4j
@Component
public class UsernamePasswordLoginStrategy implements LoginStrategy {

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private JwtUtil jwtUtil;

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

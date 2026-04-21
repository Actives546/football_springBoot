package com.sports.auth.strategy;

import com.sports.auth.dto.LoginDTO;
import com.sports.auth.dto.LoginResponseDTO;
import com.sports.auth.feign.UserFeignClient;
import com.sports.auth.service.SmsService;
import com.sports.common.constant.HttpStatusConstant;
import com.sports.common.constant.MessageConstant;
import com.sports.common.dto.UserDTO;
import com.sports.common.entity.Result;
import com.sports.common.enums.LoginTypeEnum;
import com.sports.common.enums.SmsTypeEnum;
import com.sports.common.enums.UserStatusEnum;
import com.sports.common.exception.BusinessException;
import com.sports.common.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class PhoneCodeLoginStrategy implements LoginStrategy {

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private SmsService smsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public LoginResponseDTO login(LoginDTO loginDTO) {
        log.info("执行手机号验证码登录策略");

        if (!StringUtils.hasText(loginDTO.getPhone())) {
            throw new BusinessException(MessageConstant.PHONE_NOT_BLANK);
        }
        if (!StringUtils.hasText(loginDTO.getCode())) {
            throw new BusinessException(MessageConstant.CODE_NOT_BLANK);
        }

        smsService.validateCode(loginDTO.getPhone(), loginDTO.getCode(), SmsTypeEnum.LOGIN.getCode());

        Result<UserDTO> result = userFeignClient.getByPhone(loginDTO.getPhone());
        if (result.getCode() != HttpStatusConstant.SUCCESS || result.getData() == null) {
            throw new BusinessException(MessageConstant.PHONE_NOT_REGISTERED);
        }

        UserDTO user = result.getData();

        if (!UserStatusEnum.isActive(user.getStatus())) {
            throw new BusinessException(MessageConstant.USER_DISABLED);
        }

        return buildLoginResponse(user);
    }

    @Override
    public String getLoginType() {
        return LoginTypeEnum.PHONE.getCode();
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

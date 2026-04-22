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

/**
 * 手机号验证码登录策略
 */
@Slf4j
@Component
public class PhoneCodeLoginStrategy implements LoginStrategy {

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private SmsService smsService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 1. 手机号验证码登录
     *
     * 1.1 验证参数
     *     - 检查手机号是否为空
     *     - 检查验证码是否为空
     *
     * 1.2 验证验证码
     *     - 调用SmsService验证手机号和验证码
     *     - 如果验证失败，抛出对应异常
     *
     * 1.3 查询用户信息
     *     - 通过Feign调用用户服务，根据手机号查询用户
     *     - 如果用户不存在，抛出"手机号未注册"异常
     *
     * 1.4 检查用户状态
     *     - 检查用户是否被禁用
     *     - 如果被禁用，抛出"用户已被禁用"异常
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

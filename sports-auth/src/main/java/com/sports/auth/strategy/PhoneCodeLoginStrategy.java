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
 * 实现LoginStrategy接口，提供手机号验证码登录的具体实现
 */
@Slf4j
@Component
public class PhoneCodeLoginStrategy implements LoginStrategy {

    // 用户服务Feign客户端，用于远程调用用户服务
    @Autowired
    private UserFeignClient userFeignClient;

    // 短信验证码服务，用于验证验证码
    @Autowired
    private SmsService smsService;

    // JWT工具类，用于生成令牌
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 1. 手机号验证码登录
     * 流程：验证参数 -> 验证验证码 -> 查询用户信息 -> 检查用户状态 -> 构建登录响应
     *
     * @param loginDTO 登录参数，包含手机号和验证码
     * @return 登录响应，包含JWT令牌和用户信息
     */
    @Override
    public LoginResponseDTO login(LoginDTO loginDTO) {
        // 日志记录当前执行的登录策略
        log.info("执行手机号验证码登录策略");

        // 1.1 验证参数
        // 检查手机号是否为空
        if (!StringUtils.hasText(loginDTO.getPhone())) {
            // 如果手机号为空，抛出业务异常
            throw new BusinessException(MessageConstant.PHONE_NOT_BLANK);
        }
        // 检查验证码是否为空
        if (!StringUtils.hasText(loginDTO.getCode())) {
            // 如果验证码为空，抛出业务异常
            throw new BusinessException(MessageConstant.CODE_NOT_BLANK);
        }

        // 1.2 验证验证码
        // 调用短信服务验证用户输入的验证码是否正确
        smsService.validateCode(
            loginDTO.getPhone(),  // 用户手机号
            loginDTO.getCode(),   // 用户输入的验证码
            SmsTypeEnum.LOGIN.getCode()  // 验证码类型为登录
        );

        // 1.3 查询用户信息
        // 通过Feign远程调用用户服务，根据手机号查询用户
        Result<UserDTO> result = userFeignClient.getByPhone(loginDTO.getPhone());
        // 检查返回结果：HTTP状态码不是200 或 用户数据为null
        if (result.getCode() != HttpStatusConstant.SUCCESS || result.getData() == null) {
            // 手机号未注册，抛出业务异常
            throw new BusinessException(MessageConstant.PHONE_NOT_REGISTERED);
        }

        // 从返回结果中获取用户信息
        UserDTO user = result.getData();

        // 1.4 检查用户状态
        // 检查用户是否处于激活状态
        if (!UserStatusEnum.isActive(user.getStatus())) {
            // 如果用户被禁用，抛出业务异常
            throw new BusinessException(MessageConstant.USER_DISABLED);
        }

        // 1.5 构建登录响应
        // 调用私有方法构建包含JWT令牌的登录响应
        return buildLoginResponse(user);
    }

    /**
     * 获取登录类型
     * 返回当前策略支持的登录类型标识
     *
     * @return 登录类型：phone-手机号验证码登录
     */
    @Override
    public String getLoginType() {
        // 返回登录类型枚举的code值
        return LoginTypeEnum.PHONE.getCode();
    }

    /**
     * 构建登录响应
     * 私有方法，用于生成JWT令牌并封装登录响应
     *
     * @param user 用户信息
     * @return 登录响应DTO，包含JWT令牌和用户信息
     */
    private LoginResponseDTO buildLoginResponse(UserDTO user) {
        // 1.5.1 生成JWT令牌
        // 使用用户ID和用户名作为载荷生成令牌
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        // 1.5.2 创建登录响应对象
        LoginResponseDTO response = new LoginResponseDTO();
        // 设置JWT令牌
        response.setToken(token);
        // 设置令牌类型（Bearer）
        response.setTokenType(jwtUtil.getPrefix());
        // 设置令牌有效期（毫秒）
        response.setExpiresIn(jwtUtil.getExpiration());
        // 设置用户信息
        response.setUser(user);

        // 返回登录响应
        return response;
    }
}

package com.sports.auth.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.sports.auth.service.SmsService;
import com.sports.common.constant.CommonConstant;
import com.sports.common.constant.MessageConstant;
import com.sports.common.constant.RedisKeyConstant;
import com.sports.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 短信验证码服务实现类
 */
@Slf4j
@Service
public class SmsServiceImpl implements SmsService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 1. 发送验证码
     * 支持登录验证码和注册验证码两种类型
     *
     * 1.1 验证手机号格式（必须是11位数字）
     *
     * 1.2 检查是否频繁发送
     *     - 从Redis中查询该手机号是否已有未过期的验证码
     *     - 如果有，抛出"验证码发送过于频繁"异常
     *
     * 1.3 生成随机验证码
     *     - 使用RandomUtil生成6位数字验证码
     *
     * 1.4 存储验证码到Redis
     *     - Key格式：sms:code:{type}:{phone}
     *     - Value：验证码
     *     - 过期时间：5分钟
     *
     * 1.5 日志记录
     *     - 记录发送的手机号和验证码（实际生产环境应调用短信服务商API）
     *
     * @param phone 手机号
     * @param type  验证码类型（login:登录, register:注册）
     * @return 是否发送成功
     */
    @Override
    public Boolean sendCode(String phone, String type) {
        if (phone == null || phone.length() != CommonConstant.PHONE_LENGTH) {
            throw new BusinessException(MessageConstant.PHONE_FORMAT_ERROR);
        }

        String key = RedisKeyConstant.getSmsCodeKey(type, phone);

        String existingCode = stringRedisTemplate.opsForValue().get(key);
        if (existingCode != null) {
            throw new BusinessException(MessageConstant.CODE_SEND_FREQUENTLY);
        }

        String code = RandomUtil.randomNumbers(CommonConstant.VERIFICATION_CODE_LENGTH);

        stringRedisTemplate.opsForValue().set(key, code, CommonConstant.VERIFICATION_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        log.info("向手机号 {} 发送验证码: {}", phone, code);

        return true;
    }

    /**
     * 2. 验证验证码
     * 用于登录和注册时验证用户输入的验证码是否正确
     *
     * 2.1 验证手机号格式（必须是11位数字）
     *
     * 2.2 验证验证码格式（必须是6位数字）
     *
     * 2.3 从Redis中查询存储的验证码
     *     - Key格式：sms:code:{type}:{phone}
     *
     * 2.4 检查验证码是否存在
     *     - 如果不存在，说明验证码已过期或不存在，抛出"验证码已过期"异常
     *
     * 2.5 比对验证码
     *     - 如果不匹配，抛出"验证码错误"异常
     *
     * 2.6 删除验证码
     *     - 验证通过后，从Redis中删除该验证码，防止重复使用
     *
     * @param phone 手机号
     * @param code  用户输入的验证码
     * @param type  验证码类型（login:登录, register:注册）
     * @return 是否验证通过
     */
    @Override
    public Boolean validateCode(String phone, String code, String type) {
        if (phone == null || phone.length() != CommonConstant.PHONE_LENGTH) {
            throw new BusinessException(MessageConstant.PHONE_FORMAT_ERROR);
        }

        if (code == null || code.length() != CommonConstant.VERIFICATION_CODE_LENGTH) {
            throw new BusinessException(MessageConstant.CODE_FORMAT_ERROR);
        }

        String key = RedisKeyConstant.getSmsCodeKey(type, phone);
        String storedCode = stringRedisTemplate.opsForValue().get(key);

        if (storedCode == null) {
            throw new BusinessException(MessageConstant.CODE_EXPIRED);
        }

        if (!storedCode.equals(code)) {
            throw new BusinessException(MessageConstant.CODE_ERROR);
        }

        stringRedisTemplate.delete(key);

        return true;
    }
}

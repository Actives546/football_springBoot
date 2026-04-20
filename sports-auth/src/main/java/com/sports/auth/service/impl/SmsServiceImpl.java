package com.sports.auth.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.sports.auth.service.SmsService;
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

    private static final String CODE_KEY_PREFIX = "sms:code:";
    private static final long CODE_EXPIRE_TIME = 5;

    @Override
    public Boolean sendCode(String phone, String type) {
        if (phone == null || phone.length() != 11) {
            throw new BusinessException("手机号格式不正确");
        }

        String key = CODE_KEY_PREFIX + type + ":" + phone;

        String existingCode = stringRedisTemplate.opsForValue().get(key);
        if (existingCode != null) {
            throw new BusinessException("验证码发送过于频繁，请稍后再试");
        }

        String code = RandomUtil.randomNumbers(6);

        stringRedisTemplate.opsForValue().set(key, code, CODE_EXPIRE_TIME, TimeUnit.MINUTES);

        log.info("向手机号 {} 发送验证码: {}", phone, code);

        return true;
    }

    @Override
    public Boolean validateCode(String phone, String code, String type) {
        if (phone == null || phone.length() != 11) {
            throw new BusinessException("手机号格式不正确");
        }

        if (code == null || code.length() != 6) {
            throw new BusinessException("验证码格式不正确");
        }

        String key = CODE_KEY_PREFIX + type + ":" + phone;
        String storedCode = stringRedisTemplate.opsForValue().get(key);

        if (storedCode == null) {
            throw new BusinessException("验证码已过期或不存在");
        }

        if (!storedCode.equals(code)) {
            throw new BusinessException("验证码错误");
        }

        stringRedisTemplate.delete(key);

        return true;
    }
}

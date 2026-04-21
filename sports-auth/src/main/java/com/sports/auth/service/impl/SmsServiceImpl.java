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

@Slf4j
@Service
public class SmsServiceImpl implements SmsService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

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

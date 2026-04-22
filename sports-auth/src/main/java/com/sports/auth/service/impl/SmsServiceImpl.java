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
 * 实现发送验证码和验证验证码的核心业务逻辑
 */
@Slf4j
@Service
public class SmsServiceImpl implements SmsService {

    // Redis操作模板，用于存储和读取验证码
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 1. 发送验证码
     * 支持登录验证码和注册验证码两种类型
     * 流程：验证手机号格式 -> 检查是否频繁发送 -> 生成验证码 -> 存储到Redis -> 日志记录
     *
     * @param phone 手机号，必须是11位数字
     * @param type  验证码类型（login-登录，register-注册）
     * @return 是否发送成功
     */
    @Override
    public Boolean sendCode(String phone, String type) {
        // 1.1 验证手机号格式
        // 判断手机号是否为空或长度不等于11位
        if (phone == null || phone.length() != CommonConstant.PHONE_LENGTH) {
            // 如果格式不正确，抛出业务异常
            throw new BusinessException(MessageConstant.PHONE_FORMAT_ERROR);
        }

        // 1.2 构建Redis Key
        // Key格式：sms:code:{type}:{phone}
        // 例如：sms:code:login:13800138000 或 sms:code:register:13800138000
        String key = RedisKeyConstant.getSmsCodeKey(type, phone);

        // 1.3 检查是否频繁发送
        // 从Redis中查询该手机号是否已有未过期的验证码
        String existingCode = stringRedisTemplate.opsForValue().get(key);
        // 如果Redis中存在该验证码，说明在有效期内已发送过
        if (existingCode != null) {
            // 抛出业务异常，提示验证码发送过于频繁
            throw new BusinessException(MessageConstant.CODE_SEND_FREQUENTLY);
        }

        // 1.4 生成随机验证码
        // 使用Hutool工具类生成6位数字验证码
        String code = RandomUtil.randomNumbers(CommonConstant.VERIFICATION_CODE_LENGTH);

        // 1.5 将验证码存入Redis
        // 设置过期时间为5分钟（300秒）
        stringRedisTemplate.opsForValue().set(
            key,                                    // Redis Key
            code,                                   // Redis Value：验证码
            CommonConstant.VERIFICATION_CODE_EXPIRE_MINUTES,  // 过期时间：5分钟
            TimeUnit.MINUTES                        // 时间单位：分钟
        );

        // 1.6 日志记录验证码
        // 实际生产环境应该调用短信服务商API发送短信
        // 这里只是日志输出，方便开发调试
        log.info("向手机号 {} 发送验证码: {}", phone, code);

        // 返回发送成功
        return true;
    }

    /**
     * 2. 验证验证码
     * 用于登录和注册时验证用户输入的验证码是否正确
     * 流程：验证手机号格式 -> 验证验证码格式 -> 从Redis查询 -> 检查是否过期 -> 比对验证码 -> 删除验证码
     *
     * @param phone 手机号，必须是11位数字
     * @param code  用户输入的验证码，必须是6位数字
     * @param type  验证码类型（login-登录，register-注册）
     * @return 是否验证通过
     */
    @Override
    public Boolean validateCode(String phone, String code, String type) {
        // 2.1 验证手机号格式
        // 判断手机号是否为空或长度不等于11位
        if (phone == null || phone.length() != CommonConstant.PHONE_LENGTH) {
            // 如果格式不正确，抛出业务异常
            throw new BusinessException(MessageConstant.PHONE_FORMAT_ERROR);
        }

        // 2.2 验证验证码格式
        // 判断验证码是否为空或长度不等于6位
        if (code == null || code.length() != CommonConstant.VERIFICATION_CODE_LENGTH) {
            // 如果格式不正确，抛出业务异常
            throw new BusinessException(MessageConstant.CODE_FORMAT_ERROR);
        }

        // 2.3 构建Redis Key
        // Key格式：sms:code:{type}:{phone}
        String key = RedisKeyConstant.getSmsCodeKey(type, phone);
        // 2.4 从Redis中查询存储的验证码
        String storedCode = stringRedisTemplate.opsForValue().get(key);

        // 2.5 检查验证码是否存在
        // 如果Redis中不存在该验证码，说明验证码已过期或不存在
        if (storedCode == null) {
            // 抛出业务异常，提示验证码已过期
            throw new BusinessException(MessageConstant.CODE_EXPIRED);
        }

        // 2.6 比对验证码
        // 将用户输入的验证码与Redis中存储的验证码进行比对
        if (!storedCode.equals(code)) {
            // 如果不匹配，抛出业务异常，提示验证码错误
            throw new BusinessException(MessageConstant.CODE_ERROR);
        }

        // 2.7 删除验证码
        // 验证通过后，从Redis中删除该验证码，防止重复使用
        stringRedisTemplate.delete(key);

        // 返回验证成功
        return true;
    }
}

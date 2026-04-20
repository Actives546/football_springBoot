package com.sports.user.controller;

import com.sports.common.entity.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器
 * 用于验证服务连通性、网关路由、中间件连接
 */
@Api(tags = "测试接口")
@RestController
@RequestMapping("/test")
public class TestController {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 健康检查接口
     * 用于验证服务是否正常启动
     */
    @ApiOperation("健康检查")
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("service", "sports-user");
        data.put("status", "ok");
        data.put("time", LocalDateTime.now());
        return Result.success(data);
    }

    /**
     * 获取测试用户信息
     * 用于验证网关路由和服务调用
     */
    @ApiOperation("获取测试用户信息")
    @GetMapping("/user/{id}")
    public Result<Map<String, Object>> getUser(@ApiParam(value = "用户ID", example = "1") @PathVariable Long id) {
        Map<String, Object> user = new HashMap<>();
        user.put("id", id);
        user.put("username", "test_user_" + id);
        user.put("nickname", "测试用户" + id);
        user.put("email", "test" + id + "@sports.com");
        user.put("phone", "1380000" + String.format("%04d", id));
        user.put("status", 1);
        user.put("createTime", LocalDateTime.now());
        return Result.success(user);
    }

    /**
     * 测试Redis连接
     * 用于验证Redis中间件是否正常连接
     */
    @ApiOperation("测试Redis连接")
    @PostMapping("/redis/test")
    public Result<Map<String, Object>> testRedis(
            @ApiParam(value = "键名", example = "test_key") @RequestParam String key,
            @ApiParam(value = "值", example = "test_value") @RequestParam String value) {
        Map<String, Object> result = new HashMap<>();
        try {
            redisTemplate.opsForValue().set(key, value);
            Object getValue = redisTemplate.opsForValue().get(key);
            result.put("key", key);
            result.put("setValue", value);
            result.put("getValue", getValue);
            result.put("status", "redis连接成功");
            return Result.success(result);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("status", "redis连接失败");
            return Result.error("Redis测试失败: " + e.getMessage());
        }
    }

    /**
     * 服务信息接口
     * 返回服务的基本信息
     */
    @ApiOperation("获取服务信息")
    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("serviceName", "sports-user");
        info.put("port", 8081);
        info.put("description", "数字化体育赋能平台 - 用户微服务");
        info.put("version", "1.0.0");
        info.put("middlewares", new String[]{"MySQL", "Redis", "RabbitMQ", "Elasticsearch"});
        return Result.success(info);
    }
}

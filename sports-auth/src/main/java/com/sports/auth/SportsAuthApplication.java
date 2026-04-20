package com.sports.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 鉴权微服务启动类
 * 职责：提供登录、注册、验证码、JWT令牌管理等鉴权相关功能
 */
@SpringBootApplication(scanBasePackages = {"com.sports"})
@EnableDiscoveryClient  // 开启Nacos服务注册与发现
@EnableFeignClients(basePackages = {"com.sports"})  // 开启Feign远程调用
public class SportsAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(SportsAuthApplication.class, args);
    }
}

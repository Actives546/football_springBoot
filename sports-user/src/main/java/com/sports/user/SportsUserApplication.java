package com.sports.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 用户微服务启动类
 * 职责：唯一业务微服务，集成所有中间件，提供测试接口
 */
@SpringBootApplication(scanBasePackages = {"com.sports"})
@EnableDiscoveryClient  // 开启Nacos服务注册与发现
@EnableFeignClients(basePackages = {"com.sports"})  // 开启Feign远程调用
@MapperScan("com.sports.user.mapper")  // 扫描Mapper接口
public class SportsUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(SportsUserApplication.class, args);
    }
}

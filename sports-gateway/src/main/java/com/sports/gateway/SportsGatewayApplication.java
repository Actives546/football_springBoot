package com.sports.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 网关服务启动类
 * 职责：微服务统一入口、服务注册、路由转发
 */
@SpringBootApplication
@EnableDiscoveryClient  // 开启Nacos服务注册与发现
public class SportsGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(SportsGatewayApplication.class, args);
    }
}

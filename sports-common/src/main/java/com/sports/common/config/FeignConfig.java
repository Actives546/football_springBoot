package com.sports.common.config;

import feign.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenFeign 配置类
 * 配置远程调用的日志级别等
 */
@Configuration
public class FeignConfig {

    /**
     * 配置Feign日志级别：FULL（记录完整日志）
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}

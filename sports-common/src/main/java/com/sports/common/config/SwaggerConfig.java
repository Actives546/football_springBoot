package com.sports.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.oas.annotations.EnableOpenApi;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * Swagger 接口文档配置类
 * 基于 Knife4j 增强 Swagger 文档功能
 * 只在 Web MVC 环境生效（Gateway 使用 WebFlux，不支持 Springfox）
 */
@Configuration
@EnableOpenApi
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SwaggerConfig {

    /**
     * 配置 Swagger 文档
     */
    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.sports"))
                .paths(PathSelectors.any())
                .build();
    }

    /**
     * API 文档基本信息
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("数字化体育赋能平台接口文档")
                .description("微服务架构数字化体育赋能平台 - RESTful API 接口文档")
                .contact(new Contact("Sports Team", "", ""))
                .version("1.0.0")
                .build();
    }
}

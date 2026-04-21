package com.sports.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "gateway.auth")
public class GatewayAuthProperties {

    private List<String> whiteList = new ArrayList<>();

    private String secret;

    private String header = "Authorization";

    private String prefix = "Bearer";
}

package com.sports.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sports.common.constant.HttpStatusConstant;
import com.sports.common.constant.MessageConstant;
import com.sports.common.entity.Result;
import com.sports.common.util.JwtUtil;
import com.sports.gateway.config.GatewayAuthProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Autowired
    private GatewayAuthProperties gatewayAuthProperties;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getPath().value();

        log.info("网关拦截请求: {} {}", request.getMethod(), path);

        if (isWhiteList(path)) {
            log.info("请求路径在白名单中，直接放行: {}", path);
            return chain.filter(exchange);
        }

        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            log.warn("请求缺少令牌: {}", path);
            return writeUnauthorizedResponse(response, MessageConstant.TOKEN_MISSING);
        }

        try {
            String secret = gatewayAuthProperties.getSecret();
            if (secret == null || secret.isEmpty()) {
                log.error("JWT密钥未配置");
                return writeUnauthorizedResponse(response, MessageConstant.SYSTEM_ERROR);
            }

            Long userId = JwtUtil.getUserIdFromTokenStatic(token, secret);
            String username = JwtUtil.getUsernameFromTokenStatic(token, secret);

            log.info("令牌验证通过, userId: {}, username: {}", userId, username);

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", String.valueOf(userId))
                    .header("X-Username", username)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (Exception e) {
            log.warn("令牌验证失败: {}", e.getMessage());
            return writeUnauthorizedResponse(response, MessageConstant.TOKEN_INVALID);
        }
    }

    private boolean isWhiteList(String path) {
        for (String pattern : gatewayAuthProperties.getWhiteList()) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private String extractToken(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        String authorization = headers.getFirst(gatewayAuthProperties.getHeader());

        if (StringUtils.hasText(authorization)) {
            String prefix = gatewayAuthProperties.getPrefix();
            if (authorization.startsWith(prefix + " ")) {
                return authorization.substring(prefix.length() + 1);
            }
        }

        return authorization;
    }

    private Mono<Void> writeUnauthorizedResponse(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Result<Void> result = Result.error(HttpStatusConstant.UNAUTHORIZED, message);

        try {
            String json = objectMapper.writeValueAsString(result);
            DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("序列化响应失败", e);
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }
}

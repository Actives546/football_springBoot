package com.sports.gateway.filter;

import com.sports.common.exception.GatewayException;
import com.sports.common.util.JwtUtil;
import com.sports.gateway.config.GatewayAuthProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Autowired
    private GatewayAuthProperties gatewayAuthProperties;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final List<String> LOCALHOST_IPS = Arrays.asList(
            "127.0.0.1",
            "0:0:0:0:0:0:0:1",
            "localhost"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        log.info("网关拦截请求: {} {}", request.getMethod(), path);

        checkIpAllowed(request);

        if (isWhiteList(path)) {
            log.info("请求路径在白名单中，直接放行: {}", path);
            return chain.filter(exchange);
        }

        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            log.warn("请求缺少令牌: {}", path);
            throw GatewayException.tokenMissing();
        }

        String secret = gatewayAuthProperties.getSecret();
        if (secret == null || secret.isEmpty()) {
            log.error("JWT密钥未配置");
            throw GatewayException.secretNotConfigured();
        }

        Long userId = JwtUtil.getUserIdFromTokenStatic(token, secret);
        String username = JwtUtil.getUsernameFromTokenStatic(token, secret);

        log.info("令牌验证通过, userId: {}, username: {}", userId, username);

        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", String.valueOf(userId))
                .header("X-Username", username)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private void checkIpAllowed(ServerHttpRequest request) {
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress == null) {
            log.warn("无法获取客户端IP地址");
            throw GatewayException.ipNotAllowed();
        }

        String clientIp = remoteAddress.getAddress().getHostAddress();
        String hostName = remoteAddress.getHostName();

        log.info("客户端IP: {}, HostName: {}", clientIp, hostName);

        boolean isLocalhost = LOCALHOST_IPS.stream()
                .anyMatch(ip -> ip.equalsIgnoreCase(clientIp) || ip.equalsIgnoreCase(hostName));

        if (!isLocalhost) {
            log.warn("非本地IP访问被拒绝: {}", clientIp);
            throw GatewayException.ipNotAllowed();
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

    @Override
    public int getOrder() {
        return -100;
    }
}

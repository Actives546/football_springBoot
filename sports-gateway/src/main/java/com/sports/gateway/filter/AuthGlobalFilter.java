package com.sports.gateway.filter;

import com.sports.common.exception.GatewayException;
import com.sports.common.util.JwtUtil;
import com.sports.gateway.config.GatewayAuthProperties;
import com.sports.gateway.util.GatewayWhiteListUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * 网关认证全局过滤器
 * 职责：统一处理网关的认证授权逻辑，包括IP校验、白名单校验、JWT令牌校验等
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    /**
     * 网关认证配置属性
     * 包含白名单、JWT密钥、请求头名称、令牌前缀等配置
     */
    @Autowired
    private GatewayAuthProperties gatewayAuthProperties;

    /**
     * 1. 过滤器核心方法
     * 处理所有经过网关的请求，执行认证授权逻辑
     *
     * @param exchange 服务器Web交换对象，包含请求和响应信息
     * @param chain    过滤器链，用于将请求传递给下一个过滤器
     * @return Mono<Void> 响应式返回值
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        log.info("网关请求: {} {}", request.getMethod(), path);

        return checkIpAllowed(request)
                .then(Mono.defer(() -> {
                    if (GatewayWhiteListUtil.isWhiteList(path, gatewayAuthProperties.getWhiteList())) {
                        log.debug("白名单路径放行: {}", path);
                        return chain.filter(exchange);
                    }

                    String token = extractToken(request);
                    if (!StringUtils.hasText(token)) {
                        log.warn("请求缺少令牌: {}", path);
                        return Mono.error(GatewayException.tokenMissing());
                    }

                    String secret = gatewayAuthProperties.getSecret();
                    if (!StringUtils.hasText(secret)) {
                        log.error("JWT密钥未配置");
                        return Mono.error(GatewayException.secretNotConfigured());
                    }

                    Long userId;
                    String username;
                    try {
                        userId = JwtUtil.getUserIdFromTokenStatic(token, secret);
                        username = JwtUtil.getUsernameFromTokenStatic(token, secret);
                    } catch (Exception e) {
                        log.warn("令牌验证失败: {}, 错误: {}", path, e.getMessage());
                        return Mono.error(GatewayException.tokenInvalid());
                    }

                    log.debug("令牌验证通过, userId: {}, path: {}", userId, path);

                    ServerHttpRequest mutatedRequest = request.mutate()
                            .header("X-User-Id", String.valueOf(userId))
                            .header("X-Username", username)
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                }));
    }

    /**
     * 2. IP地址校验方法
     * 检查请求的IP地址是否为本地IP，只有本地IP才能访问网关
     *
     * @param request 服务器HTTP请求对象
     * @return Mono<Void> 响应式返回值
     */
    private Mono<Void> checkIpAllowed(ServerHttpRequest request) {
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress == null) {
            log.warn("无法获取客户端IP地址");
            return Mono.error(GatewayException.ipNotAllowed());
        }

        String clientIp = remoteAddress.getAddress() != null ? remoteAddress.getAddress().getHostAddress() : null;
        String hostName = remoteAddress.getHostName();

        boolean isLocalhost = GatewayWhiteListUtil.isLocalhost(clientIp, hostName);
        if (!isLocalhost) {
            log.warn("非本地IP访问被拒绝: {}", clientIp);
            return Mono.error(GatewayException.ipNotAllowed());
        }

        return Mono.empty();
    }

    /**
     * 3. 从请求中提取JWT令牌方法
     * 从Authorization请求头中提取Bearer令牌
     *
     * @param request 服务器HTTP请求对象
     * @return 提取的令牌字符串，如果没有则返回null
     */
    private String extractToken(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();

        String headerName = gatewayAuthProperties.getHeader();
        if (!StringUtils.hasText(headerName)) {
            log.warn("令牌请求头名称未配置");
            return null;
        }
        String authorization = headers.getFirst(headerName);

        if (!StringUtils.hasText(authorization)) {
            return null;
        }

        String prefix = gatewayAuthProperties.getPrefix();
        if (!StringUtils.hasText(prefix)) {
            log.warn("令牌前缀未配置");
            return authorization;
        }

        if (authorization.startsWith(prefix + " ")) {
            return authorization.substring(prefix.length() + 1);
        }

        return authorization;
    }

    /**
     * 4. 获取过滤器优先级方法
     * 优先级值越小，优先级越高
     *
     * @return 过滤器优先级值
     */
    @Override
    public int getOrder() {
        return -100;
    }
}

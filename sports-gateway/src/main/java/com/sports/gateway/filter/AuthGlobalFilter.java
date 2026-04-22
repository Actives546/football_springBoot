package com.sports.gateway.filter;

import com.sports.common.exception.GatewayException;
import com.sports.common.util.JwtUtil;
import com.sports.gateway.config.GatewayAuthProperties;
import com.sports.gateway.util.GatewayWhiteListUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
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
        // 1.1 获取请求对象和请求路径
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 1.2 打印请求日志（仅打印关键信息）
        log.info("网关请求: {} {}", request.getMethod(), path);

        // 2. IP地址校验
        checkIpAllowed(request);

        // 3. 白名单路径校验
        if (GatewayWhiteListUtil.isWhiteList(path, gatewayAuthProperties.getWhiteList())) {
            log.debug("白名单路径放行: {}", path);
            return chain.filter(exchange);
        }

        // 4. JWT令牌校验
        // 4.1 从请求中提取令牌
        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            log.warn("请求缺少令牌: {}", path);
            throw GatewayException.tokenMissing();
        }

        // 4.2 校验JWT密钥配置
        String secret = gatewayAuthProperties.getSecret();
        if (!StringUtils.hasText(secret)) {
            log.error("JWT密钥未配置");
            throw GatewayException.secretNotConfigured();
        }

        // 4.3 解析并验证JWT令牌
        Long userId;
        String username;
        try {
            userId = JwtUtil.getUserIdFromTokenStatic(token, secret);
            username = JwtUtil.getUsernameFromTokenStatic(token, secret);
        } catch (ExpiredJwtException e) {
            // 4.3.1 令牌已过期
            log.warn("令牌已过期: {}", path);
            throw GatewayException.tokenExpired();
        } catch (JwtException e) {
            // 4.3.2 令牌无效（签名错误、格式错误等）
            log.warn("令牌无效: {}, 错误: {}", path, e.getMessage());
            throw GatewayException.tokenInvalid();
        }

        // 4.4 打印令牌验证通过日志（仅打印关键信息）
        log.debug("令牌验证通过, userId: {}, path: {}", userId, path);

        // 5. 将用户信息添加到请求头，传递给下游服务
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", String.valueOf(userId))
                .header("X-Username", username)
                .build();

        // 6. 继续执行过滤器链
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * 2. IP地址校验方法
     * 检查请求的IP地址是否为本地IP，只有本地IP才能访问网关
     *
     * @param request 服务器HTTP请求对象
     */
    private void checkIpAllowed(ServerHttpRequest request) {
        // 2.1 获取客户端远程地址
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress == null) {
            log.warn("无法获取客户端IP地址");
            throw GatewayException.ipNotAllowed();
        }

        // 2.2 获取客户端IP和主机名
        String clientIp = remoteAddress.getAddress() != null ? remoteAddress.getAddress().getHostAddress() : null;
        String hostName = remoteAddress.getHostName();

        // 2.3 检查是否为本地IP
        boolean isLocalhost = GatewayWhiteListUtil.isLocalhost(clientIp, hostName);
        if (!isLocalhost) {
            log.warn("非本地IP访问被拒绝: {}", clientIp);
            throw GatewayException.ipNotAllowed();
        }
    }

    /**
     * 3. 从请求中提取JWT令牌方法
     * 从Authorization请求头中提取Bearer令牌
     *
     * @param request 服务器HTTP请求对象
     * @return 提取的令牌字符串，如果没有则返回null
     */
    private String extractToken(ServerHttpRequest request) {
        // 3.1 获取请求头
        HttpHeaders headers = request.getHeaders();

        // 3.2 获取Authorization请求头的值
        String headerName = gatewayAuthProperties.getHeader();
        if (!StringUtils.hasText(headerName)) {
            log.warn("令牌请求头名称未配置");
            return null;
        }
        String authorization = headers.getFirst(headerName);

        // 3.3 检查Authorization是否为空
        if (!StringUtils.hasText(authorization)) {
            return null;
        }

        // 3.4 获取令牌前缀
        String prefix = gatewayAuthProperties.getPrefix();
        if (!StringUtils.hasText(prefix)) {
            log.warn("令牌前缀未配置");
            return authorization;
        }

        // 3.5 检查是否以指定前缀开头，并提取令牌
        if (authorization.startsWith(prefix + " ")) {
            return authorization.substring(prefix.length() + 1);
        }

        // 3.6 如果没有前缀，直接返回Authorization值
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

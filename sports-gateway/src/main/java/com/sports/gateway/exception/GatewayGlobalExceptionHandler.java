package com.sports.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sports.common.constant.HttpStatusConstant;
import com.sports.common.entity.Result;
import com.sports.common.exception.GatewayException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 网关全局异常处理器
 * 职责：统一处理网关中发生的所有异常，返回标准的错误响应结构
 * 优先级设为-1，确保在默认异常处理器之前执行
 */
@Slf4j
@Component
@Order(-1)
public class GatewayGlobalExceptionHandler implements ErrorWebExceptionHandler {

    /**
     * JSON序列化工具
     * 用于将Result对象序列化为JSON字符串返回给客户端
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 1. 异常处理核心方法
     * 处理所有经过网关的异常，统一返回标准的错误响应
     *
     * @param exchange 服务器Web交换对象，包含请求和响应信息
     * @param ex       发生的异常对象
     * @return Mono<Void> 响应式返回值
     */
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        // 1.1 获取响应对象
        ServerHttpResponse response = exchange.getResponse();

        // 1.2 检查响应是否已提交，如果已提交则直接抛出异常
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        // 1.3 设置响应内容类型为JSON
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 2. 定义响应结果和HTTP状态
        Result<Void> result;
        HttpStatus httpStatus;

        // 3. 根据异常类型进行不同的处理
        if (ex instanceof GatewayException) {
            // 3.1 网关业务异常处理
            // 这是网关自定义的业务异常，包含错误码和错误消息
            GatewayException gatewayException = (GatewayException) ex;
            result = Result.error(gatewayException.getCode(), gatewayException.getMessage());
            httpStatus = HttpStatus.resolve(gatewayException.getCode());
            if (httpStatus == null) {
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            }
            log.warn("网关业务异常: code={}, message={}", gatewayException.getCode(), gatewayException.getMessage());

        } else if (ex instanceof ExpiredJwtException) {
            // 3.2 JWT令牌过期异常处理
            // 当JWT令牌过期时会抛出此异常
            result = Result.error(HttpStatusConstant.UNAUTHORIZED, "令牌已过期");
            httpStatus = HttpStatus.UNAUTHORIZED;
            log.warn("JWT令牌已过期: {}", ex.getMessage());

        } else if (ex instanceof JwtException) {
            // 3.3 其他JWT异常处理
            // 包括令牌无效、签名错误、格式错误等
            result = Result.error(HttpStatusConstant.UNAUTHORIZED, "令牌无效");
            httpStatus = HttpStatus.UNAUTHORIZED;
            log.warn("JWT令牌异常: {}", ex.getMessage());

        } else if (ex instanceof ResponseStatusException) {
            // 3.4 响应状态异常处理
            // Spring WebFlux框架抛出的响应状态异常
            ResponseStatusException responseStatusException = (ResponseStatusException) ex;
            result = Result.error(responseStatusException.getStatus().value(), responseStatusException.getReason());
            httpStatus = responseStatusException.getStatus();
            log.warn("网关响应状态异常: status={}, message={}", httpStatus, responseStatusException.getReason());

        } else {
            // 3.5 其他未知异常处理
            // 所有未明确处理的异常都归类为系统异常
            result = Result.error(HttpStatusConstant.INTERNAL_SERVER_ERROR, "系统异常");
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            log.error("网关系统异常", ex);
        }

        // 4. 设置HTTP响应状态码
        response.setStatusCode(httpStatus);

        // 5. 将Result对象序列化为JSON并写入响应
        try {
            // 5.1 将Result对象序列化为JSON字符串
            String json = objectMapper.writeValueAsString(result);
            // 5.2 将JSON字符串包装为DataBuffer
            DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
            // 5.3 将DataBuffer写入响应
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            // 5.4 序列化失败时的处理
            log.error("序列化响应失败", e);
            return response.setComplete();
        }
    }
}

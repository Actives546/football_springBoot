package com.sports.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sports.common.constant.HttpStatusConstant;
import com.sports.common.entity.Result;
import com.sports.common.exception.GatewayException;
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
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            log.error("响应已提交，无法处理异常: {}", ex.getMessage(), ex);
            return response.setComplete();
        }

        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Result<Void> result;
        HttpStatus httpStatus;

        if (ex instanceof GatewayException) {
            GatewayException gatewayException = (GatewayException) ex;
            result = Result.error(gatewayException.getCode(), gatewayException.getMessage());
            httpStatus = HttpStatus.resolve(gatewayException.getCode());
            if (httpStatus == null) {
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            }
            log.warn("网关业务异常: code={}, message={}", gatewayException.getCode(), gatewayException.getMessage());

        } else if (ex instanceof ResponseStatusException) {
            ResponseStatusException responseStatusException = (ResponseStatusException) ex;
            result = Result.error(responseStatusException.getStatus().value(), responseStatusException.getReason());
            httpStatus = responseStatusException.getStatus();
            log.warn("网关响应状态异常: status={}, message={}", httpStatus, responseStatusException.getReason());

        } else {
            result = Result.error(HttpStatusConstant.INTERNAL_SERVER_ERROR, "系统异常");
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            log.error("网关系统异常", ex);
        }

        response.setStatusCode(httpStatus);

        try {
            String json = objectMapper.writeValueAsString(result);
            DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("序列化响应失败", e);
            return response.setComplete();
        }
    }
}

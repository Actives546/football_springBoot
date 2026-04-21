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

@Slf4j
@Component
@Order(-1)
public class GatewayGlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
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

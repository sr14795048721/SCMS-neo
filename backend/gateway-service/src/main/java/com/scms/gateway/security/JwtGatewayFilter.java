package com.scms.gateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scms.gateway.common.api.ApiResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {

    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String API_V1_PREFIX = "/api/v1/";
    private static final String IOT_API_V1_PREFIX = "/api/iot/v1/";

    private final SecretKey secretKey;
    private final ObjectMapper objectMapper;

    public JwtGatewayFilter(@Value("${scms.jwt.secret}") String secret, ObjectMapper objectMapper) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.objectMapper = objectMapper;
    }

    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }
        HttpMethod method = exchange.getRequest().getMethod();
        String path = exchange.getRequest().getPath().value();
        if (PublicGatewayPathMatcher.matches(method, path)) {
            return chain.filter(exchange);
        }
        if (!requiresAuthentication(path)) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String token = extractBearerToken(authorization);
        if (token == null) {
            return unauthorized(exchange, "missing bearer token");
        }

        try {
            Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
            if (!TOKEN_TYPE_ACCESS.equals(String.valueOf(claims.get("typ")))) {
                return unauthorized(exchange, "invalid token type");
            }
            ServerHttpRequest mutated = withForwardedIdentity(exchange.getRequest(), claims);
            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (JwtException | IllegalArgumentException exception) {
            return unauthorized(exchange, "invalid token");
        }
    }

    private ServerHttpRequest withForwardedIdentity(ServerHttpRequest request, Claims claims) {
        HttpHeaders forwardedHeaders = new HttpHeaders();
        forwardedHeaders.putAll(request.getHeaders());
        forwardedHeaders.set("X-User-Id", String.valueOf(claims.get("uid")));
        forwardedHeaders.set("X-User-Role", String.valueOf(claims.get("role")));
        forwardedHeaders.set("X-User-Name", String.valueOf(claims.getSubject()));

        return new ServerHttpRequestDecorator(request) {
            @Override
            public HttpHeaders getHeaders() {
                return forwardedHeaders;
            }
        };
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring(7);
    }

    private boolean requiresAuthentication(String path) {
        return path.startsWith(API_V1_PREFIX) || path.startsWith(IOT_API_V1_PREFIX);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        String requestId = resolveRequestId(exchange);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set(REQUEST_ID_HEADER, requestId);
        ApiResponse<Void> body = ApiResponse.error("UNAUTHORIZED", message, requestId);
        byte[] bytes = toJsonBytes(body, requestId);
        DataBuffer dataBuffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(dataBuffer));
    }

    private byte[] toJsonBytes(ApiResponse<Void> body, String requestId) {
        try {
            return objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException exception) {
            String fallback = "{\"code\":\"UNAUTHORIZED\",\"message\":\"invalid token\",\"data\":null,\"requestId\":\""
                    + requestId + "\",\"timestamp\":\"" + Instant.now() + "\"}";
            return fallback.getBytes(StandardCharsets.UTF_8);
        }
    }

    private String resolveRequestId(ServerWebExchange exchange) {
        String requestId = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        return requestId;
    }
}

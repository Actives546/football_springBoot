package com.sports.common.util;

import com.sports.common.constant.CommonConstant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret:" + CommonConstant.JWT_DEFAULT_SECRET + "}")
    private String secret;

    @Value("${jwt.expiration:" + CommonConstant.JWT_DEFAULT_EXPIRATION + "}")
    private Long expiration;

    @Value("${jwt.header:" + CommonConstant.JWT_DEFAULT_HEADER + "}")
    private String header;

    @Value("${jwt.prefix:" + CommonConstant.JWT_DEFAULT_PREFIX + "}")
    private String prefix;

    private Key key;

    private static JwtUtil instance;

    @PostConstruct
    public void init() {
        log.info("JWT配置 - secret长度: {}, expiration: {}", secret.length(), expiration);
        this.key = buildKey(secret);
        instance = this;
        log.info("JWT工具类初始化完成");
    }

    private static Key buildKey(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < CommonConstant.JWT_SECRET_MIN_LENGTH) {
            log.warn("JWT密钥长度不足32字节，自动填充...");
            StringBuilder sb = new StringBuilder(secret);
            while (sb.length() < CommonConstant.JWT_SECRET_MIN_LENGTH) {
                sb.append(secret);
            }
            keyBytes = sb.substring(0, CommonConstant.JWT_SECRET_MIN_LENGTH).getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        return createToken(claims, username, expiration);
    }

    public String refreshToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Long userId = claims.get("userId", Long.class);
        String username = claims.getSubject();
        return generateToken(userId, username);
    }

    private String createToken(Map<String, Object> claims, String subject, Long expirationTime) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("userId", Long.class);
    }

    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Boolean validateToken(String token, String username) {
        String tokenUsername = getUsernameFromToken(token);
        return (tokenUsername.equals(username) && !isTokenExpired(token));
    }

    private Boolean isTokenExpired(String token) {
        Date expirationDate = getExpirationDateFromToken(token);
        return expirationDate.before(new Date());
    }

    public Date getExpirationDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration();
    }

    public String getHeader() {
        return header;
    }

    public String getPrefix() {
        return prefix;
    }

    public Long getExpiration() {
        return expiration;
    }

    public String extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith(prefix + " ")) {
            return authorizationHeader.substring(prefix.length() + 1);
        }
        return authorizationHeader;
    }

    public static String generateTokenStatic(Long userId, String username, String secret, Long expirationTime) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        return createTokenStatic(claims, username, secret, expirationTime);
    }

    public static String refreshTokenStatic(String token, String secret, Long expirationTime) {
        Claims claims = getClaimsFromTokenStatic(token, secret);
        Long userId = claims.get("userId", Long.class);
        String username = claims.getSubject();
        return generateTokenStatic(userId, username, secret, expirationTime);
    }

    private static String createTokenStatic(Map<String, Object> claims, String subject, String secret, Long expirationTime) {
        Key staticKey = buildKey(secret);
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(staticKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public static Long getUserIdFromTokenStatic(String token, String secret) {
        Claims claims = getClaimsFromTokenStatic(token, secret);
        return claims.get("userId", Long.class);
    }

    public static String getUsernameFromTokenStatic(String token, String secret) {
        Claims claims = getClaimsFromTokenStatic(token, secret);
        return claims.getSubject();
    }

    public static Date getExpirationDateFromTokenStatic(String token, String secret) {
        Claims claims = getClaimsFromTokenStatic(token, secret);
        return claims.getExpiration();
    }

    private static Claims getClaimsFromTokenStatic(String token, String secret) {
        Key staticKey = buildKey(secret);
        return Jwts.parserBuilder()
                .setSigningKey(staticKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public static Boolean validateTokenStatic(String token, String username, String secret) {
        String tokenUsername = getUsernameFromTokenStatic(token, secret);
        return (tokenUsername.equals(username) && !isTokenExpiredStatic(token, secret));
    }

    private static Boolean isTokenExpiredStatic(String token, String secret) {
        Date expirationDate = getExpirationDateFromTokenStatic(token, secret);
        return expirationDate.before(new Date());
    }

    public static JwtUtil getInstance() {
        return instance;
    }
}

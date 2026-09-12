package com.exam.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具。
 *
 * <p>用 HS256 对称签名。注意 jjwt 0.11 起对密钥长度做了强制校验：
 * HS256 要求密钥至少 32 字节，密钥太短会直接抛 WeakKeyException
 * （这是常见坑，所以 {@code jwt.secret} 一定要够长）。</p>
 */
@Slf4j
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMillis;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expirationMillis) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "jwt.secret 长度不足 32 字节，HS256 无法使用。请配置更长的密钥。");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMillis = expirationMillis;
    }

    /**
     * 签发 token。
     *
     * @param userId   用户 ID
     * @param username 登录名
     * @param role     角色，见 {@link com.exam.common.enums.Role}
     */
    public String generateToken(Long userId, String username, Integer role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析 token。
     *
     * @return 解析出的载荷；token 非法或已过期时返回 {@code null}
     */
    public Claims parse(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            // 过期、签名错误、格式错误都走这里：属于正常的鉴权失败，不用打错误日志
            log.debug("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }

    public long getExpirationMillis() {
        return expirationMillis;
    }
}

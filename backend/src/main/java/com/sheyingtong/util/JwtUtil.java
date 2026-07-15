package com.sheyingtong.util;

import com.sheyingtong.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT 工具类：签发 / 解析 / 校验 Token
 * ----------------------------------------------------------------
 * 算法：HMAC-SHA512 (HS512)，对称加密密钥
 * 结构：Base64(Header).Base64(Payload).Base64(Signature)
 * Payload 标准 claims：
 *   - sub (subject)   : 用户名（唯一身份标识）
 *   - iat (issuedAt)  : 签发时间
 *   - exp (expiration): 过期时间 = 签发时间 + JwtConfig.expiration(ms)
 * 密钥来源：JwtConfig.secret，来自 application.yml ${JWT_SECRET:...}
 */
@Slf4j
@Component
public class JwtUtil {
    
    private final JwtConfig jwtConfig;
    private final SecretKey secretKey;
    
    /**
     * 构造器：注入 JwtConfig 并预生成 SecretKey（避免每次签/验都重新创建）
     * 容错处理：如果配置的 secret 为空 / 长度不足（<32字节=256bits），自动生成 512bits 安全随机密钥
     *           避免 WeakKeyException 导致整个服务启动失败（本地开发友好）
     * @param jwtConfig JWT 参数配置（secret 字符串 + expiration 毫秒数）
     */
    public JwtUtil(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        String rawSecret = jwtConfig.getSecret();
        byte[] keyBytes;
        if (rawSecret == null || rawSecret.isBlank() || rawSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            // RFC7518 §3.2：HS256/HS384/HS512 要求密钥 >= 对应哈希输出长度（256/384/512 bits）
            // 这里直接生成 512 bits = 64 字节的安全随机密钥，适配 HS512
            log.warn("【JWT】jwt.secret 未配置或长度不足（<32字节），自动生成一次性随机密钥。" +
                    "注意：服务重启后已签发的 Token 将全部失效！生产环境请通过 JWT_SECRET 环境变量配置固定密钥。");
            SecureRandom sr = new SecureRandom();
            byte[] randomBytes = new byte[64];
            sr.nextBytes(randomBytes);
            keyBytes = randomBytes;
            log.info("【JWT】自动生成的随机密钥 Base64 摘要: {}",
                    Base64.getEncoder().withoutPadding().encodeToString(keyBytes).substring(0, 12) + "...");
        } else {
            keyBytes = rawSecret.getBytes(StandardCharsets.UTF_8);
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * 从 Token 中提取用户名（subject 字段）
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    /**
     * 从 Token 中提取过期时间（exp 字段），用于过期判断
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    /**
     * 通用 claims 提取器：传入 Function<Claims, T> 从 payload 中自由取值
     * 方便未来扩展：userId、email、角色等自定义 claims
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * 解析完整 JWT → 校验签名 → 返回 payload Claims 对象
     * 签名失败 / Token 格式错 / Token 过期都会抛 SignatureException / MalformedJwtException / ExpiredJwtException
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    /**
     * 判断 Token 是否已过期：exp 字段 < 当前时间
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    /**
     * 对外暴露的签发方法：默认不含自定义 claims
     * 登录成功后由 AuthService 调用，返回给前端保存在 localStorage
     */
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username);
    }
    
    /**
     * 真正签发 Token：Builder 模式拼装 Header/Payload/Signature
     * @param claims  自定义 claims（可扩展 userId/role/avatar 等）
     * @param subject subject 字段 = 用户名
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                // 过期时间 = 当前 + JwtConfig.expiration（单位毫秒，默认 24h）
                .expiration(new Date(System.currentTimeMillis() + jwtConfig.getExpiration()))
                .signWith(secretKey)
                .compact();
    }
    
    /**
     * 校验 Token 合法性：username 一致 + 未过期
     * 注：签名校验在 extractUsername / isTokenExpired 内部 parseSignedClaims 会自动完成
     */
    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }
}

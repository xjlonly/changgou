package com.changgou.base64;


import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.io.SerialException;
import io.jsonwebtoken.security.Keys;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import javax.security.auth.kerberos.EncryptionKey;
import java.beans.Encoder;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {

    Logger logger = LoggerFactory.getLogger(getClass());
    private static final long TOKEN_EXPIRED_SECOND = 60;
    private static final  String SECRET = "xQlccJmYfno5IyW/Kfs/M3i0Zk8bpsFLPojOuppMMMY=";
    @Test
    public void jwtTest(){
        SecretKey secretKey = getSecretKey();
        JwtBuilder jwtBuilder = Jwts.builder()
                .setId("12334")
                .setSubject("sin dindddd")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + TOKEN_EXPIRED_SECOND * 30))
                .signWith(secretKey);
        Map<String, Object> map = new  HashMap<String, Object>();
        map.put("name","xjlonly");
        map.put("address","beijingshi");
        map.put("password", "334ddgsd");
        jwtBuilder.addClaims(map);
        String jwsStr = jwtBuilder.compact();
        logger.info("jwsStr:{}", jwsStr);

    }


    @Test
    public void verify() {
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiIxMjMzNCIsInN1YiI6InNpbiBkaW5kZGRkIiwiaWF0IjoxNjA2MzY5OTYzLCJleHAiOjE2MDYzNjk5NjUsInBhc3N3b3JkIjoiMzM0ZGRnc2QiLCJhZGRyZXNzIjoiYmVpamluZ3NoaSIsIm5hbWUiOiJ4amxvbmx5In0.uiEA73zr0TQcNPeFmJfRaaxJZmoBk7FDwKFmMMMDUvg";
        SecretKey secretKey = getSecretKey();
        Jws<Claims> jws = Jwts.parserBuilder()
                // 解析 JWT 的服务器与创建 JWT 的服务器的时钟不一定完全同步，此设置允许两台服务器最多有 3 分钟的时差
                .setAllowedClockSkewSeconds(180L)
                .setSigningKey(secretKey)
                // 默认情况下 JJWT 只能解析 String, Date, Long, Integer, Short and Byte 类型，如果需要解析其他类型则需要配置 JacksonDeserializer
                //.deserializeJsonWith(new JacksonDeserializer(Maps.of(USER_INFO_KEY, UserInfo.class).build()))
                .build().parseClaimsJws(token);

        Claims claims = jws.getBody();

        logger.info("content:{}", claims.toString());
    }

    @Test
    public void getKey(){
        String keyString = Encoders.BASE64.encode(Keys.secretKeyFor(SignatureAlgorithm.HS256).getEncoded());
        logger.info("key:{}", keyString);
    }

    /**
     * SecretKey 根据 SECRET 的编码方式解码后得到：
     * Base64 编码：SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretString));
     * Base64URL 编码：SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secretString));
     * 未编码：SecretKey key = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
     * */
    private SecretKey getSecretKey() {
        byte[] encodeKey = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(encodeKey);
    }
}

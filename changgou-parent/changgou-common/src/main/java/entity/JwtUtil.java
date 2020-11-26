package entity;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Date;

public class JwtUtil {
    //有效期为
    public static final Long JWT_TTL = 3600000L;// 60 * 60 *1000  一个小时

    //Jwt令牌信息 采用随机生成的密钥 已经过base64位编码
    public static final String JWT_KEY = "xQlccJmYfno5IyW/Kfs/M3i0Zk8bpsFLPojOuppMMMY=";

    public static String createJWT(String id, String subject, Long ttlMillis) {
        //指定算法
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

        //当前系统时间
        long nowMillis = System.currentTimeMillis();
        //令牌签发时间
        Date now = new Date(nowMillis);

        //如果令牌有效期为null，则默认设置有效期1小时
        if(ttlMillis==null){
            ttlMillis=JwtUtil.JWT_TTL;
        }

        //令牌过期时间设置
        long expMillis = nowMillis + ttlMillis;
        Date expDate = new Date(expMillis);

        //生成秘钥
        SecretKey secretKey = getSecretKey();

        //封装Jwt令牌信息
        JwtBuilder builder = Jwts.builder()
                .setId(id)                    //唯一的ID
                .setSubject(subject)          // 主题  可以是JSON数据
                .setIssuer("admin")          // 签发者
                .setIssuedAt(now)             // 签发时间
                .signWith(secretKey) // 签名算法以及密匙
                .setExpiration(expDate);      // 设置过期时间
        return builder.compact();
    }

    /**
     * 生成加密 secretKey
     * @return
     */
    public static SecretKey generalKey() {
        byte[] encodedKey = Base64.getEncoder().encode(JwtUtil.JWT_KEY.getBytes());
        SecretKey key = new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");
        return key;
    }

    /**
     * SecretKey 根据 SECRET 的编码方式解码后得到：
     * Base64 编码：SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretString));
     * Base64URL 编码：SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secretString));
     * 未编码：SecretKey key = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
     * */
    private static SecretKey getSecretKey() {
        byte[] encodeKey = Decoders.BASE64.decode(JWT_KEY);
        return Keys.hmacShaKeyFor(encodeKey);
    }

    public static String getKey(){
       return  Encoders.BASE64.encode(Keys.secretKeyFor(SignatureAlgorithm.HS256).getEncoded());
    }

    /**
     * 解析令牌数据
     * @param token
     * @return
     * @throws Exception
     */
    public static Claims parseJWT(String token) throws Exception {
        SecretKey secretKey = getSecretKey();
        return Jwts.parserBuilder()
                // 解析 JWT 的服务器与创建 JWT 的服务器的时钟不一定完全同步，此设置允许两台服务器最多有 3 分钟的时差
                .setAllowedClockSkewSeconds(180L)
                .setSigningKey(secretKey)
                // 默认情况下 JJWT 只能解析 String, Date, Long, Integer, Short and Byte 类型，如果需要解析其他类型则需要配置 JacksonDeserializer
                //.deserializeJsonWith(new JacksonDeserializer(Maps.of(USER_INFO_KEY, UserInfo.class).build()))
                .build().parseClaimsJws(token).getBody();
    }
}

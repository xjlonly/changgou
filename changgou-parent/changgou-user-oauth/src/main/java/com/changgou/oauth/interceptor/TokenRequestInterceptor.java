package com.changgou.oauth.interceptor;

import com.alibaba.fastjson.JSON;
import com.changgou.oauth.util.AdminToken;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaSigner;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.util.HashMap;
import java.util.Map;

@Component
public class TokenRequestInterceptor implements RequestInterceptor {
    //生成令牌时校验用户信息需要调用user微服务 需要权限认证
    //feign加载之前进行发放令牌认证
    //header带上认证令牌
    @Override
    public void apply(RequestTemplate requestTemplate) {
        String token =  AdminToken.adminToken();
        requestTemplate.header("Authorization", "bearer " + token);
    }
}

package com.changgou.mq.interceptor;


import com.changgou.mq.util.AdminToken;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

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

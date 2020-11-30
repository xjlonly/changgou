package com.changgou.filter;

import com.changgou.util.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthorizeFilter implements GlobalFilter, Ordered {

    //令牌头名字
    private static final String AUTHORIZE_TOKEN = "Authorization";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        //获取请求的URI
        String path = request.getURI().getPath();

        if(URLFilter.hasAuthorize(path)){
            //放行
            Mono<Void> filter = chain.filter(exchange);
            return filter;
        }
//        //如果是登录、goods等开放的微服务[这里的goods部分开放],则直接放行
//        if (path.startsWith("/api/user/login") || path.startsWith("/api/goods/brand/search")) {
//            //放行
//            Mono<Void> filter = chain.filter(exchange);
//            return filter;
//        }
        boolean hasToken = true;
        String token = request.getHeaders().getFirst(AUTHORIZE_TOKEN);

        if(StringUtils.isEmpty(token)){
            hasToken = false;
            token = request.getQueryParams().getFirst(AUTHORIZE_TOKEN);
        }
        if(StringUtils.isEmpty(token)){
            hasToken = false;
            HttpCookie cookie = request.getCookies().getFirst(AUTHORIZE_TOKEN);
            if(cookie != null){
                token = cookie.getValue();
            }
        }
        if(StringUtils.isEmpty(token)){
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
//        try {
//            JwtUtil.parseJWT(token);
//        } catch (Exception e) {
//            e.printStackTrace();
//            response.setStatusCode(HttpStatus.UNAUTHORIZED);
//            return response.setComplete();
//        }

        if(StringUtils.isEmpty(token)){
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        if(!hasToken){

            if(!token.startsWith("bearer") && !token.startsWith("Bearer ")) token = "bearer " + token;
            //将令牌封装到请求头中 Auth2.0使用
            request.mutate().header(AUTHORIZE_TOKEN, token);
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}

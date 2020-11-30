package com.changgou.order.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.RequestContext;

import java.util.Enumeration;

//改由bean配置
//@Component
//public class TokenRequestInterceptor implements RequestInterceptor {
//
//    private Logger logger = LoggerFactory.getLogger(getClass());
//    //feign加载之前进行发放令牌认证
//    //header带上认证令牌
//    @Override
//    public void apply(RequestTemplate requestTemplate) {
//        //获取用户令牌
//
//        //记录了当前用户请求的所有数据，包含请求头和请求参数
//        //用户当前请求的时候对应线程的数据 如果开启了熔断，熔断默认隔离措施为线程池隔离，
//        // 此时会开启新的线程用于feign调用，需在配置文件里设置熔断配置为信号量隔离
//        ServletRequestAttributes requestAttributes =
//                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
//        if(requestAttributes != null){
//            Enumeration<String> headerNames = requestAttributes.getRequest().getHeaderNames();
//            while (headerNames.hasMoreElements()){
//                //请求头key
//                String headerKey = headerNames.nextElement();
//                String headerValue = requestAttributes.getRequest().getHeader(headerKey);
//                logger.info("{}:{}",headerKey,headerValue);
//                //将请求头封装到feign调用头文件中
//                requestTemplate.header(headerKey, headerValue);
//            }
//        }
//
//    }
//}

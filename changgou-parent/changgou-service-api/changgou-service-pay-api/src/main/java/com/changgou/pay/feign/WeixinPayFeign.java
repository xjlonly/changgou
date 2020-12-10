package com.changgou.pay.feign;

import entity.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

/**
 * @author ：xjlonly
 * @date ：Created in 2020/12/10 16:58
 * @description：feign
 * @modified By：
 * @version: $
 */
@FeignClient(name = "pay")
@RequestMapping("/weixin/pay")
public interface WeixinPayFeign {

    @GetMapping(value = "/status/query")
    Result<Map<String,String>>  queryStatus(String outtradeno);
}

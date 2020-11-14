package com.changgou.goods.controller;

import com.changgou.content.feign.ContentFeign;
import com.changgou.goods.feign1.DcClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DcController {
    @Autowired
    DcClient dcClient;

    @Autowired
    ContentFeign feign;

    @GetMapping("/feign/consumer")
    public String getDcFeign(){
        //通过feign进行负载均衡消费 由于Feign是基于Ribbon实现的，所以它自带了客户端负载均衡功能
        return  dcClient.consumer();
    }



    @GetMapping("/feign/consumer1")
    public String getDcFeign1(){
        //通过feign进行负载均衡消费 由于Feign是基于Ribbon实现的，所以它自带了客户端负载均衡功能
        var t =  feign.findByCategory(1);
        return t.getCode().toString();
    }
}


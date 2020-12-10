package com.changgou.order.schedul;

import com.changgou.order.pojo.Order;
import com.changgou.order.service.OrderService;
import com.changgou.pay.feign.WeixinPayFeign;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;
import org.springframework.stereotype.Component;

import java.io.PipedReader;
import java.util.Map;
import java.util.Objects;

/**
 * @author ：xjlonly
 * @date ：Created in 2020/12/10 16:45
 * @description：定时任务服务类
 * @modified By：
 * @version: 1.0$
 */
@Component
public class TaskService {

    @Autowired
    private OrderService orderService;

    @Autowired
    private WeixinPayFeign weixinPayFeign;

    @Autowired
    private RedisTemplate redisTemplate;
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Scheduled(initialDelay = 10_000,fixedRate = 50_000)
    public void OrderQueryStatus(){
        logger.info("读取redis内订单信息...");
        var result =  redisTemplate.boundHashOps("Order");
        result.keys().forEach(id->{
            var resultMap =  weixinPayFeign.queryStatus(id.toString());
            //获取订单支付状态 更新订单信息
            logger.info(resultMap.getData().toString());
        });
    }
}

package com.changgou.seckill.mq;

import com.alibaba.fastjson.JSON;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RabbitListener
public class SeckillMessageListener {
    /**
     * 监听消费消息
     * @param message
     */
    @RabbitHandler
    public void consumeMessage(@Payload String message){
        System.out.println(message);
        //将消息转换成Map对象
        Map<String,String> resultMap = JSON.parseObject(message,Map.class);
        System.out.println("监听到的消息:"+resultMap);
    }
}

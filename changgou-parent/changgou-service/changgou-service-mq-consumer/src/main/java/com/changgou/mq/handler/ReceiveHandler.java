package com.changgou.mq.handler;

import com.alibaba.fastjson.JSON;
import com.changgou.content.feign.ContentFeign;
import com.changgou.item.feign.PageFeign;
import com.changgou.mq.config.RabbitmqConfig;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
public class ReceiveHandler {

    private final ContentFeign contentFeign;
    private final PageFeign pageFeign;
    private final StringRedisTemplate stringRedisTemplate;
    @Autowired
    public ReceiveHandler( ContentFeign contentFeign, PageFeign pageFeign,
                           StringRedisTemplate stringRedisTemplate){
        this.contentFeign = contentFeign;
        this.pageFeign = pageFeign;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());
    @RabbitListener(queues = RabbitmqConfig.QUEUE_INFO_CONTENT)
    public void receiver_content(Object obj, Message message, Channel channel){
        logger.info("receive content msg : " + message.toString());
        String categoryId = new String(message.getBody(), StandardCharsets.UTF_8);
        var result = contentFeign.findByCategory(Long.parseLong(categoryId));
        var contents = result.getData();
        stringRedisTemplate.boundValueOps("content_" + categoryId).set(JSON.toJSONString(contents));
    }

    @RabbitListener(queues = RabbitmqConfig.QUEUE_INFO_HTML)
    public void receiver_spu(Object obj, Message message, Channel channel){
        logger.info("receive spu msg : " + message.toString());
        String json = new String(message.getBody());
        SpuData spuData = JSON.parseObject(json,SpuData.class);
        if(!spuData.delete) pageFeign.createHtml(spuData.spuId);
    }


    public class SpuData {
        private  long spuId;
        private  boolean delete;
        public SpuData(long spuId, boolean delete){
            this.spuId = spuId;
            this.delete = delete;
        }

    }
}

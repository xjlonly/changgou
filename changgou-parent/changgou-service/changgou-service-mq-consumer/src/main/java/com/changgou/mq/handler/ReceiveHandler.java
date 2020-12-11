package com.changgou.mq.handler;

import com.alibaba.fastjson.JSON;
import com.changgou.content.feign.ContentFeign;
import com.changgou.item.feign.PageFeign;
import com.changgou.mq.config.RabbitmqConfig;
import com.changgou.order.feign.OrderFeign;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

@Component
public class ReceiveHandler {

    private final ContentFeign contentFeign;
    private final PageFeign pageFeign;
    private final StringRedisTemplate stringRedisTemplate;
    private final OrderFeign orderFeign;

    @Autowired
    public ReceiveHandler(ContentFeign contentFeign, PageFeign pageFeign, OrderFeign orderFeign,
                           StringRedisTemplate stringRedisTemplate){
        this.contentFeign = contentFeign;
        this.pageFeign = pageFeign;
        this.orderFeign = orderFeign;
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

    @RabbitListener(queues = {"${mq.pay.queue.order}"})
    public void receiver_order(Object obj, Message message, Channel channel){
        String msg = new String(message.getBody());
        //将数据转成Map
        Map<String,String> result = JSON.parseObject(msg, Map.class);

        //return_code=SUCCESS
        String return_code = result.get("return_code");
        //业务结果
        String result_code = result.get("result_code");

        //业务结果 result_code=SUCCESS/FAIL，修改订单状态
        if(return_code.equalsIgnoreCase("success") ) {
            //获取订单号
            String outtradeno = result.get("out_trade_no");
            //业务结果
            if (result_code.equalsIgnoreCase("success")) {
                if (outtradeno != null) {
                    //修改订单状态  out_trade_no
                    if(!StringUtils.isEmpty(result.get("transaction_id"))){
                        orderFeign.updateStatus(outtradeno,result.get("transaction_id"));
                    }
                }
            } else {
                //订单删除
            }
        }else{
            logger.error("订单支付失败：{}",result.get("out_trade_no"));
        }
    }


    @RabbitListener(queues = {"orderListenerQueue"})
    public void receiver_order_delay(Object obj, Message message, Channel channel){
        String msg = new String(message.getBody());

        logger.info("超时订单：{}",msg);
        //调用关闭订单接口
        //TODO
    }
}

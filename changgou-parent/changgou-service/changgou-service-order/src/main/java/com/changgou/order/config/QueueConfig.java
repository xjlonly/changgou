package com.changgou.order.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tk.mybatis.mapper.code.ORDER;


/**
 * @author ：xjlonly
 * @date ：Created in 2020/12/10 17:12
 * @description：延时队列配置类，用于处理超时订单
 * @modified By：
 * @version: 1.0$
 */
@Configuration
public class QueueConfig {

    public static  final String QUEUE_ORDER_DELAY ="orderDelayQueue";
    public static  final String QUEUE_ORDER_LISTENER ="orderListenerQueue";
    public static  final String EXCHANGE_ORDER_LISTENER = "orderListenerExchange";
    //创建延时队列 超时会将数据发送到其他队列
    @Bean
    public Queue orderDelayQueue(){
        return new Queue(QUEUE_ORDER_DELAY);
    }

    @Bean
    public Queue orderListenerQueue(){
        return QueueBuilder.durable(QUEUE_ORDER_LISTENER)
                .withArgument("x-dead-letter-exchange", EXCHANGE_ORDER_LISTENER)//死信队列数据绑定到其他交换机
                .withArgument("x-dead-letter-routing-key", QUEUE_ORDER_LISTENER)
                .build();
    }

    @Bean
    public Exchange orderListenerExchange(){
        return new DirectExchange(EXCHANGE_ORDER_LISTENER);
    }

    //延时队列与其他交换机绑定
    @Bean
    public Binding orderListenerBinding(Queue orderDelayQueue, Exchange orderListenerExchange){
        return BindingBuilder.bind(orderDelayQueue).to(orderListenerExchange).with(QUEUE_ORDER_LISTENER).noargs();
    }
}

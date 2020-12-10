package com.changgou.mq.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitmqConfig {
    public  static final String QUEUE_INFO_CONTENT = "queue_inform_content";
    public  static final String QUEUE_INFO_HTML = "queue_inform_html";
    public  static final String EXCHANGE_TOPICS_INFORM = "exchange_topics_inform";
    public static final String ROUTING_KEY_CONTENT="inform.#.content.#";
    public static final String ROUTING_KEY_HTML="inform.#.html.#";

    @Value("${mq.pay.exchange.order}")
    public  String  EXCHANGE_QUEUE_ORDER;
    @Value("${mq.pay.queue.order}")
    public  String QUEUE_PAY_ORDER;
    @Value("${mq.pay.routing.key}")
    public  String ROUTING_KEY_PAY;

    /*
    * 声明交换机
    * */
    @Bean(EXCHANGE_TOPICS_INFORM)
    public Exchange EXCHANGE_TOPICS_INFORM(){
        //durable(true) 持久化，mq重启之后交换机还在
        return ExchangeBuilder.topicExchange(EXCHANGE_TOPICS_INFORM).durable(true).build();
    }

    //声明QUEUE_INFORM_EMAIL队列
    @Bean(QUEUE_INFO_CONTENT)
    public Queue QUEUE_INFO_CONTENT(){
        return new Queue(QUEUE_INFO_CONTENT,true);
    }

    //将content队列绑定到交换机并指定routingkey
    @Bean
    public Binding binding_queue_inform_content(@Qualifier(QUEUE_INFO_CONTENT) Queue queue, @Qualifier(EXCHANGE_TOPICS_INFORM) Exchange exchange){
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY_CONTENT).noargs();
    }


    //声明QUEUE_INFORM_SMS队列
    @Bean(QUEUE_INFO_HTML)
    public Queue QUEUE_INFO_HTML(){
        return new Queue(QUEUE_INFO_HTML,true);
    }

    //将content队列绑定到交换机并指定routingkey
    @Bean
    public Binding binding_queue_inform_html(@Qualifier(QUEUE_INFO_HTML) Queue queue, @Qualifier(EXCHANGE_TOPICS_INFORM) Exchange exchange){
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY_HTML).noargs();
    }


    /*
     * 声明订单交换机
     * */
    @Bean("order_exchange")
    public Exchange EXCHANGE_TOPICS_ORDER(){
        //durable(true) 持久化，mq重启之后交换机还在
        return ExchangeBuilder.topicExchange(EXCHANGE_QUEUE_ORDER).durable(true).build();
    }


    //声明队列 订单消息队列
    @Bean("queue_order")
    public Queue QUEUE_PAY_ORDER(){
        return new Queue(QUEUE_PAY_ORDER,true);
    }

    //将content队列绑定到交换机并指定routingkey
    @Bean
    public Binding binding_queue_pay_order(@Qualifier("queue_order") Queue queue, @Qualifier("order_exchange") Exchange exchange){
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY_PAY).noargs();
    }

    @Bean
    public Queue QUEUE_ORDER_DELAY(){
        return  new Queue("orderListenerQueue");
    }

}

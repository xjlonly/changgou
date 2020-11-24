package com.changgou.mq.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitmqConfig {
    public  static final String QUEUE_INFO_CONTENT = "queue_inform_content";
    public  static final String QUEUE_INFO_HTML = "queue_inform_html";
    public  static final String EXCHANGE_TOPICS_INFORM = "exchange_topics_inform";
    public static final String ROUTING_KEY_CONTENT="inform.#.content.#";
    public static final String ROUTING_KEY_HTML="inform.#.html.#";

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
    //声明QUEUE_INFORM_SMS队列
    @Bean(QUEUE_INFO_HTML)
    public Queue QUEUE_INFO_HTML(){
        return new Queue(QUEUE_INFO_HTML,true);
    }
    //将content队列绑定到交换机并指定routingkey
    @Bean
    public Binding binding_queue_inform_content(@Qualifier(QUEUE_INFO_CONTENT) Queue queue, @Qualifier(EXCHANGE_TOPICS_INFORM) Exchange exchange){
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY_CONTENT).noargs();
    }

    //将content队列绑定到交换机并指定routingkey
    @Bean
    public Binding binding_queue_inform_html(@Qualifier(QUEUE_INFO_HTML) Queue queue, @Qualifier(EXCHANGE_TOPICS_INFORM) Exchange exchange){
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY_HTML).noargs();
    }

}

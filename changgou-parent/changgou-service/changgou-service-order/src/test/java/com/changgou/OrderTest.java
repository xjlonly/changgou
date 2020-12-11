package com.changgou;

import com.changgou.order.config.QueueConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author ：xjlonly
 * @date ：Created in 2020/12/11 16:58
 * @description：测试类 需保持与java包路径一致 否则需指定SpringBootTest(value="OrderApplication.class")
 * @modified By：
 * @version: $
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class OrderTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;


    @Test
    public void SendOrderToRabbit(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        rabbitTemplate.convertAndSend(QueueConfig.QUEUE_ORDER_DELAY, (Object) "12334444444444", new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                message.getMessageProperties().setExpiration("10000");
                return message;
            }
        });
    }
}

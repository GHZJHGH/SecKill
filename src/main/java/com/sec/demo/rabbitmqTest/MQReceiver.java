package com.sec.demo.rabbitmqTest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MQReceiver {

    @RabbitListener(queues = "queue_fanout01")
    public void receive(Object msg){
        log.info("Quene01接收消息：" + msg);
    }

    @RabbitListener(queues = "queue_fanout02")
    public void receive2(Object msg){
        log.info("Quene02接收消息：" + msg);
    }

    @RabbitListener(queues = "queue_direct01")
    public void receive3(Object msg){
        log.info("Quene01接收消息：" + msg);
    }

    @RabbitListener(queues = "queue_direct02")
    public void receive4(Object msg){
        log.info("Quene02接收消息：" + msg);
    }
}

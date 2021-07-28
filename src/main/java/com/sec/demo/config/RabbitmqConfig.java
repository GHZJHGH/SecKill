package com.sec.demo.config;

import org.apache.curator.shaded.com.google.common.collect.Maps;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Map;

@Configuration
public class RabbitmqConfig {
    @Autowired
    private Environment env;
    @Autowired
    private CachingConnectionFactory connectionFactory;
    @Autowired
    private SimpleRabbitListenerContainerFactoryConfigurer factoryConfigurer;

    private static final String QUEUE = "seckillQueue";
    private static final String EXCHANGE = "seckillExchange";

    //下单
    @Bean
    public Queue queue(){
        return new Queue(QUEUE);
    }
    @Bean
    public TopicExchange topicExchange(){
        return new TopicExchange(EXCHANGE);
    }
    @Bean
    public Binding binding(){
        return BindingBuilder.bind(queue()).to(topicExchange()).with("seckill.#");
    }


    //单一消费者
    @Bean(name = "singleListenerContainer")
    public SimpleRabbitListenerContainerFactory listenerContainer(){
        SimpleRabbitListenerContainerFactory factory=new SimpleRabbitListenerContainerFactory();
        //设置连接工厂
        factory.setConnectionFactory(connectionFactory);
        //设置数据交换格式
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        //消费者监听个数
        factory.setConcurrentConsumers(1);
        //消费端的监听最大个数
        factory.setMaxConcurrentConsumers(1);
        //设置每次发送给消费者的消息数
        factory.setPrefetchCount(1);
        factory.setTxSize(1);
        return factory;
    }

    //多个消费者
    @Bean(name = "multiListenerContainer")
    public SimpleRabbitListenerContainerFactory multiListennerContainer(){
        SimpleRabbitListenerContainerFactory factory=new SimpleRabbitListenerContainerFactory();
        factoryConfigurer.configure(factory,connectionFactory);
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        //确认消费模式
        factory.setAcknowledgeMode(AcknowledgeMode.NONE);
        factory.setConcurrentConsumers(env.getProperty("spring.rabbitmq.listener.simple.councurrency",int.class));
        factory.setMaxConcurrentConsumers(env.getProperty("spring.rabbitmq.listener.simple.max-concurrency",int.class));
        factory.setPrefetchCount(env.getProperty("spring.rabbitmq.listener.simple.prefetch",int.class));
        return factory;
    }

    //创建rabbit模板
    @Bean
    public RabbitTemplate rabbitTemplate(){
        connectionFactory.setPublisherConfirms(true);
        connectionFactory.setPublisherReturns(true);
        RabbitTemplate template=new RabbitTemplate(connectionFactory);
        template.setMandatory(true);
        template.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            @Override
            public void confirm(CorrelationData correlationData, boolean b, String s) {
                System.out.println("消息发送成功:correlationData("+correlationData+"),ack("+b+"),cause({"+s+"})");
            }
        });
        template.setReturnCallback(new RabbitTemplate.ReturnCallback() {
            @Override
            public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
                System.out.println("消息丢失：exchage("+exchange+"),route("+routingKey+"),replyCode("+replyCode+replyText+"),message("+message+")");
            }
        });
        return template;
    }

    //构建秒杀成功之后-订单超时未支付的死信队列消息模型
    //创建死信队列，死信队列中需要有两个参数x-dead-letter-exchange和x-dead-letter-routing-key
    @Bean
    public Queue successKillDeadQueue(){
        Map<String, Object> argsMap= Maps.newHashMap();
        argsMap.put("x-dead-letter-exchange",env.getProperty("mq.kill.item.success.kill.dead.exchange"));
        argsMap.put("x-dead-letter-routing-key",env.getProperty("mq.kill.item.success.kill.dead.routing.key"));
        return new Queue(env.getProperty("mq.kill.item.success.kill.dead.queue"),true,false,false,argsMap);
    }
    //基本交换机
    @Bean
    public TopicExchange successKillDeadProdExchange(){
        return new TopicExchange(env.getProperty("mq.kill.item.success.kill.dead.prod.exchange"),true,false);
    }
    //创建基本交换机+基本路由 -> 死信队列的绑定
    @Bean
    public Binding successKillDeadProdBinding(){
        return BindingBuilder.bind(successKillDeadQueue()).to(successKillDeadProdExchange()).with(env.getProperty("mq.kill.item.success.kill.dead.prod.routing.key"));
    }



    //真正的队列
    @Bean
    public Queue successKillRealQueue(){
        return new Queue(env.getProperty("mq.kill.item.success.kill.dead.real.queue"),true);
    }
    //死信交换机
    @Bean
    public TopicExchange successKillDeadExchange(){
        return new TopicExchange(env.getProperty("mq.kill.item.success.kill.dead.exchange"),true,false);
    }
    //死信交换机+死信路由->真正队列 的绑定
    @Bean
    public Binding successKillDeadBinding(){
        return BindingBuilder.bind(successKillRealQueue()).to(successKillDeadExchange()).with(env.getProperty("mq.kill.item.success.kill.dead.routing.key"));
    }


//    //测试fanout模式
//    @Bean
//    public Queue queue01(){
//        return new Queue("queue_fanout01");
//    }
//    @Bean
//    public Queue queue02(){
//        return new Queue("queue_fanout02");
//    }
//    @Bean
//    public FanoutExchange fanoutExchange(){
//        return new FanoutExchange("fanout_exchange");
//    }
//    @Bean
//    public Binding binding01(){
//        return BindingBuilder.bind(queue01()).to(fanoutExchange());
//    }
//    @Bean
//    public Binding binding02(){
//        return BindingBuilder.bind(queue02()).to(fanoutExchange());
//    }
//
//    //测试Direct模式
//    @Bean
//    public Queue queue_direct01(){
//        return new Queue("queue_direct01");
//    }
//    @Bean
//    public Queue queue_direct02(){
//        return new Queue("queue_direct02");
//    }
//    @Bean
//    public DirectExchange directExchange(){
//        return new DirectExchange("direct_exchange");
//    }
//
//    @Bean
//    public Binding binding_direct01(){
//        return BindingBuilder.bind(queue_direct01()).to(directExchange()).with("red");
//    }
//    @Bean
//    public Binding binding_direct02(){
//        return BindingBuilder.bind(queue_direct02()).to(directExchange()).with("green");
//    }
}

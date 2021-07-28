package com.sec.demo.rabbitmqTest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@org.springframework.stereotype.Controller
public class Controller {

    @Autowired
    MQSender mqSender;

    //测试fanout模式
    @RequestMapping("/mq/fanout")
    @ResponseBody
    public void mq(){
        mqSender.send("Hello");
    }

    //测试direct模式
    @RequestMapping("/mq/direct01")
    @ResponseBody
    public void mq02(){
        mqSender.send01("Hello,Red");
    }
    @RequestMapping("/mq/direct02")
    @ResponseBody
    public void mq03(){
        mqSender.send02("Hello,Green");
    }
}

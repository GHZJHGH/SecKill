package com.sec.demo.service;

import com.sec.demo.entity.ItemKillSuccess;
import com.sec.demo.mapper.ItemKillSuccessMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Configuration
@Component
public class SchedulerService {
    @Autowired
    private ItemKillSuccessMapper itemKillSuccessMapper;

    @Autowired
    private Environment env;

    //定时获取status=0的订单并判断是否超过了支付订单时间，然后进行失效
    @Scheduled(cron = "0/30 * * * * ?")//30秒触发一次
    public void schedulerExpireOrders() {
        List<ItemKillSuccess> list = itemKillSuccessMapper.selectExpireOrders();
        if(list!=null&&!list.isEmpty()){
            for (ItemKillSuccess item : list) {
                System.out.println(env.getProperty("scheduler.expire.orders.time"));
                if (item!=null && item.getStatus()==0 && item.getDiffTime()>env.getProperty("scheduler.expire.orders.time",Integer.class)){
                    itemKillSuccessMapper.expireOrder(item.getCode());
                }
            }
        }
    }
}

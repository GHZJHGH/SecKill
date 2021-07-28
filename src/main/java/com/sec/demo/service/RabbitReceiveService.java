package com.sec.demo.service;

import com.alibaba.druid.support.json.JSONUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.sec.demo.dto.GoodsForKill;
import com.sec.demo.dto.KillDto;
import com.sec.demo.dto.KillSuccessUserDto;
import com.sec.demo.entity.ItemKill;
import com.sec.demo.entity.ItemKillSuccess;
import com.sec.demo.mapper.ItemKillMapper;
import com.sec.demo.mapper.ItemKillSuccessMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

@Service
public class RabbitReceiveService {
    public static final Logger log= LoggerFactory.getLogger(RabbitSenderService.class);
    @Autowired
    private ItemKillSuccessMapper itemKillSuccessMapper;
    @Autowired
    private KillService killService;
    @Autowired
    private ItemKillMapper itemKillMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 下单操作
     * @param message
     */
    @RabbitListener(queues = "seckillQueue")
    public void receive(Message message) throws Exception {
        log.info("接收的消息" + message);
        String s = new String(message.getBody());
        s = s.replaceAll("^" + "\"" + "*|" + "\"" + "*$","");
        s = s.replaceAll("\\\\","");
        System.out.println(s);
        //s = "{\"killid\":2,\"userid\":2}";
        JSONObject jsonObject = JSON.parseObject(s);
        KillDto killDto =(KillDto)JSON.toJavaObject(jsonObject, KillDto.class);
        int killId = killDto.getKillid();
        int userId = killDto.getUserid();

        //库存
        ItemKill itemKill=itemKillMapper.selectByidV2(killId);
        if (itemKill==null&&itemKill.getCanKill()!=1 && itemKill.getTotal()<1){
            log.info("消费者-"+"库存不足");
            return;
        }
        //是否重复抢购
        Object o = redisTemplate.opsForValue().get(String.valueOf("重复"+userId + killId));
        if (o != null){
            log.info("消费者-"+"重复抢购");
            return;
        }

        killService.KillItemV3(itemKill,userId);
    }

    /**
     * 用户秒杀成功后超时未支付-监听者
     * @param info
     */
    @RabbitListener(queues = {"${mq.kill.item.success.kill.dead.real.queue}"},containerFactory = "singleListenerContainer")
    public void consumeExpireOrder(KillSuccessUserDto info){
        try {
            log.info("用户秒杀成功后超时未支付-监听者-接收消息:{}",info);

            if (info!=null){
                ItemKillSuccess entity=itemKillSuccessMapper.selectByPrimaryKey(info.getCode());
                if (entity!=null && entity.getStatus().intValue()==0){
                    itemKillSuccessMapper.expireOrder(info.getCode());
                    redisTemplate.delete(entity.getItemId()+entity.getUserId());
                }
            }
        }catch (Exception e){
            log.error("用户秒杀成功后超时未支付-监听者-发生异常：",e.fillInStackTrace());
        }
    }
}

package com.sec.demo.service.Impl;

import com.sec.demo.entity.ItemKill;
import com.sec.demo.entity.ItemKillSuccess;
import com.sec.demo.enums.SysConstant;
import com.sec.demo.mapper.ItemKillMapper;
import com.sec.demo.mapper.ItemKillSuccessMapper;
import com.sec.demo.service.KillService;
import com.sec.demo.service.RabbitSenderService;
import com.sec.demo.utils.SnowFlake;
import org.joda.time.DateTime;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class KillServiceImpl implements KillService {
    private SnowFlake snowFlake = new SnowFlake(2,3);
    @Autowired
    private ItemKillMapper itemKillMapper;
    @Autowired
    private ItemKillSuccessMapper itemKillSuccessMapper;
    @Autowired
    private RabbitSenderService rabbitSenderService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    @Override
    public Boolean killItem(Integer killId, Integer userId) throws Exception {
        Boolean result = false;

        if (itemKillSuccessMapper.countByKillUserId(killId,userId)<=0){
            ItemKill itemKill = itemKillMapper.selectByid(killId);
            if (itemKill!=null && itemKill.getCanKill()==1){
                int res = itemKillMapper.updateKillItem(killId);
                if (res>0){
                    commonRecordKillSuccessInfo(itemKill,userId);
                    result = true;
                }
            }
        }else {
            System.out.println("您已经抢购过该商品");
        }
        return result;
    }

    //mysql优化
    @Override
    public Boolean KillItemV2(Integer killId, Integer userId) throws Exception {
        Boolean result=false;
        //判断当前用户是否抢购过该商品
        if (itemKillSuccessMapper.countByKillUserId(killId,userId)<=0){
            //获取商品详情
            ItemKill itemKill=itemKillMapper.selectByidV2(killId);
            if (itemKill!=null&&itemKill.getCanKill()==1 && itemKill.getTotal()>0){
                int res=itemKillMapper.updateKillItemV2(killId);
                if (res>0){
                    commonRecordKillSuccessInfo(itemKill,userId);
                    result=true;
                }
            }
        }else {
            System.out.println("您已经抢购过该商品");
        }
        return result;
    }

    //redis分布式锁
    @Override
    public Boolean KillItemV3(Integer killId, Integer userId) throws Exception {

        //借助Redis的原子操作实现分布式锁
        ValueOperations valueOperations = stringRedisTemplate.opsForValue();
        //设置redis的key，key由killid和userid组成
        final String key = new StringBuffer().append(killId).append(userId).toString();
        //设置redis的value
        final String value = String.valueOf(snowFlake.nextId());
        //尝试获取锁
        Boolean result=valueOperations.setIfAbsent(key,value);

        if (result){
            stringRedisTemplate.expire(key,30, TimeUnit.SECONDS);
            //判断当前用户是否抢购过该商品
            if (itemKillSuccessMapper.countByKillUserId(killId,userId)<=0){
                //获取商品详情
                ItemKill itemKill=itemKillMapper.selectByidV2(killId);
                if (itemKill!=null&&itemKill.getCanKill()==1 && itemKill.getTotal()>0){
                    int res=itemKillMapper.updateKillItemV2(killId);
                    if (res>0){
                        commonRecordKillSuccessInfo(itemKill,userId);
                        return true;
                    }
                }
            }else {
                System.out.println("您已经抢购过该商品");
            }
            if (value.equals(valueOperations.get(key).toString())){
                stringRedisTemplate.delete(key);
            }
        }

        return false;
    }

    //redisson分布式锁
    @Override
    public Boolean KillItemV4(Integer killId, Integer userId) throws Exception {

        Boolean result=false;
        final String key=new StringBuffer().append(killId).append(userId).toString();
        RLock lock=redissonClient.getLock(key);
        //三个参数、等待时间、锁过期时间、时间单位
        Boolean cacheres=lock.tryLock(30,10,TimeUnit.SECONDS);

        if (cacheres){
            //判断当前用户是否抢购过该商品
            if (itemKillSuccessMapper.countByKillUserId(killId,userId)<=0){
                //获取商品详情
                ItemKill itemKill=itemKillMapper.selectByidV2(killId);
                if (itemKill!=null&&itemKill.getCanKill()==1 && itemKill.getTotal()>0){
                    int res=itemKillMapper.updateKillItemV2(killId);
                    if (res>0){
                        commonRecordKillSuccessInfo(itemKill,userId);
                        result = true;
                    }
                }
            }else {
                System.out.println("您已经抢购过该商品");
            }
            lock.unlock();
        }
        return result;
    }

    private void commonRecordKillSuccessInfo(ItemKill itemKill,Integer userId){
        ItemKillSuccess entity = new ItemKillSuccess();
        String orderNo=String.valueOf(snowFlake.nextId());
        entity.setCode(orderNo);
        entity.setItemId(itemKill.getItemId());
        entity.setKillId(itemKill.getId());
        entity.setUserId(userId.toString());
        entity.setStatus(SysConstant.OrderStatus.SuccessNotPayed.getCode().byteValue());
        entity.setCreateTime(DateTime.now().toDate());

        if (itemKillSuccessMapper.countByKillUserId(itemKill.getId(),userId) <= 0){
            int res=itemKillSuccessMapper.insertSelective(entity);
            if(res>0){
                //处理抢购成功后的流程
                //将订单送入死信队列
                rabbitSenderService.sendKillSuccessOrderExpireMsg(orderNo);
            }
        }
    }

}

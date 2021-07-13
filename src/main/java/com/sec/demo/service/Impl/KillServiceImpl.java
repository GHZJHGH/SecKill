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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class KillServiceImpl implements KillService {
    private SnowFlake snowFlake = new SnowFlake(2,3);
    @Autowired
    private ItemKillMapper itemKillMapper;
    @Autowired
    private ItemKillSuccessMapper itemKillSuccessMapper;
    @Autowired
    private RabbitSenderService rabbitSenderService;

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

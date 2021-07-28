package com.sec.demo.service.Impl;

import com.sec.demo.dto.GoodsForKill;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.util.List;
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
    private RedisTemplate redisTemplate;
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
    public Boolean KillItemV3(ItemKill itemKill, Integer userId) throws Exception {
        int Itemid = itemKill.getItemId();

        //更新数据库库存
        int res=itemKillMapper.updateKillItemV2(Itemid);
        if (res>0){
            //更新完后是否还有库存
            ItemKill i = itemKillMapper.selectByid(Itemid);
            if (i.getTotal()<1){
                redisTemplate.opsForValue().set("GoodsEmpty"+Itemid,"0");
            }

            redisTemplate.setEnableTransactionSupport(true);
            //判断是否已抢购
            if (itemKillSuccessMapper.countByKillUserId(Itemid,userId)<=0){
                //将订单插入数据库，并送入死信队列
                boolean result = commonRecordKillSuccessInfo(itemKill,userId);
                System.out.println(result);
                return result;
            }else {
                redisTemplate.opsForValue().set(String.valueOf("重复"+userId+Itemid),"1",60,TimeUnit.SECONDS);
                return false;
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

    @Override
    public List<GoodsForKill> searchKill() {
        return itemKillMapper.selectKill();
    }

    //获取秒杀结果
    @Override
    public int getResult(Integer userid, Integer killid) {
        int i = itemKillSuccessMapper.countByKillUserId(killid,userid);
        System.out.println(i+"and"+userid+":"+killid);
        if (i>0){
            return 0;
        }else if (redisTemplate.hasKey("GoodsEmpty"+killid)){
            return -1;
        }
        return 1;
    }

    @Override
    public String createPath(Object uid, int goodsId) {
//        //生成从ASCII 32到126组成的随机字符串 （包括符号）
//        String salt = RandomStringUtils.randomAscii(12);

        String str = DigestUtils.md5DigestAsHex((String.valueOf(goodsId)).getBytes());
        //System.out.println(str);
        redisTemplate.opsForValue().set("seckillPath:"+uid+":"+goodsId,str,60,TimeUnit.SECONDS);
        return str;
    }

    @Override
    public boolean checkPath(int userId, int killId, String path) {
        if (userId<0||killId<0|| StringUtils.isEmpty(path)){
            return false;
        }
        String redisPath = (String) redisTemplate.opsForValue().get("seckillPath:" + userId + ":" + killId);
        return path.equals(redisPath);
    }

    @Override
    public boolean checkCaptcha(Object uid, Integer goodsId, String captcha) {
        if (StringUtils.isEmpty(captcha)||uid==null||goodsId<0){
            return false;
        }
        //System.out.println(uid+":"+goodsId);
        String redisCaptcha = (String) redisTemplate.opsForValue().get("captcha:" + uid + ":" + goodsId);
        //System.out.println(captcha+"and"+redisCaptcha);
        return captcha.equals(redisCaptcha);
    }

    private boolean commonRecordKillSuccessInfo(ItemKill itemKill,Integer userId){
        ItemKillSuccess entity = new ItemKillSuccess();
        String orderNo=String.valueOf(snowFlake.nextId());
        entity.setCode(orderNo);
        entity.setItemId(itemKill.getItemId());
        entity.setKillId(itemKill.getId());
        entity.setUserId(userId.toString());
        entity.setStatus(SysConstant.OrderStatus.SuccessNotPayed.getCode());
        entity.setCreateTime(DateTime.now().toDate());

        //数据库添加订单
        int res=itemKillSuccessMapper.insertSelective(entity);
        if(res>0){
            //处理抢购成功后的流程
            //将订单送入死信队列
            rabbitSenderService.sendKillSuccessOrderExpireMsg(orderNo);
            return true;
        }
        return false;
    }

}

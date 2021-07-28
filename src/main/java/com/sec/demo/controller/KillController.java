package com.sec.demo.controller;
import com.alibaba.druid.support.json.JSONUtils;
import com.alibaba.fastjson.JSON;
import com.sec.demo.dto.GoodsForKill;
import com.sec.demo.dto.KillDto;
import com.sec.demo.enums.StatusCode;
import com.sec.demo.response.BaseResponse;
import com.sec.demo.service.KillService;
import com.sec.demo.service.RabbitSenderService;
import com.sec.demo.utils.SnowFlake;
import com.wf.captcha.ArithmeticCaptcha;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.util.SocketUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Controller
public class KillController implements InitializingBean {
    private static final String prefix = "kill";
    @Autowired
    private KillService killService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RabbitSenderService rabbitSenderService;

    private SnowFlake snowFlake = new SnowFlake(2,3);

    private Map<Integer,Boolean> EmptyStockMap = new HashMap<>();


    @RequestMapping(value = prefix+"/execute",method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public BaseResponse execute(@RequestBody @Validated KillDto killDto, BindingResult result, HttpSession httpSession){
        if (result.hasErrors()||killDto.getKillid()<0){
            return new BaseResponse(StatusCode.InvalidParam);
        }
        Object uid = httpSession.getAttribute("uid");
        if (uid == null){
            return new BaseResponse(StatusCode.UserNotLog);
        }
        Integer userid = (Integer) uid;
        try {
            Boolean res=killService.killItem(killDto.getKillid(),userid);
            if (!res){
                return new BaseResponse(StatusCode.Fail.getCode(),"商品已经抢购完或您已抢购过该商品");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        BaseResponse baseResponse=new BaseResponse(StatusCode.Success);
        return baseResponse;
    }
    //mysql优化版本
    @RequestMapping(value = prefix+"/test/execute",method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public BaseResponse testexecute(@RequestBody @Validated KillDto killDto, BindingResult result, HttpSession httpSession){
        if (result.hasErrors()||killDto.getKillid()<0){
            return new BaseResponse(StatusCode.InvalidParam);
        }
        try {
            Boolean res=killService.KillItemV2(killDto.getKillid(),killDto.getUserid());
            if (!res){
                return new BaseResponse(StatusCode.Fail.getCode(),"商品已经抢购完或您已抢购过该商品");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        BaseResponse baseResponse=new BaseResponse(StatusCode.Success);
        baseResponse.setData("抢购成功");
        return baseResponse;
    }

    /**
     * 优化前QPS：972
     * 优化后QPS：3341
     *
     * @param killDto
     * @param httpSession
     * @return
     */
    //redis分布式锁版本
    @RequestMapping(value = prefix+"/{path}/execute3",method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public BaseResponse testexecute3(@PathVariable String path, @RequestBody KillDto killDto, HttpSession httpSession){
        if (killDto.getKillid()<0){
            return new BaseResponse(StatusCode.InvalidParam);
        }
        Object uid=httpSession.getAttribute("uid");

        redisTemplate.setEnableTransactionSupport(true);
        ValueOperations valueOperations = redisTemplate.opsForValue();

        int killId = killDto.getKillid();
        //int userId = killDto.getUserid();
        int userId = (int) uid;

        boolean check = killService.checkPath(userId,killId,path);
        if (!check){
            return new BaseResponse(StatusCode.REQUEST_ILLEGAL);
        }

        //判断是否重复抢购
        //设置redis的key，key由killid和userid组成
        final String key = new StringBuffer().append(killId).append(userId).toString();
        //设置redis的value
        final String value = String.valueOf(snowFlake.nextId());
        //尝试获取锁
        Boolean r=valueOperations.setIfAbsent(key,value,60, TimeUnit.SECONDS);
        System.out.println(r);
        if (!r) {
            System.out.println("已抢购");
            return new BaseResponse(StatusCode.WAIT,"排队中");
        }

        //内存标记，减少Redis的访问
        if(EmptyStockMap.get(killId)){
            return new BaseResponse(StatusCode.Fail,"已卖完");
        }

        //预减库存
        Long stock = valueOperations.decrement("seckillGoods" + killId);
        if (stock < 0){
            valueOperations.increment("seckillGoods" + killId);
            EmptyStockMap.put(killId,true);
            return new BaseResponse(StatusCode.Fail,"已卖完");
        }

        KillDto killDto1 = new KillDto(killId,userId);
        rabbitSenderService.sendSeckillMessage(JSON.toJSONString(killDto1));
        return new BaseResponse(StatusCode.WAIT,"排队中");

    }

    @RequestMapping(value = "/result",method = RequestMethod.GET)
    @ResponseBody
    public BaseResponse getResult(Integer killId, HttpSession httpSession){
        if (httpSession.getAttribute("uid") == null){
            return new BaseResponse(StatusCode.UserNotLog);
        }

        int orderId = killService.getResult((Integer) httpSession.getAttribute("uid"),killId);
        StatusCode.Success.setCode(orderId);
        return new BaseResponse(StatusCode.Success);
    }

    @RequestMapping(value = "/path",method = RequestMethod.GET)
    @ResponseBody
    public BaseResponse getPath(Integer goodsId, String captcha, HttpSession httpSession, HttpServletRequest request){
        Object uid=httpSession.getAttribute("uid");
        if (uid==null){
            return new BaseResponse(StatusCode.UserNotLog);
        }
        ValueOperations valueOperations = redisTemplate.opsForValue();

        String uri = request.getRequestURI();
        //captcha = "0";
        Integer count = (Integer) valueOperations.get(uri + ":" + uid);
        if (count == null){
            valueOperations.set(uri + ":" + uid,1,10,TimeUnit.SECONDS);
            count = 1;
        }

        boolean check = killService.checkCaptcha(uid,goodsId,captcha);
        if (!check){
            return new BaseResponse(StatusCode.ERROR_CAPTCHA);
        }else if (count<5){
            valueOperations.increment(uri + ":" + uid);
        }else {
            return new BaseResponse(StatusCode.ACCESS_LIMIT_REAHCED);
        }

        String str = killService.createPath(uid,goodsId);
        return new BaseResponse(StatusCode.Success,str);
    }

    @RequestMapping(value = "/captcha",method = RequestMethod.GET)
    public void verifyCode(Integer itemId, HttpServletResponse response,HttpSession httpSession) throws Exception {
        if (itemId < 0){
            throw new Exception("请求非法");
        }
        Object uid=httpSession.getAttribute("uid");
        response.setContentType("image/jpg");
        response.setHeader("Pargam","No-cache");
        response.setHeader("Cache-Control","no-cache");
        response.setDateHeader("Expires",0);
        //生成验证码，存入Redis
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(130, 32, 3);
        redisTemplate.opsForValue().set("captcha:"+uid+":"+itemId,captcha.text(),300,TimeUnit.SECONDS);

        try {
            captcha.out(response.getOutputStream());
        }catch (IOException e){
            log.error("验证码生成失败",e.getMessage());
        }

    }



    /**
     *
     * @param killDto
     * @param result
     * @param httpSession
     * @return
     */

    //redisson分布式锁版本
    @RequestMapping(value = prefix+"/test/execute4",method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public BaseResponse testexecute4(@RequestBody @Validated KillDto killDto, BindingResult result, HttpSession httpSession){
        if (result.hasErrors()||killDto.getKillid()<0){
            return new BaseResponse(StatusCode.InvalidParam);
        }


        try {
            Boolean res=killService.KillItemV4(killDto.getKillid(),killDto.getUserid());
            if (!res){
                return new BaseResponse(StatusCode.Fail.getCode(),"商品已经抢购完或您已抢购过该商品");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new BaseResponse(StatusCode.Success,"抢购成功");
    }


    @RequestMapping(value = prefix+"/execute/success",method = RequestMethod.GET)
    public String killsuccess(){
        return "killsuccess";
    }
    @RequestMapping(value = prefix+"/execute/fail",method = RequestMethod.GET)
    public String killfail(){
        return "killfail";
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        List<GoodsForKill> list = killService.searchKill();
        if (CollectionUtils.isEmpty(list)){
            return;
        }
        list.forEach(GoodsForKill ->{
            redisTemplate.opsForValue().set("seckillGoods"+GoodsForKill.getItem_id(),GoodsForKill.getTotal());
            redisTemplate.delete("GoodsEmpty"+GoodsForKill.getItem_id());
            EmptyStockMap.put(GoodsForKill.getItem_id(),false);
        });
    }
}

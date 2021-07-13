package com.sec.demo.controller;

import com.sec.demo.dto.KillDto;
import com.sec.demo.enums.StatusCode;
import com.sec.demo.response.BaseResponse;
import com.sec.demo.service.KillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

@Controller
public class KillController {
    private static final String prefix = "kill";
    @Autowired
    private KillService killService;


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
    @RequestMapping(value = prefix+"/execute/success",method = RequestMethod.GET)
    public String killsuccess(){
        return "killsuccess";
    }
    @RequestMapping(value = prefix+"/execute/fail",method = RequestMethod.GET)
    public String killfail(){
        return "killfail";
    }
}

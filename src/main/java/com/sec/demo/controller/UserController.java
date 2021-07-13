package com.sec.demo.controller;

import com.sec.demo.entity.User;
import com.sec.demo.mapper.UserMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.jasper.security.SecurityUtil;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.DisabledAccountException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UserController {
    @Autowired
    private UserMapper userMapper;

    @RequestMapping(value = {"/login","unauth"},method = RequestMethod.GET)
    public String toLogin(){
        return "login";
    }

    @RequestMapping(value = "/login",method = RequestMethod.POST)
    public String login(@RequestParam("username")String username,@RequestParam("password")String password,Model model){
        System.out.println(username+password);

        //用于记录错误信息
        String msg = "";
        try {
            //未认证，生成一个UsernamePasswordToken对象，通过login登陆
            if (!SecurityUtils.getSubject().isAuthenticated()){
                UsernamePasswordToken token = new UsernamePasswordToken(username,password);
                SecurityUtils.getSubject().login(token);
            }
        }catch (UnknownAccountException e){
            msg=e.getMessage();
        }catch (DisabledAccountException e){
            msg=e.getMessage();
        }catch (IncorrectCredentialsException e){
            msg=e.getMessage();
        }catch (Exception e){
            msg="用户登陆异常";
            e.printStackTrace();
        }
        if (StringUtils.isBlank(msg)){
            return "redirect:/item";
        }else {
            model.addAttribute("errormsg",msg);
            return "login";
        }
    }

    @RequestMapping(value = "/register",method = RequestMethod.GET)
    public String toRegister(){
        return "register";
    }

    @RequestMapping(value = "/register",method = RequestMethod.POST)
    public String checkRegister(@RequestParam("username")String username,@RequestParam("password")String password,Model model){
        String msg="";
        User user = userMapper.selectByName(username);
        if (user!=null){
            msg="当前用户已存在";
            model.addAttribute("errormsg",msg);
            return "register";
        }
        else {
            int res=userMapper.insertUser(username,password);
            if (res>0){
                return "login";
            }else {
                msg="用户创建失败，请联系管理员处理";
                model.addAttribute("errormsg",msg);
                return "register";
            }
        }
    }

    @RequestMapping(value = "/logout",method = RequestMethod.GET)
    public String Logout(){
        SecurityUtils.getSubject().logout();
        return "login";
    }
}

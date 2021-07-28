package com.sec.demo.controller;

import com.sec.demo.entity.ItemKill;
import com.sec.demo.service.Impl.ItemServiceImpl;
import com.sec.demo.service.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Controller
public class ItemController {
    private static final Logger log = LoggerFactory.getLogger(ItemController.class);

    @Autowired
    private ItemServiceImpl itemServiceImpl;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ThymeleafViewResolver thymeleafViewResolver;

    /**
     * 添加缓存前：1571
     * 添加缓存后：3248
     * @param model
     * @return
     */
    //获取商品列表
    @RequestMapping(value = "/item",method = RequestMethod.GET,produces = "text/html;charset=utf-8")
    @ResponseBody
    public String list(Model model, HttpServletRequest request, HttpServletResponse response){
        redisTemplate.setEnableTransactionSupport(true);
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String  html = (String)valueOperations.get("goodsList");
        if (!StringUtils.isEmpty(html)){
            return html;
        }
        try {
            List<ItemKill> itemKills = itemServiceImpl.getKillItems();
            model.addAttribute("itemkills",itemKills);
        }catch (Exception e) {
            log.error("获取商品列表异常",e.fillInStackTrace());
            return "redirect:/base/error";
        }
        WebContext context = new WebContext(request,response,request.getServletContext(),request.getLocale(),model.asMap());
        html = thymeleafViewResolver.getTemplateEngine().process("item",context);
        if (!StringUtils.isEmpty(html)){
            valueOperations.set("goodsList",html,60, TimeUnit.SECONDS);
        }
       return html;
    }

    @RequestMapping(value = "/detail/{id}",produces = "text/html;charset=utf-8",method = RequestMethod.GET)
    @ResponseBody
    public String detail(@PathVariable Integer id,Model model,HttpServletRequest request, HttpServletResponse response){
        if (id == null || id < 0){
            return "redirect:/base/error";
        }
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String html = (String) valueOperations.get("goodsDetail"+id);
        if (!StringUtils.isEmpty(html)){
            return html;
        }
        try {
            ItemKill itemKill = itemServiceImpl.getKillDetail(id);
            Date starDate = itemKill.getStartTime();
            Date endDate = itemKill.getEndTime();
            Date nowDate = new Date();
            //秒杀状态
            int seckillStatus = 0;
            int remainSeconds = 0;
            if (nowDate.before(starDate)){
                remainSeconds = (int) ((starDate.getTime() - nowDate.getTime())/1000);
            }else if (nowDate.after(endDate)){
                seckillStatus = 2;
                remainSeconds = -1;
            }else {
                seckillStatus = 1;
                remainSeconds = 0;
            }
            model.addAttribute("remainSeconds",remainSeconds);
            model.addAttribute("seckillStatus",seckillStatus);
            model.addAttribute("itemkill",itemKill);
        }catch (Exception e){
            log.error("获取详情发生异常：id={}"+id);
        }
        WebContext context = new WebContext(request,response,request.getServletContext(),request.getLocale(),model.asMap());
        html = thymeleafViewResolver.getTemplateEngine().process("detail",context);
        if (!StringUtils.isEmpty(html)){
            valueOperations.set("goodsDetail"+id,html,60, TimeUnit.SECONDS);
        }

        return html;
    }


}

package com.sec.demo.controller;

import com.sec.demo.entity.ItemKill;
import com.sec.demo.service.Impl.ItemServiceImpl;
import com.sec.demo.service.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

@Controller
public class ItemController {
    private static final Logger log = LoggerFactory.getLogger(ItemController.class);

    @Autowired
    private ItemServiceImpl itemServiceImpl;

    //获取商品列表
    @RequestMapping(value = "/item",method = RequestMethod.GET)
    public String list(Model model){
        try {
            List<ItemKill> itemKills = itemServiceImpl.getKillItems();
            model.addAttribute("itemkills",itemKills);
        }catch (Exception e) {
            log.error("获取商品列表异常",e.fillInStackTrace());
            return "redirect:/base/error";
        }
       return "item";
    }

    @RequestMapping(value = "/detail/{id}",method = RequestMethod.GET)
    public String detail(@PathVariable Integer id,Model model){
        if (id == null || id < 0){
            return "redirect:/base/error";
        }
        try {
            ItemKill itemKill = itemServiceImpl.getKillDetail(id);
            model.addAttribute("itemkill",itemKill);
        }catch (Exception e){
            log.error("获取详情发生异常：id={}"+id);
        }
        return "detail";
    }
}

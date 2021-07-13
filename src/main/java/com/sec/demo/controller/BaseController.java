package com.sec.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


@Controller
@RequestMapping("/base")
public class BaseController {

    @RequestMapping(value = "/error",method = RequestMethod.GET)
    public String error(){
        return "error";
    }
}

package com.sec.demo.controller;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.sql.DataSource;

@Controller
public class testController {

    @Autowired
    DataSource dataSource;

    @RequestMapping(value = "/test",method = RequestMethod.GET)
    public String index(){
        System.out.println(dataSource.getClass());
        DruidDataSource druidDataSource = (DruidDataSource) dataSource;
        System.out.println(druidDataSource.getInitialSize());

        return "test";
    }
}

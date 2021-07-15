package com.sec.demo.service;


public interface KillService {
    Boolean killItem(Integer killId,Integer userId) throws Exception;

    Boolean KillItemV2(Integer killId,Integer userId) throws Exception;

    Boolean KillItemV3(Integer killId,Integer userId) throws Exception;

    Boolean KillItemV4(Integer killId,Integer userId) throws Exception;
}

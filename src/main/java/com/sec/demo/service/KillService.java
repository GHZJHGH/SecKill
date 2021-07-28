package com.sec.demo.service;


import com.sec.demo.dto.GoodsForKill;
import com.sec.demo.entity.ItemKill;

import java.util.List;

public interface KillService {
    Boolean killItem(Integer killId,Integer userId) throws Exception;

    Boolean KillItemV2(Integer killId,Integer userId) throws Exception;

    Boolean KillItemV3(ItemKill itemKill, Integer userId) throws Exception;

    Boolean KillItemV4(Integer killId,Integer userId) throws Exception;

    List<GoodsForKill> searchKill();

    int getResult(Integer userid, Integer killid);

    String createPath(Object uid, int goodsId);

    boolean checkPath(int userId, int killId,String path);

    boolean checkCaptcha(Object uid, Integer goodsId, String captcha);
}

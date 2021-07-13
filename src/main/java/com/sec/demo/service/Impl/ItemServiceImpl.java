package com.sec.demo.service.Impl;

import com.sec.demo.entity.ItemKill;
import com.sec.demo.mapper.ItemKillMapper;
import com.sec.demo.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItemServiceImpl implements ItemService {
    @Autowired
    private ItemKillMapper itemKillMapper;

    @Override
    public List<ItemKill> getKillItems() {
        List<ItemKill> list = itemKillMapper.selectAll();
        return list;
    }

    @Override
    public ItemKill getKillDetail(Integer id) throws Exception {
        ItemKill itemKill = itemKillMapper.selectByid(id);
        if (itemKill == null){
            throw new Exception("秒杀详情记录不存在");
        }
        return itemKill;
    }
}

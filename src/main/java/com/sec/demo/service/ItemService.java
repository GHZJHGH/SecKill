package com.sec.demo.service;

import com.sec.demo.entity.ItemKill;

import java.util.List;

public interface ItemService {
    List<ItemKill> getKillItems();
    ItemKill getKillDetail(Integer id) throws Exception;
}

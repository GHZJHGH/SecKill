package com.sec.demo.entity;

import lombok.Data;
import lombok.ToString;

import java.util.Date;

@Data
@ToString
public class ItemKillSuccess {
    private String code;

    private Integer itemId;

    private Integer killId;

    private String userId;

    private Integer status;

    private Date createTime;

    private Integer diffTime;
}

package com.sec.demo.dto;

import lombok.Data;

import java.io.Serializable;

//抢购时就通过订单id和用户id数据来发起抢购
@Data
public class KillDto implements Serializable {
    private Integer killid;
    private Integer userid;

    public KillDto(){}
    public KillDto(Integer killid,Integer userid){
        this.killid = killid;
        this.userid = userid;
    }

}

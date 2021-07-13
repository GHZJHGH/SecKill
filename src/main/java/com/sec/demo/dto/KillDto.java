package com.sec.demo.dto;

import java.io.Serializable;

//抢购时就通过订单id和用户id数据来发起抢购
public class KillDto implements Serializable {
    private Integer killid;
    private Integer userid;

    public KillDto(){}
    public KillDto(Integer killid,Integer userid){
        this.killid = killid;
        this.userid = userid;
    }

    public Integer getKillid() {
        return killid;
    }

    public void setKillid(Integer killid) {
        this.killid = killid;
    }

    public Integer getUserid() {
        return userid;
    }

    public void setUserid(Integer userid) {
        this.userid = userid;
    }
}

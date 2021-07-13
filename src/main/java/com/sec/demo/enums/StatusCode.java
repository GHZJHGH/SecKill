package com.sec.demo.enums;

public enum StatusCode {
    //成功
    Success(0,"成功"),
    //失败
    Fail(-1,"失败"),
    //非法参数
    InvalidParam(201,"非法参数"),
    //未登录
    UserNotLog(202,"用户未登录"),
    ;
    private Integer code;
    private String msg;
    StatusCode(Integer code,String msg){
        this.code=code;
        this.msg=msg;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}

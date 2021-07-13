package com.sec.demo.enums;

public class SysConstant {
    public enum OrderStatus{
        //订单无效
        Invalid(-1,"无效"),
        //成功未支付
        SuccessNotPayed(0,"成功未支付"),
        //已支付
        HasPayed(1,"已支付"),
        //已取消
        Cancel(2,"已取消"),
        ;
        private Integer code;
        private String msg;
        OrderStatus (Integer code, String msg){
            this.code = code;
            this.msg = msg;
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
}

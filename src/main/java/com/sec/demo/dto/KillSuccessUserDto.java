package com.sec.demo.dto;

import com.sec.demo.entity.ItemKillSuccess;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@ToString
public class KillSuccessUserDto extends ItemKillSuccess implements Serializable {
    private String code;
    private String userName;
    private String phone;
    private String email;
    private String itemName;
}

package com.sec.demo.mapper;

import com.sec.demo.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
    @Select("select * from user where user_name=#{username}")
    User selectByName(@Param("username")String username);

    @Insert("insert into user(user_name,password) values (#{username},#{password})")
    int insertUser(@Param("username")String username,@Param("password")String password);

    @Insert("insert into user(id,user_name,password) values (#{id},#{username},#{password})")
    int insertUser2(@Param("id")int id,@Param("username")String username,@Param("password")String password);
}

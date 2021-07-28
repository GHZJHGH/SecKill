package com.sec.demo;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;


@SpringBootTest
class DemoApplicationTests {

    @Autowired
    DataSource dataSource;

    //@Test
    void contextLoads() throws SQLException {
        Connection connection = dataSource.getConnection();
        System.out.println(dataSource.getClass());
    }

    @Autowired
    private RedisTemplate redisTemplate;
    @Test
    public void testRedisLock(){
        ValueOperations valueOperations = redisTemplate.opsForValue();
        Boolean isLock = valueOperations.setIfAbsent("k1","v1",5, TimeUnit.SECONDS);
        if (isLock){
            valueOperations.set("name","xxxx");
            String name = (String) valueOperations.get("name");
            System.out.println("name" + name);
            //Integer.parseInt("xxxx");
            redisTemplate.delete("k1");
        }else {
            System.out.println("有线程在使用");
        }
    }

    public static void main(String[] args) {
        DemoApplicationTests t = new DemoApplicationTests();
        t.testRedisLock();
    }
}

package com.sec.demo.config;


import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class RedissonConfig {
    @Autowired
    private Environment environment;

    @Bean
    public RedissonClient redissonClient(){
        System.out.println(environment.getProperty("spring.redis.redisson"));
        Config config = new Config();
        config.useSingleServer()
                .setAddress(environment.getProperty("spring.redis.redisson"));
        RedissonClient client = Redisson.create(config);
        return client;
    }
}

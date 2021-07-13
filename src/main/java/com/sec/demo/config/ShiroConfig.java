package com.sec.demo.config;

import com.sec.demo.service.CustomRealm;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.servlet.ShiroFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ShiroConfig {
    //身份认证的realm
    @Bean
    public CustomRealm customRealm(){
        return new CustomRealm();
    }

    //安全管理器，并设置realm
    @Bean
    public DefaultWebSecurityManager securityManager(){
        DefaultWebSecurityManager manager = new DefaultWebSecurityManager();
        manager.setRealm(customRealm());
        return manager;
    }

    @Bean
    public ShiroFilterFactoryBean shiroFilterFactoryBean(){

        //创建一个ShiroFilter工厂
        ShiroFilterFactoryBean bean = new ShiroFilterFactoryBean();
        //设置安全管理器
        bean.setSecurityManager(securityManager());
        //设置登陆url
        bean.setLoginUrl("/login");
        //设置为认证的url
        bean.setUnauthorizedUrl("/unauth");
        //创建过滤器，在这里可以自定义需要过滤的url
        Map<String,String> filterChainDefinitionMap = new HashMap<String, String>();
        //anon: 无需认证即可访问
        //authc: 需要认证才可访问
        filterChainDefinitionMap.put("/login","anon");
        filterChainDefinitionMap.put("/register","anon");
        filterChainDefinitionMap.put("/item/*","authc");
        filterChainDefinitionMap.put("/kill/execute/*","authc");
        filterChainDefinitionMap.put("/**","authc");
        bean.setFilterChainDefinitionMap(filterChainDefinitionMap);

        return bean;
    }
}

package com.sec.demo.service;

import com.google.common.base.Objects;
import com.sec.demo.entity.User;
import com.sec.demo.mapper.UserMapper;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;

public class CustomRealm extends AuthorizingRealm {
    @Autowired
    public UserMapper userMapper;

    //认证-授权
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        return null;
    }

    //认证-登录
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {

        UsernamePasswordToken token = (UsernamePasswordToken) authenticationToken;
        String username = token.getUsername();
        String password = String.valueOf(token.getPassword());
        System.out.println(username+password);

        User user = userMapper.selectByName(username);
        if (user==null){
            throw new UnknownAccountException("用户名不存在");
        }

        if (!Objects.equal(1,user.getIsActive().intValue())){
            throw new DisabledAccountException("当前用户已被禁用");
        }

        if (!user.getPassword().equals(password)){
            throw new IncorrectCredentialsException("用户密码错误");
        }

        SimpleAuthenticationInfo info = new SimpleAuthenticationInfo(user.getId(),password,getName());
        setSession("uid",user.getId());
        return info;
    }

    //将key和对应的value装入shiro的session中，最终交给httpSession进行管理
    private void setSession(String key, Object value) {
        Session session = SecurityUtils.getSubject().getSession();
        if (session!=null){
            session.setAttribute(key,value);
            session.setTimeout(300000L);
        }
    }
}

package com.complain.community.controller.interceptor;

import com.complain.community.entity.LoginTicket;
import com.complain.community.entity.User;
import com.complain.community.service.UserService;
import com.complain.community.util.CookieUtil;
import com.complain.community.util.HostHolder;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;


/*
* 自动登录验证 ：通过Cookie中的票据（ticket）自动识别用户身份。
* 用户信息绑定 ：将认证后的用户信息绑定到当前线程（HostHolder）和Spring Security上下文中。
* 安全上下文管理 ：与Spring Security集成，为后续权限控制提供认证数据
* */


@Component
public class LoginTicketInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //从cookie中获取凭证
        String ticket = CookieUtil.getValue(request,"ticket");

        if(ticket != null){
            //查询凭证
            LoginTicket loginTicket = userService.findLoginTicket(ticket);
            //凭证是否有效
            if (loginTicket != null && loginTicket.getStatus() == 0 && loginTicket.getExpired().after(new Date())) {
                //根据凭证获取查询用户
                User user = userService.findUserById(loginTicket.getUserId());

                //在本次请求中持有用户
                hostHolder.setUsers(user);

                //构建用户认证的结果，并存入SecurityContext,以便Security进行授权
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        user,
                        user.getPassword(),
                        userService.getAuthorities(user.getId())
                );
                System.out.println("当前用户：" + authentication.getName());
                System.out.println("当前用户权限：" + authentication.getAuthorities());
                System.out.println("认证信息已设置：" + authentication);

                SecurityContextHolder.getContext().setAuthentication(authentication);

            }
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();
        if(user!= null && modelAndView!=null){
            modelAndView.addObject("loginUser",user);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        hostHolder.clear();
        /*
        * afterCompletion 的副作用
        * afterCompletion 在请求结束时调用，此时清除 SecurityContext 会导致：
        * 同一请求中后续的异步操作（如模板渲染）无法获取认证信息。
        * 在重定向或转发时，目标资源无法识别用户身份。
        * Spring Security 的默认行为
        * Spring Security 默认会在请求结束时自动清理 SecurityContext，无需手动调用 SecurityContextHolder.clearContext()。手动清除会干扰其生命周期管理。
        * */
        // SecurityContextHolder.clearContext();
    }
}

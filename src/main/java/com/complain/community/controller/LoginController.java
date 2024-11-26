package com.complain.community.controller;


import com.complain.community.entity.User;
import com.complain.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

@Controller
public class LoginController {

    @Autowired
    private UserService userService;

    @RequestMapping(path = "/register",method = RequestMethod.GET)

    public String getRegisterPage(){
        return "site/register";
    }

    @RequestMapping(path = "/register",method = RequestMethod.POST)
    public String register(Model model , User user){
        Map<String, Object> map = userService.register(user);
        if (map==null || map.isEmpty()){
            model.addAttribute("msq","注册成果啦，我们已向您的邮箱发送了邮件，请尽快激活！");
            model.addAttribute("target","/index");
            return "/site/operate-result";
        } else {
            model.addAttribute("usernameMsq",map.get("usernameMsq"));
            model.addAttribute("passwordMsq",map.get("passwordMsq"));
            model.addAttribute("emailMsq",map.get("emailMsq"));
            return "/site/register";
        }
    }

}

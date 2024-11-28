package com.complain.community.controller;


import com.complain.community.entity.User;
import com.complain.community.service.UserService;
import com.complain.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

@Controller
public class LoginController implements CommunityConstant {

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

    @RequestMapping(path = "/login",method = RequestMethod.GET)
    public String getLoginPage(){
        return "/site/login";
    }


    // http://localhost:8080/community/activation/101/code
    @RequestMapping(path = "/activation/{userId}/{code}",method = RequestMethod.GET)
    public String activation(Model model, @PathVariable("userId") int userId,@PathVariable("code") String code){
        int result = userService.activation(userId,code);
        if (result ==ACTIVATION_SUCCESS){
            model.addAttribute("msq","恭喜！激活成功，您的账号已经可以正常使用！");
            model.addAttribute("target","/login");
        } else if (result == ACTIVATION_REPEAT) {
            model.addAttribute("msq","抱歉，该账号已经激活过了！");
            model.addAttribute("target","/index");
        } else {
            model.addAttribute("msq","抱歉，激活失败，您提供的激活码不正确！");
            model.addAttribute("target","/index");
        }
        return "/site/operate-result";
    }



}

package com.complain.community.controller;

import com.complain.community.entity.User;
import com.complain.community.service.UserService;
import com.complain.community.util.CommunityConstant;
import com.complain.community.util.CommunityUtil;
import com.complain.community.util.RedisKeyUtil;
import com.google.code.kaptcha.Producer;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Controller
public class LoginController implements CommunityConstant {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserService userService;


    @Autowired
    private Producer kaptchaProducer;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private RedisTemplate redisTemplate;

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

    @RequestMapping(path = "/login",method = RequestMethod.POST)
    public String Login(String username, String password, String code, boolean rememberme,
                        Model model, HttpSession session, HttpServletResponse response,
                        @CookieValue(value = "kaptchaOwner",required = false) String kaptchaOwner){

        //检查验证码
        //String kaptcha = (String) session.getAttribute("kaptcha");
        String kaptcha = null;

        if (StringUtils.isNotBlank(kaptchaOwner)){
            String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha = (String) redisTemplate.opsForValue().get(redisKey);
        }else {
            model.addAttribute("codeMsq","验证码失效，刷新后重试！");
            return "/site/login";
        }

        if(StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)){
            model.addAttribute("codeMsq","验证码不正确");
            return "/site/login";
        }
        //检查账号，密码
        int expiredSecondes = rememberme ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> map = userService.login(username, password, expiredSecondes);
        if(map.containsKey("ticket")){
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expiredSecondes);
            response.addCookie(cookie);
            return "redirect:/index";

        }else {
            model.addAttribute("usernameMsq",map.get("usernameMsq"));
            model.addAttribute("passwordMsq",map.get("passwordMsq"));
            return "/site/login";
        }
    }

    @RequestMapping(path = "/logout",method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket){
        userService.logout(ticket);
        /*
        *  为什么不需要手动清理？
        * Spring Security 的自动清理机制
        * Spring Security 默认会在每个请求结束时（即 FilterChain 执行完毕后）自动清理 SecurityContext，无需手动干预。
        * 手动调用 SecurityContextHolder.clearContext() 可能会导致以下问题：
        * 重定向时认证信息丢失 ：在返回 redirect:/login 时，新的请求可能无法正确继承已清理的上下文。
        * 干扰框架逻辑 ：Spring Security 的 LogoutFilter 会处理认证清理（如使 Session 失效），手动清理可能引发冲突。
        * */
        // SecurityContextHolder.clearContext();
        return "redirect:/login";
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

    @RequestMapping(path = "/kaptcha",method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response, HttpSession session){
        //生成验证码
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        // 将验证码存入session
        // session.setAttribute("kaptcha",text);

        // 验证码的归属
        String kaptchaOwner = CommunityUtil.generateUUID();
        Cookie cookie = new Cookie("kaptchaOwner",kaptchaOwner);
        cookie.setMaxAge(60);
        cookie.setPath(contextPath);
        response.addCookie(cookie);
        //将验证码存入Redis
        String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        System.out.println();
        redisTemplate.opsForValue().set(redisKey,text,60, TimeUnit.SECONDS);

        //将图片输出给浏览器
        response.setContentType("image/png");
        try {
            ServletOutputStream outputStream = response.getOutputStream();
            ImageIO.write(image,"png",outputStream);
        } catch (IOException e) {
             logger.error("响应验证码失败: "+e);
        }


    }




}

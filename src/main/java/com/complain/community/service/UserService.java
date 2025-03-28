package com.complain.community.service;

import com.complain.community.dao.LoginTicketMapper;
import com.complain.community.dao.UserMapper;
import com.complain.community.entity.LoginTicket;
import com.complain.community.entity.User;
import com.complain.community.util.CommunityConstant;
import com.complain.community.util.CommunityUtil;
import com.complain.community.util.MailClient;
import com.complain.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private LoginTicketMapper loginTicketMapper;


    @Autowired
    private RedisTemplate redisTemplate;


    public User findUserById(int id) {
        // return userMapper.selectById(id);
        User user = getCache(id);
        if (user == null) {
            user = initCache(id);
        }
        return user;

    }

    public Map<String,Object> register(User user){

        Map<String, Object> map = new HashMap<>();

        //空置处理
        if(user ==null){
            throw new IllegalArgumentException("参数不能为空");
        }
        if (StringUtils.isBlank(user.getUsername())){
            map.put("usernameMsq","账号不能为空，请输入！");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())){
            map.put("passwordMsq","密码不能为空，请输入！");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())){
            map.put("emailMsq","邮箱不能为空，请输入！");
            return map;
        }

        //验证账号
        User u = userMapper.selectByName(user.getUsername());
        if (u != null){
            map.put("usernameMsq","该账号已存在，请重新输入！");
            return map;
        }


        //验证邮箱
        u = userMapper.selectByEmail(user.getEmail());
        if (u != null){
            map.put("emailMsq","该邮箱已存在，请重新输入！");
            return map;
        }

        //注册用户
        user.setSalt(CommunityUtil.generateUUID().substring(0,5));
        user.setPassword(CommunityUtil.md5(user.getPassword()+user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png",new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        // 激活邮件
        Context context = new Context();
        context.setVariable("email",user.getEmail());
        // http://localhost:8080/community/activation/101/code

        String url = domain + contextPath + "/activation/" +user.getId()+ "/" +user.getActivationCode();
        context.setVariable("url",url);
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(user.getEmail(), "激活账号",content);

        return map;
    }

    public int activation(int userId,String code){
        User user = userMapper.selectById(userId);
        if (user.getStatus() ==1 ){
            return ACTIVATION_REPEAT;
        }else if (user.getActivationCode().equals(code)){
            userMapper.updateStatus(userId,1);
            clearCache(userId);
            return ACTIVATION_SUCCESS;
        }else {
            return ACTIVATION_FAILURE;
        }
    }

    public Map<String,Object> login(String username, String password,int expiredSeconds){
       Map<String,Object> map =new HashMap<>();
       //空值处理
        if (StringUtils.isBlank(username)){
            map.put("usernameMsq","账号不能为空，请重新输入！");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsq", "密码不能为空，请重新输入！");
            return map;
        }

        //验证账号
        User user = userMapper.selectByName(username);

        if(user == null){
            map.put("usernameMsq","账号不存在，请重新输入！");
            return map;
        }

        if(user.getStatus()==0){
            map.put("usernameMsq","账号未激活，请重新输入！");
            return map;
        }

        //验证密码
        String pd = CommunityUtil.md5(password+user.getSalt());
        if(!user.getPassword().equals(pd)){
            map.put("passwordMsq", "密码不正确，请重新输入！");
            return map;
        }

        //生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setExpired(new Date(System.currentTimeMillis()+ TimeUnit.SECONDS.toMillis(expiredSeconds)));
        loginTicket.setStatus(0);

        // loginTicketMapper.insertLoginTicket(loginTicket);
        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(redisKey,loginTicket);

        map.put("ticket",loginTicket.getTicket());
        return map;
    }

    public void logout(String ticket){
        // loginTicketMapper.updateStatus(ticket,1);
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(redisKey,loginTicket);
    }


    public LoginTicket findLoginTicket(String ticket){
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(redisKey);
       // return loginTicketMapper.selectByTicket(ticket);
    }

    public int updateHeader(int userId,String headerUrl){

        // return userMapper.updateHeader(userId, headerUrl);

        int rows = userMapper.updateHeader(userId, headerUrl);
        clearCache(userId);
        return rows;
    }

    public int updatePassword(int userId,String password){
        return userMapper.updatePassword(userId,password);
    }

    public User findUserByName(String username){
        return userMapper.selectByName(username);
    }

    //1.优先从缓存中取值
    private User getCache(int userId){
        String redisKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);
    }

    // 2.取不到时初始化缓存数据
    private User initCache(int userId) {
        User user = userMapper.selectById(userId);
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS);
        return user;
    }

    // 3.数据变更时清除缓存数据
    private void clearCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }

    public Collection<? extends GrantedAuthority> getAuthorities(int userId) {
        User user = this.findUserById(userId);

        List<GrantedAuthority> list = new ArrayList<>();

        if (user.getType() == 1) {
            list.add(new SimpleGrantedAuthority(AUTHORITY_ADMIN));
        } else if (user.getType() == 2) {
            list.add(new SimpleGrantedAuthority(AUTHORITY_MODERATOR));
        } else {
            list.add(new SimpleGrantedAuthority(AUTHORITY_USER));
        }
        return list;
    }

//    public Collection<? extends GrantedAuthority> getAuthorities(int userId){
//        User user = this.findUserById(userId);
//
//        List<GrantedAuthority> list =new ArrayList<>();
//
//        list.add(new GrantedAuthority() {
//            @Override
//            public String getAuthority() {
//                switch (user.getType()){
//                    case 1 :
//                        return AUTHORITY_ADMIN;
//                    case 2 :
//                        return AUTHORITY_MODERATOR;
//                    default:
//                        return AUTHORITY_USER;
//                }
//            }
//        });
//        return list;
//    }


}

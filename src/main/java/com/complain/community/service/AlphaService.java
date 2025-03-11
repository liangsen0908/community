package com.complain.community.service;

import com.complain.community.dao.AlphaDao;
import com.complain.community.dao.DiscussPostMapper;
import com.complain.community.dao.UserMapper;
import com.complain.community.entity.DiscussPost;
import com.complain.community.entity.User;
import com.complain.community.util.CommunityUtil;

import org.springframework.beans.factory.annotation.Autowired;



import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.swing.plaf.ComponentUI;
import java.util.Date;


@Service
//@Scope("prototype")
public class AlphaService {

    @Autowired
    private AlphaDao alphaDao;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private TransactionTemplate transactionTemplate;

    public AlphaService() {
        System.out.println("实例化AlphaService");
    }

    @PostConstruct
    public void init() {
        System.out.println("初始化AlphaService");
    }

    @PreDestroy
    public void destroy() {
        System.out.println("销毁AlphaService");
    }

    public String find() {
        return alphaDao.select();
    }


    /*
    * REQUIRED:支持当前事物（外部事物），如果不存在就创建新事物
    * REQUIRES_NEW：创建新事物，并且暂停当前事物（外部事物）
    * NESTED：如果当前存在当前事物（外部事物），则嵌套在该事物中执行（独立的提交和回滚），否则就会和REQUIRERD一样
    *
    * */

    @Transactional(isolation = Isolation.READ_COMMITTED,propagation = Propagation.REQUIRED)
    public Object save1(){
        //新增用户
        User user = new User();
         user.setUsername("alpha");
         user.setSalt(CommunityUtil.generateUUID().substring(0,5));
         user.setPassword(CommunityUtil.md5("123"+user.getSalt()));
         user.setEmail("alpha@qq.com");
         user.setCreateTime(new Date());
         userMapper.insertUser(user);

         //新增帖子
        DiscussPost discussPost = new DiscussPost();
        discussPost.setUserId(user.getId());
        discussPost.setTitle("大傻子");
        discussPost.setContent("今天天气真好");
        discussPost.setCreateTime(new Date());
        discussPostMapper.insertDiscussPost(discussPost);

        Integer.valueOf("adc");

        return "ok";
    }

    public Object save2(){
        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        
        return transactionTemplate.execute(new TransactionCallback<Object>() {
            @Override
            public Object doInTransaction(TransactionStatus status) {
                //新增用户
                User user = new User();
                user.setUsername("beta");
                user.setSalt(CommunityUtil.generateUUID().substring(0,5));
                user.setPassword(CommunityUtil.md5("123"+user.getSalt()));
                user.setEmail("alpha@qq.com");
                user.setCreateTime(new Date());
                userMapper.insertUser(user);

                //新增帖子
                DiscussPost discussPost = new DiscussPost();
                discussPost.setUserId(user.getId());
                discussPost.setTitle("傻子");
                discussPost.setContent("今天真好");
                discussPost.setCreateTime(new Date());
                discussPostMapper.insertDiscussPost(discussPost);

                Integer.valueOf("wqw");

                return "ok";
            }
        });

    }

}

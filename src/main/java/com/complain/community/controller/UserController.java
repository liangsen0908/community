package com.complain.community.controller;

import com.complain.community.annotation.LoginRequired;
import com.complain.community.entity.User;
import com.complain.community.service.FollowService;
import com.complain.community.service.LikeService;
import com.complain.community.service.UserService;
import com.complain.community.util.CommunityConstant;
import com.complain.community.util.CommunityUtil;
import com.complain.community.util.HostHolder;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant{
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;
    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @LoginRequired
    @RequestMapping(path = "/setting",method = RequestMethod.GET)
    public String getSettingPage(){
        return "/site/setting";
    }


    @LoginRequired
    @RequestMapping(path = "/upload",method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model){
        if(headerImage==null){
            model.addAttribute("error","您还没有选择图片");
            return "/site/setting";
        }
        String originalFilename = headerImage.getOriginalFilename();
        String substring = originalFilename.substring(originalFilename.lastIndexOf("."));
        if (StringUtils.isBlank(substring)){
            model.addAttribute("error","您上传的文件名不正确");
            return "/site/setting";
        }

        String contentType = headerImage.getContentType();

        System.out.println(contentType);

        if (!contentType.startsWith("image/")) {
            model.addAttribute("error", "您上传的文件不是图片");
            return "/site/setting";
        }
        //生成随机文件名
        String fileName = CommunityUtil.generateUUID() + substring;
        //确定文件存放的路径
        File dest = new File(uploadPath + "/" + fileName);
        try {
            // 将上传的文件（headerImage）保存到服务器上的指定路径（dest）
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败： "+e.getMessage());
            model.addAttribute("error", "上传头像失败，请稍后再试");
            throw new RuntimeException("上传文件失败，服务器异常！",e);
        }
        //更新当前用户的头像的路径（web 访问路径）
        // http://localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String hraderUrl = domain+contextPath+"/user/header/"+fileName;
        userService.updateHeader(user.getId(),hraderUrl);
        return "redirect:/index";
    }

    @RequestMapping(path="/header/{fileName}",method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response){
        //服务器存放的路径
        String serviceFileName = uploadPath+"/"+fileName;
        //文件的后缀
        String substring = serviceFileName.substring(serviceFileName.lastIndexOf("."));
        //响应图片
        response.setContentType("image/"+substring);
        try {
            ServletOutputStream outputStream = response.getOutputStream();
            FileInputStream fis = new FileInputStream(serviceFileName);
            byte[] buffer = new byte[1024];
            int b=0;
            while((b=fis.read(buffer))!=-1){
                outputStream.write(buffer,0,b);
            }
        } catch (IOException e) {
            logger.error("获取头像失败："+e.getMessage());
        }

    }

    @LoginRequired
    @RequestMapping(path = "/updatePassword",method = RequestMethod.POST)
    public String updatePassword(String oldPassword,String newPassword, Model model){
        if(oldPassword ==null){
            model.addAttribute("oldPasswordError","您需要输入旧密码才能修改");
            return "/site/setting";
        }
        User user = hostHolder.getUser();
        if (oldPassword != null && !user.getPassword().equals( CommunityUtil.md5(oldPassword+user.getSalt()))){
            model.addAttribute("oldPasswordError","您输入的旧密码不正确");
            return "/site/setting";
        }
        if(newPassword == null){
            model.addAttribute("newPasswordError","您未输入修改的密码");
            return "/site/setting";
        }
        userService.updatePassword(user.getId(),CommunityUtil.md5(newPassword+user.getSalt()));

        return "redirect:/index";
    }


    //个人主页
    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }

        // 用户
        model.addAttribute("user", user);
        // 点赞数量
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);

        //关注数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount",followeeCount);
        //粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER,userId);
        model.addAttribute("followerCount",followerCount);
        //是否已关注
        boolean hasFollowed = false;
        if( hostHolder.getUser() != null){
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER,userId);
        }
        model.addAttribute("hasFollowed",hasFollowed);
        return "/site/profile";
    }



}


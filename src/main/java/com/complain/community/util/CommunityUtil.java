package com.complain.community.util;




import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class CommunityUtil {
    //生成随机字符串
    public static String generateUUID(){
        return UUID.randomUUID().toString().replaceAll("-","");
    }
    //MD5加密
    //hello -> abc123sasa
    //hello+2ds2d -> hello2ds2d
    public static String md5(String key){
        if (StringUtils.isBlank(key))
            return null;
        return DigestUtils.md5DigestAsHex(key.getBytes());
    }
}

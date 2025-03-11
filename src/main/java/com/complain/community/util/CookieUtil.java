package com.complain.community.util;


import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

public class CookieUtil {
    public static String getValue(HttpServletRequest request, String name){
        if(request == null || name==null){
            throw new IllegalArgumentException("参数为空");
        }
        Cookie[] cookies = request.getCookies();
        if(cookies != null){
            for (Cookie c:cookies){
                if (c.getName().equals(name)){
                    return c.getValue();
                }
            }
        }
        return null;
    }}





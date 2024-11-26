package com.complain.community;


import com.complain.community.util.MailClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;


@SpringBootTest
public class MailTests {
    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Test
    public void testTextMail(){
        mailClient.sendMail("1826936@163.com","TEST","Welcome!");
    }

    @Test
    public void testHtmlMail(){
        Context context = new Context();
        context.setVariable("username","sls");
        String content = templateEngine.process("mail/demo", context);

        System.out.println(content);
        mailClient.sendMail("1826936@163.com","HTML",content);

    }

}

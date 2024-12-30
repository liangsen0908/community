package com.complain.community;

import com.complain.community.service.AlphaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestExecutionListeners;


@SpringBootTest
public class TransactionTests {
    @Autowired
    private AlphaService alphaService;


    @Test
    public void testSavel1(){
        Object o = alphaService.save1();
        System.out.println(o);
    }
    @Test
    public void testSavel2(){
        Object o = alphaService.save2();
        System.out.println(o);
    }
}

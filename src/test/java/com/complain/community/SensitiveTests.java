package com.complain.community;


import com.complain.community.util.SensitiveFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(classes = CommunityApplication.class)
public class SensitiveTests {

    @Autowired
    private SensitiveFilter sensitiveFilter;
    @Test
    public void testSensitiveFilter(){
        String text = "dnsidnsid赌博@赌博@，随后对上述我的书我，嫖娼wijdiwed去死当时的";
        String filter = sensitiveFilter.filter(text);
        System.out.println(filter);
    }
}

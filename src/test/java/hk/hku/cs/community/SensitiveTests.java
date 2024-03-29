package hk.hku.cs.community;

import hk.hku.cs.community.util.SensitiveFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class SensitiveTests {

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Test
    public void testSensitiveFilter() {
        String text = "这里可以赌博，可以嫖娼，可以吸毒，可以开票！";
        text = sensitiveFilter.filter(text);
        System.out.println(text);

        text = "这里可以✶赌✶博，可以嫖✶娼✶，可以吸✶毒✶，可以✶开✶票！";
        text = sensitiveFilter.filter(text);
        System.out.println(text);
    }

}

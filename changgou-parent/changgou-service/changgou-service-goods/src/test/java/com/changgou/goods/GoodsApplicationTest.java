package com.changgou.goods;

import com.changgou.content.feign.ContentFeign;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@EnableFeignClients(basePackages = {"com.changgou.content.feign"})
public  class GoodsApplicationTest {

    @Autowired
    ContentFeign contentFeign;
    @Test
    public void contextLoads() {
      var t =  contentFeign.findByCategory(2);
      System.out.println(t.getCode());
    }

}

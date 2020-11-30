package com.changgou.order.controller;

import com.changgou.order.pojo.OrderItem;
import com.changgou.order.service.CartService;
import com.google.common.collect.Ordering;
import entity.Result;
import entity.StatusCode;
import entity.TokenDecode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private TokenDecode tokenDecode;

    @GetMapping(value = "/add")
    public Result add(int num, long id){
        String username =  tokenDecode.getUserInfo().get("username");
        cartService.add(num,id,username);
        return new Result(true, StatusCode.OK,"添加成功");
    }

    @GetMapping(value = "/list")
    public Result<List<OrderItem>> list(){
        //String username = "xjlonly";
        String username =  tokenDecode.getUserInfo().get("username");
        List<OrderItem> orderItems = cartService.list(username);
        return  new Result<>(true,StatusCode.OK,"查询成功", orderItems);
    }
}

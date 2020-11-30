package com.changgou.order.service;

import com.changgou.order.pojo.OrderItem;

import java.util.List;

public interface CartService {
    void add(int num, long id, String username);

    List<OrderItem> list(String username);
}

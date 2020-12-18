package com.changgou.seckill.service;

import com.changgou.seckill.pojo.SeckillOrder;
import com.github.pagehelper.PageInfo;
import entity.SeckillStatus;

import java.util.List;

/****
 * @Author:shenkunlin
 * @Description:SeckillOrder业务层接口
 * @Date 2019/6/14 0:16
 *****/
public interface SeckillOrderService {



    /***
     * 抢单状态查询
     * @param username
     */
    SeckillStatus queryStatus(String username);

    /***
     * 添加秒杀订单
     * @param id:商品ID
     * @param time:商品秒杀开始时间
     * @param username:用户登录名
     * @return
     */
    Boolean add(Long id, String time, String username);


}

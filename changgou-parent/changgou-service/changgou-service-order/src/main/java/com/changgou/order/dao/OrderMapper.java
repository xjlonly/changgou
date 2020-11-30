package com.changgou.order.dao;
import com.changgou.order.pojo.Order;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.common.Mapper;

/****
 * @Author:shenkunlin
 * @Description:Order的Dao
 * @Date 2019/6/14 0:12
 *****/
@Component
public interface OrderMapper extends Mapper<Order> {
}

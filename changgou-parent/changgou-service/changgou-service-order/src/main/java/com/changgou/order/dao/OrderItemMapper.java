package com.changgou.order.dao;
import com.changgou.order.pojo.OrderItem;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.common.Mapper;

/****
 * @Author:shenkunlin
 * @Description:OrderItem的Dao
 * @Date 2019/6/14 0:12
 *****/
@Component
public interface OrderItemMapper extends Mapper<OrderItem> {
}

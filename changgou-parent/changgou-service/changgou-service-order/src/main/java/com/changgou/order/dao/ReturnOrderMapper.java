package com.changgou.order.dao;
import com.changgou.order.pojo.ReturnOrder;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.common.Mapper;

/****
 * @Author:shenkunlin
 * @Description:ReturnOrder的Dao
 * @Date 2019/6/14 0:12
 *****/
@Component
public interface ReturnOrderMapper extends Mapper<ReturnOrder> {
}

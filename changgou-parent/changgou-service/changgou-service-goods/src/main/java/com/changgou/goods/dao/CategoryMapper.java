package com.changgou.goods.dao;
import com.changgou.goods.pojo.Category;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.common.Mapper;

/****
 * @Author:shenkunlin
 * @Description:Category的Dao
 * @Date 2019/6/14 0:12
 *****/
@Component
public interface CategoryMapper extends Mapper<Category> {
}

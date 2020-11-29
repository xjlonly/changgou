package com.changgou.goods.dao;
import com.changgou.goods.pojo.Sku;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.common.Mapper;

/****
 * @Author:shenkunlin
 * @Description:Sku的Dao
 * @Date 2019/6/14 0:12
 *****/
@Component
public interface SkuMapper extends Mapper<Sku> {

    /**
     * 递减库存
     * @param num
     * @param skuId
     * @return
     */
    @Update("UPDATE tb_sku SET num=num-#{num},sale_num=sale_num+#{num} WHERE id=#{skuId} AND num>=#{num}")
    int decrCount(@Param(value = "num") int num, @Param(value = "skuId") long skuId);
}

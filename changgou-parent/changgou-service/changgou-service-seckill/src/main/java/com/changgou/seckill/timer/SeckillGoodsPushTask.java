package com.changgou.seckill.timer;

import com.changgou.seckill.dao.SeckillGoodsMapper;
import com.changgou.seckill.pojo.SeckillGoods;
import entity.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.entity.Example;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author ：xjlonly
 * @date ：Created in 2020/12/14 15:06
 * @description：定时任务类
 * @modified By：
 * @version: 1.0$
 */
@Component
public class SeckillGoodsPushTask {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired private SeckillGoodsMapper seckillGoodsMapper;
    @Autowired private RedisTemplate<String,Object> redisTemplate;

    @Scheduled(cron = "0/30 * * * * ? ")
    public void loadGoodsPushRedis(){

        //获取时间段集合
        List<Date> dateMenus = DateUtil.getDateMenus();
        for (Date startTime : dateMenus){
            // namespace = SeckillGoods_20195712
            String extName = DateUtil.data2str(startTime,DateUtil.PATTERN_YYYYMMDDHH);
            //根据时间段数据查询对应的秒杀商品数据
            Example example = new Example(SeckillGoods.class);
            Example.Criteria criteria = example.createCriteria();
            // 1)商品必须审核通过  status=1
            criteria.andEqualTo("status","1");
            // 2)库存>0
            criteria.andGreaterThan("stockCount",0);
            // 3)开始时间<=活动开始时间
            criteria.andGreaterThanOrEqualTo("startTime",startTime);
            // 4)活动结束时间<开始时间+2小时
            criteria.andLessThan("endTime", DateUtil.addDateHour(startTime,2));
            // 5)排除之前已经加载到Redis缓存中的商品数据
            Set keys = redisTemplate.boundHashOps("SeckillGoods_" + extName).keys();
            if(keys!=null && keys.size()>0){
                criteria.andNotIn("id",keys);
            }

            //查询数据
            List<SeckillGoods> seckillGoods = seckillGoodsMapper.selectByExample(example);

            //将秒杀商品数据存入到Redis缓存
            for (SeckillGoods seckillGood : seckillGoods) {
                logger.info("商品存入数据库：{}", seckillGood.getId());
                redisTemplate.boundHashOps("SeckillGoods_"+extName).put(seckillGood.getId().toString(),seckillGood);
                redisTemplate.expireAt("SeckillGoods_"+extName,DateUtil.addDateHour(startTime, 2));
                //给每个商品做个队列
                redisTemplate.boundListOps("SeckillGoodsQueue_" + seckillGood.getId())
                        .leftPushAll(putAllIds(seckillGood.getStockCount(),seckillGood.getId()));
            }
        }

    }

    public Long[] putAllIds(Integer num, Long id){
        Long[] ids = new Long[num];
        Arrays.fill(ids, id);
        return ids;
    }
}

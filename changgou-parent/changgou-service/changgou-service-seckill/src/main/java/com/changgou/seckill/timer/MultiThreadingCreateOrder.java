package com.changgou.seckill.timer;

import com.changgou.seckill.dao.SeckillGoodsMapper;
import com.changgou.seckill.pojo.SeckillGoods;
import com.changgou.seckill.pojo.SeckillOrder;
import entity.IdWorker;
import entity.SeckillStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @author ：xjlonly
 * @date ：Created in 2020/12/18 14:41
 * @description：多线程下单
 * @modified By：
 * @version: 1.0$
 */
@Component
public class MultiThreadingCreateOrder {
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired private IdWorker idWorker;

    @Autowired private SeckillGoodsMapper seckillGoodsMapper;

    Logger logger = LoggerFactory.getLogger(getClass());
    /***
     * 多线程下单操作
     */
    @Async
    public void createOrder(){
        try {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            SeckillStatus seckillStatus = (SeckillStatus) redisTemplate.boundListOps("SeckillOrderQueue").rightPop();
            Long id = seckillStatus.getGoodsId();
            String time = seckillStatus.getTime();
            String username = seckillStatus.getUsername();
            if(seckillStatus != null){
                SeckillGoods seckillGood = (SeckillGoods)redisTemplate.boundHashOps("SeckillGoods_" + seckillStatus.getTime()).get(seckillStatus.getGoodsId().toString());

                if(seckillGood == null || seckillGood.getStockCount() <= 0){
                    throw  new RuntimeException("已售罄");
                }
                //如果有库存，则创建秒杀商品订单
                SeckillOrder seckillOrder = new SeckillOrder();
                seckillOrder.setId(idWorker.nextId());
                seckillOrder.setSeckillId(id);
                seckillOrder.setMoney(seckillGood.getCostPrice());
                seckillOrder.setUserId(username);
                seckillOrder.setCreateTime(new Date());
                seckillOrder.setStatus("0");


                //将秒杀订单存入redis
                redisTemplate.boundHashOps("SeckillOrder").put(username, seckillOrder);

                //库存减少
                seckillGood.setStockCount(seckillGood.getStockCount()-1);
                //判断当前商品是否还有库存
                if(seckillGood.getStockCount()<=0){
                    //并且将商品数据同步到MySQL中
                    seckillGoodsMapper.updateByPrimaryKeySelective(seckillGood);
                    //如果没有库存,则清空Redis缓存中该商品
                    redisTemplate.boundHashOps("SeckillGoods_" + time).delete(id.toString());
                }else{
                    //如果有库存，则直数据重置到Reids中
                    redisTemplate.boundHashOps("SeckillGoods_" + time).put(id.toString(),seckillGood);
                }

                //抢单成功，更新抢单状态,排队->等待支付
                seckillStatus.setStatus(2);
                seckillStatus.setOrderId(seckillOrder.getId());
                seckillStatus.setMoney(Float.valueOf(seckillOrder.getMoney()));
                redisTemplate.boundHashOps("UserQueueStatus").put(username,seckillStatus);

            }

        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }
}

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
            if(seckillStatus == null){
                return;
            }


            Long id = seckillStatus.getGoodsId();
            String time = seckillStatus.getTime();
            String username = seckillStatus.getUsername();
            //获取商品库存数据 判断商品是否还有库存
            Object sgood = redisTemplate.boundListOps("SeckillGoodsCountList_" + id).rightPop();
            if(sgood==null){
                //清理当前用户的排队信息
                clearQueue(seckillStatus);
                return;
            }


            //查询秒杀商品
            String namespace = "SeckillGoods_" + time;
            SeckillGoods seckillGood =
                    (SeckillGoods)redisTemplate.boundHashOps(namespace).get(id.toString());

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

            Long size = redisTemplate.boundListOps("SeckillGoodsCountList_" + id).size();

            //判断当前商品是否还有库存
            if(size <= 0){
                seckillGood.setStockCount(size.intValue());
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

        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    /***
     * 清理用户排队信息
     * @param seckillStatus
     */
    public void clearQueue(SeckillStatus seckillStatus){
        //清理排队标示
        redisTemplate.boundHashOps("UserQueueCount").delete(seckillStatus.getUsername());

        //清理抢单标示
        redisTemplate.boundHashOps("UserQueueStatus").delete(seckillStatus.getUsername());
    }
}

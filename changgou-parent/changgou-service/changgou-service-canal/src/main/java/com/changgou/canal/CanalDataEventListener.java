package com.changgou.canal;

import com.alibaba.fastjson.JSON;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.changgou.config.RabbitmqConfig;
import com.changgou.content.feign.ContentFeign;
import com.netflix.discovery.BackupRegistry;
import com.netflix.discovery.converters.Auto;
import com.xpand.starter.canal.annotation.*;
import com.xpand.starter.canal.annotation.ListenPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.core.ReactiveRedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.UUID;

@CanalEventListener
public class CanalDataEventListener {

    private   Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private ContentFeign contentFeign;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

//    /**
//     *
//     * 增加监听
//     */
//    @InsertListenPoint
//    public void onEventInsert(CanalEntry.EventType eventType, CanalEntry.RowData rowData){
//        rowData.getAfterColumnsList().forEach(c-> logger.info("数据列：{}，值：{}",c.getName(),c.getValue()));
//    }
//
    /***
     * 修改数据监听
     * @param rowData
     */
    @UpdateListenPoint
    public void onEventUpdate(CanalEntry.RowData rowData) {
        logger.info("UpdateListenPoint");
        rowData.getAfterColumnsList().forEach((c) -> logger.info("By--Annotation: " + c.getName() + " ::   " + c.getValue()));
    }
//
//    /***
//     * 删除数据监听
//     * @param eventType
//     */
//    @DeleteListenPoint
//    public void onEventDelete(CanalEntry.EventType eventType) {
//        logger.info("DeleteListenPoint");
//    }
//
//    /***
//     * 自定义数据修改监听
//     * @param eventType
//     * @param rowData
//     */
//    @ListenPoint(destination = "example", schema = "changgou_content", table = {"tb_content_category", "tb_content"}, eventType = CanalEntry.EventType.UPDATE)
//    public void onEventCustomUpdate(CanalEntry.EventType eventType, CanalEntry.RowData rowData) {
//        logger.info("custom listener update data");
//        rowData.getAfterColumnsList().forEach((c) -> logger.info("By--Annotation: " + c.getName() + " ::   " + c.getValue()));
//    }

    final RabbitTemplate.ConfirmCallback confirmCallback = new RabbitTemplate.ConfirmCallback() {
        @Override
        public void confirm(CorrelationData correlationData, boolean ack, String cause) {
            logger.info("消息ACK结果:{},correlationData:{}",ack,correlationData.getId());
        }
    };
    /***
     * 自定义数据修改监听
     * @param eventType
     * @param rowData
     */
    @ListenPoint(destination = "example",
            schema = "changgou_content", table = {"tb_content","tb_content_category"},
            eventType = {CanalEntry.EventType.UPDATE,CanalEntry.EventType.INSERT, CanalEntry.EventType.DELETE})
    public void onEventCustomContent(CanalEntry.EventType eventType, CanalEntry.RowData rowData) {
        logger.info("广告数据有变更，更新缓存数据...");
        String categoryId = getColumnValue(eventType, rowData);
        var result = contentFeign.findByCategory(Long.parseLong(categoryId));
        var contents = result.getData();
        stringRedisTemplate.boundValueOps("content_" + categoryId).set(JSON.toJSONString(contents));

        rabbitTemplate.setConfirmCallback(confirmCallback);
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        rabbitTemplate.convertAndSend(RabbitmqConfig.EXCHANGE_TOPICS_INFORM, "inform.content", categoryId,correlationData);
    }

    /*
    *获取分类标识
    * */
    private String getColumnValue(CanalEntry.EventType eventType, CanalEntry.RowData rowData){
        String categoryId = "";
        String columnName = "category_id";
        if(eventType.equals(CanalEntry.EventType.DELETE) ){
            for (CanalEntry.Column column : rowData.getBeforeColumnsList()){
                if(column.getName().equalsIgnoreCase(columnName)){
                    categoryId = column.getValue();
                    return categoryId;
                }
            }
        }
        else{
            for(CanalEntry.Column column : rowData.getAfterColumnsList()){
                if(column.getName().equalsIgnoreCase(columnName)){
                    categoryId = column.getValue();
                    return categoryId;
                }
            }
        }
        return categoryId;
    }



    @ListenPoint(destination = "example",
            schema = "changgou_goods",
            table = {"tb_spu"},
            eventType = {CanalEntry.EventType.UPDATE, CanalEntry.EventType.INSERT, CanalEntry.EventType.DELETE})
    public void onEventCustomSpu(CanalEntry.EventType eventType, CanalEntry.RowData rowData){
        long spuId = 0L;
        boolean delete =false;
        if(eventType == CanalEntry.EventType.DELETE){
            List<CanalEntry.Column> beforeColumnsList = rowData.getBeforeColumnsList();
            for(CanalEntry.Column column : beforeColumnsList){
                if(column.getName().equals("id")){
                    spuId = Long.parseLong(column.getValue());
                    delete = true;
                    break;
                }
            }
        }
        else{
            List<CanalEntry.Column> afterColumnsList= rowData.getAfterColumnsList();
            for(CanalEntry.Column column : afterColumnsList){
                if(column.getName().equals("id")){
                    spuId = Long.parseLong(column.getValue());
                    break;
                }
            }
        }

        rabbitTemplate.setConfirmCallback(confirmCallback);
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        rabbitTemplate.convertAndSend(RabbitmqConfig.EXCHANGE_TOPICS_INFORM,"inform.html", JSON.toJSONString(new SpuData(spuId, delete)),correlationData);
    }

    class SpuData {
        public final long spuId;
        public final boolean delete;
        public SpuData(long spuId, boolean delete){
            this.spuId = spuId;
            this.delete = delete;
        }
    }
}

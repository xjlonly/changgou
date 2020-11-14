package com.changgou.canal;

import com.alibaba.fastjson.JSON;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.changgou.content.feign.ContentFeign;
import com.netflix.discovery.converters.Auto;
import com.xpand.starter.canal.annotation.*;
import com.xpand.starter.canal.annotation.ListenPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.core.ReactiveRedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

@CanalEventListener
public class CanalDataEventListener {

    private   Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private ContentFeign contentFeign;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

//    /**
//     *
//     * 增加监听
//     */
//    @InsertListenPoint
//    public void onEventInsert(CanalEntry.EventType eventType, CanalEntry.RowData rowData){
//        rowData.getAfterColumnsList().forEach(c-> logger.info("数据列：{}，值：{}",c.getName(),c.getValue()));
//    }
//
//    /***
//     * 修改数据监听
//     * @param rowData
//     */
//    @UpdateListenPoint
//    public void onEventUpdate(CanalEntry.RowData rowData) {
//        logger.info("UpdateListenPoint");
//        rowData.getAfterColumnsList().forEach((c) -> logger.info("By--Annotation: " + c.getName() + " ::   " + c.getValue()));
//    }
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


    /***
     * 自定义数据修改监听
     * @param eventType
     * @param rowData
     */
    @ListenPoint(destination = "example",
            schema = "changgou_content", table = {"tb_content","tb_content_category"},
            eventType = {CanalEntry.EventType.UPDATE,CanalEntry.EventType.INSERT, CanalEntry.EventType.DELETE})
    public void onEventCustomUpdate(CanalEntry.EventType eventType, CanalEntry.RowData rowData) {
        logger.info("广告数据有变更，更新缓存数据...");
        String categoryId = getColumnValue(eventType, rowData);
        var result = contentFeign.findByCategory(Long.parseLong(categoryId));
        var contents = result.getData();
        stringRedisTemplate.boundValueOps("content_" + categoryId).set(JSON.toJSONString(contents));
    }

    /*
    *
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

}

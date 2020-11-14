package com.changgou.canal;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.changgou.content.feign.ContentFeign;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson.JSON;

import javax.swing.text.TabExpander;
import java.net.InetSocketAddress;
import java.util.List;

//@Component
public class ReadBinLogService implements ApplicationRunner {
    private Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private ContentFeign contentFeign;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${canal.host}")
    private String host;
    @Value("${canal.port}")
    private int port;
    @Value("${canal.instance}")
    private String instance;

    @Override
    public void run(ApplicationArguments args) throws Exception {
//        CanalConnector conn = getConnector();
//        while (true){
//            conn.connect();
//            //订阅实例中所有的数据库和表
//            conn.subscribe(".*\\..*");
//            // 回滚到未进行ack的地方
//            conn.rollback();
//            // 获取数据 每次获取一百条改变数据
//            Message message = conn.getWithoutAck(100);
//            long id = message.getId();
//            int size = message.getEntries().size();
//            if (id != -1 && size > 0) {
//                // 数据解析
//                analysis(message.getEntries());
//            }else {
//                Thread.sleep(1000);
//            }
//            // 确认消息
//            conn.ack(message.getId());
//            // 关闭连接
//            conn.disconnect();
//        }

    }

    /**
     * 数据解析
     */
    private void analysis(List<CanalEntry.Entry> entries) {
        for (CanalEntry.Entry entry : entries) {
            // 只解析mysql事务的操作，其他的不解析
            if (entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONBEGIN) {
                continue;
            }
            if (entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONEND) {
                continue;
            }
            // 解析binlog
            CanalEntry.RowChange rowChange = null;
            try {
                rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
            } catch (Exception e) {
                throw new RuntimeException("解析出现异常 data:" + entry.toString(), e);
            }
            if (rowChange != null) {
                // 获取操作类型
                CanalEntry.EventType eventType = rowChange.getEventType();
                // 获取当前操作所属的数据库
                String dbName = entry.getHeader().getSchemaName();
                // 获取当前操作所属的表
                String tableName = entry.getHeader().getTableName();
                if(tableName.equalsIgnoreCase("tb_content")){
                    // 事务提交时间
                    long timestamp = entry.getHeader().getExecuteTime();
                    for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                        dataDetails(rowData.getBeforeColumnsList(), rowData.getAfterColumnsList(), dbName, tableName, eventType, timestamp);
                        logger.info("---------------------------广告数据有变更，更新缓存数据----------------------------------");
                        String categoryId = getColumnValue(eventType, rowData);
                        var result = contentFeign.findByCategory(Long.parseLong(categoryId));
                        var contents = result.getData();
                        stringRedisTemplate.boundValueOps("content_" + categoryId).set(JSON.toJSONString(contents));
                        logger.info(
                                "-----------------------------------------------------------------");

                    }
                }
            }
        }
    }


    /**
     * 解析具体一条Binlog消息的数据
     *
     * @param dbName    当前操作所属数据库名称
     * @param tableName 当前操作所属表名称
     * @param eventType 当前操作类型（新增、修改、删除）
     */
    private static void dataDetails(List<CanalEntry.Column> beforeColumns,
                                    List<CanalEntry.Column> afterColumns,
                                    String dbName,
                                    String tableName,
                                    CanalEntry.EventType eventType,
                                    long timestamp) {

        System.out.println("数据库：" + dbName);
        System.out.println("表名：" + tableName);
        System.out.println("操作类型:" + eventType);
        if (CanalEntry.EventType.INSERT.equals(eventType)) {
            System.out.println("新增数据：");
            printColumn(afterColumns);
        } else if (CanalEntry.EventType.DELETE.equals(eventType)) {
            System.out.println("删除数据：");
            printColumn(beforeColumns);
        } else {
            System.out.println("更新数据：更新前数据--");
            printColumn(beforeColumns);
            System.out.println("更新数据：更新后数据--");
            printColumn(afterColumns);
        }
        System.out.println("操作时间：" + timestamp);
    }

    private static void printColumn(List<CanalEntry.Column> columns) {
        for (CanalEntry.Column column : columns) {
            System.out.println(column.getName() + " : " + column.getValue() + "    update=" + column.getUpdated());
        }
    }

    public CanalConnector getConnector(){
        return CanalConnectors.newSingleConnector(new InetSocketAddress(host,port),instance,"","");
    }

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

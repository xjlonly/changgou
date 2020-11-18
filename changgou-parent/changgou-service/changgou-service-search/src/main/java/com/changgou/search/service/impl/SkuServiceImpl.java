package com.changgou.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.changgou.goods.feign.SkuFeign;
import com.changgou.goods.pojo.Sku;
import com.changgou.search.dao.SkuEsMapper;
import com.changgou.search.pojo.SkuInfo;
import com.changgou.search.service.SkuService;
import com.netflix.discovery.converters.Auto;
import entity.Result;
import io.netty.util.internal.StringUtil;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SkuServiceImpl implements SkuService {

    @Autowired
    private   SkuEsMapper skuEsMapper;
    @Autowired
    private SkuFeign skuFeign;


    @Override
    public void importData() {
        Result<List<Sku>> skuResult = skuFeign.findAll();

        List<SkuInfo> skuInfoList = JSON.parseArray(JSON.toJSONString(skuResult.getData()), SkuInfo.class);


        for (SkuInfo skuInfo : skuInfoList){
            //es动态创建域
            Map<String, Object> specMap = JSON.parseObject(skuInfo.getSpec());
            skuInfo.setSpecMap(specMap);
        }

        skuEsMapper.saveAll(skuInfoList);
    }

    /*
     * ElasticSearchTemplate 实现数据搜索
     * */

    @Autowired
    private   ElasticsearchTemplate elasticsearchTemplate;

    @Override
    public Map<String, Object> search(Map<String, String> map) {
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        if(map != null && map.size() > 0){
            String keyword = map.get("keywords");
            if(!StringUtil.isNullOrEmpty(keyword)){
                nativeSearchQueryBuilder.withQuery(QueryBuilders.matchQuery("name", keyword));
                nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuCategorygroup").field("categoryName").size(50));
            }
        }
        AggregatedPage<SkuInfo> aggregatedPage = elasticsearchTemplate.queryForPage(nativeSearchQueryBuilder.build(), SkuInfo.class);
        //获取分组结果
        StringTerms stringTerms =  (StringTerms) aggregatedPage.getAggregation("skuCategorygroup");

        //封装一个Map存储所有数据 并返回
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("rows", aggregatedPage.getContent());
        resultMap.put("total", aggregatedPage.getTotalElements());
        resultMap.put("totalPages", aggregatedPage.getTotalPages());
        return resultMap;
    }


}

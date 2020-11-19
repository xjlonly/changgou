package com.changgou.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.changgou.goods.feign.SkuFeign;
import com.changgou.goods.pojo.Sku;
import com.changgou.search.dao.SkuEsMapper;
import com.changgou.search.pojo.SkuInfo;
import com.changgou.search.service.SkuService;
import entity.Result;
import io.netty.util.internal.StringUtil;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.*;

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

    /*
    * 构建条件查询器
    * */
    private NativeSearchQueryBuilder buildBaseQuery(Map<String,String> map){
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();


        if(map != null && map.size() > 0){
            String keyword = map.get("keywords");
            if(!StringUtil.isNullOrEmpty(keyword)){
                //以名称为条件查询name分词后结果集
                nativeSearchQueryBuilder.withQuery(QueryBuilders.matchQuery("name", keyword));
            }
        }
        //aggregationBuilder 聚合搜索 按列名进行分组聚合
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuCategoryGroup").field("categoryName").size(50));
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuBrandGroup").field("brandName").size(50));
        //规格数据分组查询 keywrod 不进行分词
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuSpecGroup").field("spec.keyword").size(10000));


        //filter 过滤搜索

        return nativeSearchQueryBuilder;
    }
    @Override
    public Map<String, Object> search(Map<String, String> map) {
        NativeSearchQueryBuilder nativeSearchQueryBuilder = buildBaseQuery(map);
        return searchList(nativeSearchQueryBuilder);
    }

    private Map<String, Object> searchList(NativeSearchQueryBuilder nativeSearchQueryBuilder) {
        AggregatedPage<SkuInfo> aggregatedPage = elasticsearchTemplate.queryForPage(nativeSearchQueryBuilder.build(), SkuInfo.class);
        //获取分组结果
        StringTerms stringTerms =  (StringTerms) aggregatedPage.getAggregation("skuCategoryGroup");
        List<String> categoryList = getStringsGroupList(stringTerms);

        List<String> brandList = getStringsGroupList((StringTerms)aggregatedPage.getAggregation("skuBrandGroup"));

        Map<String, Set<String>> specList = getSpecGroupList((StringTerms) aggregatedPage.getAggregation("skuSpecGroup"));

        //封装一个Map存储所有数据 并返回
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("categoryList", categoryList);
        resultMap.put("brandList",brandList);
        resultMap.put("specList",specList);
        resultMap.put("rows", aggregatedPage.getContent());
        resultMap.put("total", aggregatedPage.getTotalElements());
        resultMap.put("totalPages", aggregatedPage.getTotalPages());
        return resultMap;
    }


    /**
     * 获取分类及品牌等列表数据
     *
     * @param stringTerms
     * @return
     */
    private List<String> getStringsGroupList(StringTerms stringTerms) {
        List<String> groupList = new ArrayList<>();
        if (stringTerms != null) {
            for (StringTerms.Bucket bucket : stringTerms.getBuckets()) {
                String keyAsString = bucket.getKeyAsString();//分组的值
                groupList.add(keyAsString);
            }
        }
        return groupList;
    }

    /**
     * 获取规格列表数据
     *
     * @param stringTerms
     * @return
     */
    private Map<String,Set<String>> getSpecGroupList(StringTerms stringTerms){
        Map<String,Set<String>>  specMap = new HashMap<>();
        Set<String> specSet = new HashSet<>();
        if(stringTerms != null){
            for(StringTerms.Bucket bucket : stringTerms.getBuckets()){
                specSet.add(bucket.getKeyAsString());
            }
        }
        for(String item : specSet){
            Map<String,String> tempMap = JSON.parseObject(item, Map.class);
            for(Map.Entry<String, String> entry : tempMap.entrySet()){
                String key = entry.getKey();
                String value = entry.getValue();
                var specValues =  specMap.get(key);
                if(specValues == null){
                    specValues = new HashSet<>();
                }
                specValues.add(value);
                specMap.put(key, specValues);
            }
        }
        return specMap;
    }

}

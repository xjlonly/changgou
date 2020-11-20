package com.changgou.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.changgou.goods.feign.SkuFeign;
import com.changgou.goods.pojo.Sku;
import com.changgou.search.dao.SkuEsMapper;
import com.changgou.search.pojo.SkuInfo;
import com.changgou.search.service.SkuService;
import entity.Result;
import io.netty.util.internal.StringUtil;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

        //过滤查询
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();

        //高亮
        nativeSearchQueryBuilder.withHighlightFields(buildField());

        boolean hasSpec = false;
        if(map != null && map.size() > 0){
            String keyword = map.get("keywords");
            if(!StringUtil.isNullOrEmpty(keyword)){
                //设置主关键字查询
                nativeSearchQueryBuilder.withQuery(QueryBuilders.multiMatchQuery(keyword,"name","brandName","categoryName"));
            }
            for(Map.Entry<String,String> entry : map.entrySet()){
                String key = entry.getKey();
                if(key.startsWith("spec_")){
                    hasSpec =true;
                    String value = entry.getValue();
                    boolQueryBuilder.filter(QueryBuilders.termQuery("specMap." +  key.replace("spec_","") + ".keyword",value));
                }
            }
        }

        //当用户选择了分类。将分类作为搜索条件，则不需要对分类进行分组搜索。因为分组搜索的数据是用于显示分类搜索条件
        //aggregationBuilder 聚合搜索 按列名进行分组聚合
        if(map != null && map.get("category") != null){
            boolQueryBuilder.filter(QueryBuilders.termQuery("categoryName",map.get("category")));
        }else {
            nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuCategoryGroup").field("categoryName").size(10000));
        }

        if(map != null && map.get("brand") != null){
            boolQueryBuilder.filter(QueryBuilders.termQuery("brandName",map.get("brand")));
        }else{
            nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuBrandGroup").field("brandName").size(10000));
        }

        //规格数据分组查询 keywrod 不进行分词
        if(!hasSpec){
            nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuSpecGroup").field("spec.keyword").size(10000));
        }
        nativeSearchQueryBuilder.withFilter(boolQueryBuilder);

        //filter 价格过滤搜索
        String price = map.get("price");
        if(!StringUtil.isNullOrEmpty(price)){
            price = price.replace("元", "").replace("以上","");
            String[] prices = price.split("-");
            if(prices.length > 0){
                boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").gt(Integer.parseInt(prices[0])));
                if(prices.length == 2){
                    boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").lte(Integer.parseInt(prices[1])));
                }
            }
        }


        //分页
        int pageNum = covertPage(map);
        int size =30;
        nativeSearchQueryBuilder.withPageable(PageRequest.of(pageNum - 1,size));

        //排序
        String sortField = map.get("sortField");
        String sortRule = map.get("Rule");
        if(!StringUtils.isEmpty(sortField) && !StringUtils.isEmpty(sortRule)){
            nativeSearchQueryBuilder.withSort(new FieldSortBuilder(sortField).order(sortRule.toUpperCase().equals("DESC")  ? SortOrder.DESC : SortOrder.ASC));
        }

        return nativeSearchQueryBuilder;
    }

    private HighlightBuilder.Field buildField(){
        //高亮搜索
        HighlightBuilder.Field field = new HighlightBuilder.Field("name");//指定高亮域

        //前缀 <em style="color:red">
        field.preTags(" <em style=\"color:red\">");
        field.postTags("</em>");
        //碎片长度 关键词数据长度
        field.fragmentSize(100);
        return field;
    }

    private int covertPage(Map<String, String> searchMap){
        if(searchMap != null){
            String pageNum = searchMap.get("pageNum");
            if(!StringUtils.isEmpty(pageNum)){
                try {
                    return Integer.parseInt(pageNum);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        return 1;
    }


    @Override
    public Map<String, Object> search(Map<String, String> map) {
        //封装一个Map存储所有数据 并返回
        Map<String, Object> resultMap = new HashMap<>();

        NativeSearchQueryBuilder nativeSearchQueryBuilder = buildBaseQuery(map);
        //AggregatedPage<SkuInfo> aggregatedPage = elasticsearchTemplate.queryForPage(nativeSearchQueryBuilder.build(), SkuInfo.class);
        //SearchResultMapper 搜索之后的结果集映射 包含高亮数据时使用此方法
        AggregatedPage<SkuInfo> aggregatedPage = elasticsearchTemplate.queryForPage(nativeSearchQueryBuilder.build(), SkuInfo.class, new SearchResultMapperImpl());

        //获取分组结果
        if(map.get("category") == null){
            List<String> categoryList = getStringsGroupList((StringTerms) aggregatedPage.getAggregation("skuCategoryGroup"));
            resultMap.put("categoryList", categoryList);
        }

        if(map.get("brand") == null){
            List<String> brandList = getStringsGroupList((StringTerms)aggregatedPage.getAggregation("skuBrandGroup"));
            resultMap.put("brandList",brandList);
        }

        Map<String, Set<String>> specList = getSpecGroupList((StringTerms) aggregatedPage.getAggregation("skuSpecGroup"));
        if(specList.size() > 0){
            resultMap.put("specList",specList);
        }

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

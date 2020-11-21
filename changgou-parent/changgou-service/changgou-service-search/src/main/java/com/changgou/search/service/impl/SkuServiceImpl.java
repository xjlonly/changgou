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
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
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

        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuCategoryGroup").field("categoryName").size(10000));
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuBrandGroup").field("brandName").size(10000));
        //规格数据分组查询 keywrod 不进行分词
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuSpecGroup").field("spec.keyword").size(10000));

        if(map != null && map.size() > 0){
            String keyword = map.get("keywords");
            if(!StringUtil.isNullOrEmpty(keyword)){
                //设置主关键字查询
                boolQueryBuilder.filter(QueryBuilders.multiMatchQuery(keyword,"name","brandName","categoryName"));
            }

            //当用户选择了分类。将分类作为搜索条件，则不需要对分类进行分组搜索。因为分组搜索的数据是用于显示分类搜索条件
            //aggregationBuilder 聚合搜索 按列名进行分组聚合
            if(map.get("category") != null){
                boolQueryBuilder.filter(QueryBuilders.termQuery("categoryName",map.get("category")));
            }

            if(map.get("brand") != null){
                boolQueryBuilder.filter(QueryBuilders.termQuery("brandName",map.get("brand")));
            }

            for(Map.Entry<String,String> entry : map.entrySet()){
                String key = entry.getKey();
                if(key.startsWith("spec_")){
                    String value = entry.getValue();
                    boolQueryBuilder.filter(QueryBuilders.termQuery("specMap." +  key.replace("spec_","") + ".keyword",value));
                }
            }

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

            //排序
            String sortField = map.get("sortField");
            String sortRule = map.get("Rule");
            if(!StringUtils.isEmpty(sortField) && !StringUtils.isEmpty(sortRule)){
                nativeSearchQueryBuilder.withSort(new FieldSortBuilder(sortField).order(sortRule.toUpperCase().equals("DESC")  ? SortOrder.DESC : SortOrder.ASC));
            }
        }

        nativeSearchQueryBuilder.withQuery(boolQueryBuilder);

        //分页
        int pageNum = covertPage(map);
        int size =30;
        nativeSearchQueryBuilder.withPageable(PageRequest.of(pageNum - 1,size));



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



    //@Override
    public Map<String, Object> search1(Map<String, String> searchMap) {

        //1.获取关键字的值
        String keywords = searchMap.get("keywords");

        if (StringUtils.isEmpty(keywords)) {
            keywords = "华为";//赋值给一个默认的值
        }
        //2.创建查询对象 的构建对象
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();

        //3.设置查询的条件

        //设置分组条件  商品分类
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuCategorygroup").field("categoryName").size(10000));

        //设置分组条件  商品品牌
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuBrandgroup").field("brandName").size(10000));

        //设置分组条件  商品的规格
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuSpecgroup").field("spec.keyword").size(500000));


        //设置高亮条件
        nativeSearchQueryBuilder.withHighlightFields(new HighlightBuilder.Field("name"));
        nativeSearchQueryBuilder.withHighlightBuilder(new HighlightBuilder().preTags("<em style=\"color:red\">").postTags("</em>"));

        //设置主关键字查询
        nativeSearchQueryBuilder.withQuery(QueryBuilders.multiMatchQuery(keywords,"name","brandName","categoryName"));


        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();


        if (!StringUtils.isEmpty(searchMap.get("brand"))) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("brandName", searchMap.get("brand")));
        }

        if (!StringUtils.isEmpty(searchMap.get("category"))) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("categoryName", searchMap.get("category")));
        }

        //规格过滤查询
        if (searchMap != null) {
            for (String key : searchMap.keySet()) {
                if (key.startsWith("spec_")) {
                    boolQueryBuilder.filter(QueryBuilders.termQuery("specMap." + key.substring(5) + ".keyword", searchMap.get(key)));
                }
            }
        }

        //价格过滤查询
        String price = searchMap.get("price");
        if (!StringUtils.isEmpty(price)) {
            String[] split = price.split("-");
            if (!split[1].equalsIgnoreCase("*")) {
                boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").from(split[0], true).to(split[1], true));
            } else {
                boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").gte(split[0]));
            }
        }


        //构建过滤查询
        nativeSearchQueryBuilder.withFilter(boolQueryBuilder);

        //构建分页查询
        Integer pageNum = 1;
        if (!StringUtils.isEmpty(searchMap.get("pageNum"))) {
            try {
                pageNum = Integer.valueOf(searchMap.get("pageNum"));
            } catch (NumberFormatException e) {
                e.printStackTrace();
                pageNum=1;
            }
        }
        Integer pageSize = 3;
        nativeSearchQueryBuilder.withPageable(PageRequest.of(pageNum - 1, pageSize));


        //构建排序查询
        String sortRule = searchMap.get("sortRule");
        String sortField = searchMap.get("sortField");
        if (!StringUtils.isEmpty(sortRule) && !StringUtils.isEmpty(sortField)) {
            nativeSearchQueryBuilder.withSort(SortBuilders.fieldSort(sortField).order(sortRule.equals("DESC") ? SortOrder.DESC : SortOrder.ASC));
        }


        //4.构建查询对象
        NativeSearchQuery query = nativeSearchQueryBuilder.build();

        //5.执行查询
        AggregatedPage<SkuInfo> skuPage = elasticsearchTemplate.queryForPage(query, SkuInfo.class, new SearchResultMapperImpl());

        //获取分组结果  商品分类
        StringTerms stringTermsCategory = (StringTerms) skuPage.getAggregation("skuCategorygroup");
        //获取分组结果  商品品牌
        StringTerms stringTermsBrand = (StringTerms) skuPage.getAggregation("skuBrandgroup");
        //获取分组结果  商品规格数据
        StringTerms stringTermsSpec = (StringTerms) skuPage.getAggregation("skuSpecgroup");

        List<String> categoryList = getStringsCategoryList(stringTermsCategory);

        List<String> brandList = getStringsBrandList(stringTermsBrand);

        Map<String, Set<String>> specMap = getStringSetMap(stringTermsSpec);


        //6.返回结果
        Map resultMap = new HashMap<>();

        resultMap.put("specMap", specMap);
        resultMap.put("categoryList", categoryList);
        resultMap.put("brandList", brandList);
        resultMap.put("rows", skuPage.getContent());
        resultMap.put("total", skuPage.getTotalElements());
        resultMap.put("totalPages", skuPage.getTotalPages());

        return resultMap;
    }

    /**
     * 获取品牌列表
     *
     * @param stringTermsBrand
     * @return
     */
    private List<String> getStringsBrandList(StringTerms stringTermsBrand) {
        List<String> brandList = new ArrayList<>();
        if (stringTermsBrand != null) {
            for (StringTerms.Bucket bucket : stringTermsBrand.getBuckets()) {
                brandList.add(bucket.getKeyAsString());
            }
        }
        return brandList;
    }

    /**
     * 获取分类列表数据
     *
     * @param stringTerms
     * @return
     */
    private List<String> getStringsCategoryList(StringTerms stringTerms) {
        List<String> categoryList = new ArrayList<>();
        if (stringTerms != null) {
            for (StringTerms.Bucket bucket : stringTerms.getBuckets()) {
                String keyAsString = bucket.getKeyAsString();//分组的值
                categoryList.add(keyAsString);
            }
        }
        return categoryList;
    }

    /**
     * 获取规格列表数据
     *
     * @param stringTermsSpec
     * @return
     */
    private Map<String, Set<String>> getStringSetMap(StringTerms stringTermsSpec) {
        Map<String, Set<String>> specMap = new HashMap<String, Set<String>>();

        Set<String> specList = new HashSet<>();

        if (stringTermsSpec != null) {
            for (StringTerms.Bucket bucket : stringTermsSpec.getBuckets()) {
                specList.add(bucket.getKeyAsString());
            }
        }

        for (String specjson : specList) {
            Map<String, String> map = JSON.parseObject(specjson, Map.class);
            for (Map.Entry<String, String> entry : map.entrySet()) {//
                String key = entry.getKey();        //规格名字
                String value = entry.getValue();    //规格选项值
                //获取当前规格名字对应的规格数据
                Set<String> specValues = specMap.get(key);
                if (specValues == null) {
                    specValues = new HashSet<String>();
                }
                //将当前规格加入到集合中
                specValues.add(value);
                //将数据存入到specMap中
                specMap.put(key, specValues);
            }
        }
        return specMap;
    }
}

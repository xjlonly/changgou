package com.changgou.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.changgou.search.pojo.SkuInfo;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;

import java.util.ArrayList;
import java.util.List;

public class SearchResultMapperImpl  implements SearchResultMapper {
    @Override
    public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
        List<T> list  = new ArrayList<>();
        //执行查询 获取所有数据-》结果集【非高亮数据|高亮数据】
        for(SearchHit hit : response.getHits()){
            //分析结果集数据，获取非高亮数据和高亮数据 然后将非高亮数据中指定的域替换成高亮数据
            SkuInfo skuInfo = JSON.parseObject(hit.getSourceAsString(),SkuInfo.class);
            HighlightField highlightField = hit.getHighlightFields().get("name");
            if(highlightField != null && highlightField.getFragments() != null){
                Text[] fragments = highlightField.getFragments();
                StringBuffer buffer = new StringBuffer();
                for(Text text : fragments){
                    buffer.append(text);
                }
                skuInfo.setName(buffer.toString());
            }
            list.add((T)skuInfo);
        }
        return new AggregatedPageImpl<T>(list, pageable,response.getHits().getTotalHits(),response.getAggregations(),response.getScrollId());
    }
}

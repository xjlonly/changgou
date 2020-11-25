package com.changou.item.service.Impl;

import com.alibaba.fastjson.JSON;
import com.changgou.goods.feign.CategoryFeign;
import com.changgou.goods.feign.SkuFeign;
import com.changgou.goods.feign.SpuFeign;
import com.changgou.goods.feign.TemplateFeign;
import com.changgou.goods.pojo.Sku;
import com.changgou.goods.pojo.Spu;
import com.changou.item.service.PageService;
import entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import springfox.documentation.spring.web.json.Json;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PageServiceImpl implements PageService {

    @Autowired
    private SkuFeign skuFeign;

    @Autowired
    private SpuFeign spuFeign;

    @Autowired
    private CategoryFeign categoryFeign;


    @Autowired
    private TemplateEngine templateEngine;

    @Value("${pagepath}")
    private String pagepath;

    @Override
    public void createPageHtml(Long spuId) {
        Context context = new Context();
        Map<String, Object> dataMap= buildModelData(spuId);
        context.setVariables(dataMap);

        File dir = new File(pagepath);
        if(!dir.exists()){
            dir.mkdir();
        }
        File dest = new File(dir, spuId + ".html");

        try (PrintWriter writer = new PrintWriter(dest,"UTF-8")){
            templateEngine.process("item", context, writer);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private Map<String, Object> buildModelData(long spuId){
        Map<String,Object> dataMap = new HashMap<>();
        Result<Spu> spuResult = spuFeign.findById(spuId);
        Spu spu = spuResult.getData();

        dataMap.put("category1",categoryFeign.findById(spu.getCategory1Id()).getData());
        dataMap.put("category2",categoryFeign.findById(spu.getCategory2Id()).getData());
        dataMap.put("category3",categoryFeign.findById(spu.getCategory3Id()).getData());

        if(spu.getImages() != null){
            dataMap.put("imageList",spu.getImages().split(","));
        }

        dataMap.put("specificationList", JSON.parseObject(spu.getSpecItems(),Map.class));
        dataMap.put("spu",spu);

        Sku skuCondition = new Sku();
        skuCondition.setSpuId(spu.getId());
        Result<List<Sku>> listResult = skuFeign.findList(skuCondition);
        dataMap.put("skuList", listResult.getData());
        return dataMap;

    }


}

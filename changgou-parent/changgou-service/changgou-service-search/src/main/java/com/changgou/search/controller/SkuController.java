package com.changgou.search.controller;

import com.changgou.search.service.SkuService;
import entity.Result;
import entity.StatusCode;
import jdk.net.SocketFlow;
import org.apache.lucene.search.SearcherLifetimeManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/search")
public class SkuController {

    @Autowired
    private   SkuService skuService;

    @GetMapping(value = "/import")
    public Result importData(){
        skuService.importData();
        return new Result(true, StatusCode.OK,"导入成功");
    }

    @GetMapping
    public Map search(@RequestParam(required = false) Map<String,String> queryMap){
        return skuService.search(queryMap);
    }
}

package com.changgou.controller;

import com.changgou.search.feign.SkuFeign;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@RequestMapping(value = "/search")
public class SkuController {

    @Autowired
    private   SkuFeign skuFeign;
    @GetMapping("/list")
    public String search(@RequestParam(required = false) Map<String, String> searchMap, Model model){
        Map map = skuFeign.search(searchMap);
        model.addAttribute("result",map);
        model.addAttribute("searchMap",searchMap);
        return "search";

    }
}

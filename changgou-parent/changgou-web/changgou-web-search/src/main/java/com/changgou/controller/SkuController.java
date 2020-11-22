package com.changgou.controller;

import com.changgou.search.feign.SkuFeign;
import com.changgou.search.pojo.SkuInfo;
import entity.Page;
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
        handlerSearchMap(searchMap);
        Map map = skuFeign.search(searchMap);
        model.addAttribute("result",map);
        model.addAttribute("searchMap",searchMap);

        Page<SkuInfo> skuInfoPage =  new Page<SkuInfo>(Long.parseLong(map.get("total").toString()),
                Integer.parseInt(map.get("pageNumber").toString()) + 1,
                Integer.parseInt(map.get("pageSize").toString()));

        model.addAttribute("pageInfo", skuInfoPage);
        String[] url = url(searchMap);
        model.addAttribute("url", url[0]);
        model.addAttribute("sorturl", url[1]);
        return "search";

    }

    public void handlerSearchMap(Map<String, String> searchMap){
        if (searchMap != null) {
            for(Map.Entry<String,String> entry : searchMap.entrySet()){
                if(entry.getKey().startsWith("spec_")){
                    entry.setValue(entry.getValue().replace("+","%2B"));
                }
            }
        }
    }
    private String[] url(Map<String,String> searchMap){
        String url = "/search/list";
        String sorturl = "/search/list";
        if(searchMap != null && searchMap.size() > 0){
            url += "?";
            sorturl+="?";
            for (Map.Entry<String,String> entry : searchMap.entrySet()){
                String key = entry.getKey();
                if(key.equalsIgnoreCase("pageNum")){
                    continue;
                }
                String value = entry.getValue();
                url += entry.getKey() + "=" + entry.getValue()+ "&";
                if(key.equalsIgnoreCase("sortField") || key.equalsIgnoreCase("sortRule")){
                    continue;
                }
                sorturl += entry.getKey() + "=" + entry.getValue()+ "&";

            }

            url = url.substring(0, url.length() -1);
            sorturl = sorturl.substring(0, sorturl.length() -1);
        }
        return  new String[]{url,sorturl};

    }
}

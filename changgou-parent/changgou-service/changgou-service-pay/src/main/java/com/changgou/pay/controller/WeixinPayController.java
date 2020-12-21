package com.changgou.pay.controller;

import com.alibaba.fastjson.JSON;
import com.changgou.pay.service.WeixinPayService;
import com.github.wxpay.sdk.WXPayUtil;
import entity.Result;
import entity.StatusCode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ：xjlonly
 * @date ：Created in 2020/12/9 16:26
 * @description：生成微信支付二维码控制器
 * @modified By：
 * @version: $
 */
@RestController
@CrossOrigin
@RequestMapping("/weixin/pay")
public class WeixinPayController {
    @Value("${mq.pay.exchange.order}")
    private String exchange;
    @Value("${mq.pay.queue.order}")
    private String queue;
    @Value("${mq.pay.routing.key}")
    private String routing;


    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private WeixinPayService weixinPayService;

    /**
     * @author: xjlonly
     * @description: TODO
     * @date: 2020/12/9 16:31
     * @Param: null
     * @return
     */
    @RequestMapping(value = "/create/native")
    public Result createNative(@RequestParam Map<String,String> parameterMap){
        Map<String,String> resultMap = weixinPayService.createNative(parameterMap);
        return new Result(true, StatusCode.OK,"创建二维码预付订单成功！",resultMap);
    }


    /***
     * 查询支付状态
     * @param outtradeno
     * @return
     */
    @GetMapping(value = "/status/query")
    public Result<Map<String,String>> queryStatus(String outtradeno){
        Map<String,String> resultMap = weixinPayService.queryPayStatus(outtradeno);

        //测试使用 消息写入队列
        //将消息发送给RabbitMQ
        rabbitTemplate.convertAndSend(exchange,routing, JSON.toJSONString(resultMap));

        return new Result(true,StatusCode.OK,"查询状态成功！",resultMap);
    }


    /***
     * 支付回调 消息写入队列
     * @param request
     * @return
     */
    @RequestMapping(value = "/notify/url")
    public String notifyUrl(HttpServletRequest request){
        InputStream inStream;
        try {
            //读取支付回调数据
            inStream = request.getInputStream();
            ByteArrayOutputStream outSteam = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = inStream.read(buffer)) != -1) {
                outSteam.write(buffer, 0, len);
            }
            outSteam.close();
            inStream.close();
            // 将支付回调数据转换成xml字符串
            String result = new String(outSteam.toByteArray(), "utf-8");
            //将xml字符串转换成Map结构
            Map<String, String> map = WXPayUtil.xmlToMap(result);

            String attach = map.get("attach");
            Map<String,String> attachMap = JSON.parseObject(attach,Map.class);
            //将消息发送给RabbitMQ
            rabbitTemplate.convertAndSend(attachMap.get("exchange"),attachMap.get("routingkey"),
                    JSON.toJSONString(map));

            //响应数据设置
            Map respMap = new HashMap();
            respMap.put("return_code","SUCCESS");
            respMap.put("return_msg","OK");
            return WXPayUtil.mapToXml(respMap);
        } catch (Exception e) {
            e.printStackTrace();
            //记录错误日志
        }
        return null;
    }
}

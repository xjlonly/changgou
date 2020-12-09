package com.changgou.pay.service;

import java.util.Map;

/**
 * @author ：xjlonly
 * @date ：Created in 2020/12/9 16:15
 * @description：微信支付接口
 * @modified By：
 * @version: 1.0$
 */
public interface WeixinPayService {

    /*****
     * 创建二维码
     * @param out_trade_no : 客户端自定义订单编号
     * @param total_fee    : 交易金额,单位：分
     * @return
     */
    public Map createNative(String out_trade_no, String total_fee);


    /***
     * 查询订单状态
     * @param out_trade_no : 客户端自定义订单编号
     * @return
     */
    public Map queryPayStatus(String out_trade_no);
}

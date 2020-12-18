package com.changgou.seckill.controller;

import com.changgou.seckill.pojo.SeckillOrder;
import com.changgou.seckill.service.SeckillOrderService;
import com.github.pagehelper.PageInfo;
import entity.Result;
import entity.SeckillStatus;
import entity.StatusCode;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;


@Api(value = "SeckillOrderController")
@RestController
@RequestMapping("/seckillOrder")
@CrossOrigin
public class SeckillOrderController {

    @Autowired
    private SeckillOrderService seckillOrderService;


    /****
     * 查询抢购
     * @return
     */
    @RequestMapping(value = "/query")
    public Result queryStatus(){
        //获取用户名
        //String username = tokenDcode.getUserInfo().get("username");
        String username = "test";
        //根据用户名查询用户抢购状态
        SeckillStatus seckillStatus = seckillOrderService.queryStatus(username);

        if(seckillStatus != null){
            return new Result(true,seckillStatus.getStatus(),"抢购状态", seckillStatus);
        }
        //NOTFOUNDERROR =20006,没有对应的抢购数据
        return new Result(false,StatusCode.NOTFOUNDERROR,"没有抢购信息");
    }



    /****
     * URL:/seckill/order/add
     * 添加订单
     * 调用Service增加订单
     * 匿名访问：anonymousUser
     * @param time
     * @param id
     */
    @RequestMapping(value = "/add")
    public Result add(String time, Long id){
        try {
            //用户登录名
            //String username = TokenDcode.getUserInfo().get("username");
            String username = "test";
            //调用Service增加订单
            Boolean bo = seckillOrderService.add(id, time, username);

            if(bo){
                //抢单成功
                return new Result(true,StatusCode.OK,"正在排队！");
            }
        } catch (Exception e) {
            e.printStackTrace();
            if(Objects.equals(e.getMessage(), "20005")){
                return new Result(true,StatusCode.REPERROR,"重复下单");
            }
        }
        return new Result(true,StatusCode.ERROR,"服务器繁忙，请稍后再试");
    }


}

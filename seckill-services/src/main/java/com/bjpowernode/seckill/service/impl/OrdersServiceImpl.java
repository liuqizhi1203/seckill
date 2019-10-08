package com.bjpowernode.seckill.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.bjpowernode.seck.constants.Constants;
import com.bjpowernode.seck.model.Orders;
import com.bjpowernode.seck.model.OrdersExample;
import com.bjpowernode.seck.model.ProductExample;
import com.bjpowernode.seckill.mapper.OrdersMapper;
import com.bjpowernode.seckill.service.OrdersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * ClassName:OrdersServiceImpl
 * Package:com.bjpowernode.seckill.service.impl
 *
 * @Description: 描述
 * @Author: Mr.Liu
 * @Date: 2019/7/30 17:42
 */
@Service
public class OrdersServiceImpl implements OrdersService {
    @Autowired
    private OrdersMapper ordersMapper;
    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @Override
    public int saveOrders(Orders orders) {
        //1.接收web发过来的消息 然后把order信息插入到数据库中
        int result = ordersMapper.insert(orders);
        //判断是否成功
        if (result > 0) {
            //从限流名单中剔除,腾出位置
            redisTemplate.opsForList().rightPop(Constants.REDIS_LIMIT + orders.getGoodsid());
            //在redis中写一个service操作成功的标记
            Map retMap = new HashMap();
            retMap.put("errorCode", Constants.ONE);
            retMap.put("errorMessage", "秒杀成功!");
            retMap.put("data", orders);
            String retJson = JSONObject.toJSONString(retMap);
            redisTemplate.opsForValue().set(Constants.REDIS_RESULT + orders.getGoodsid() + ":" + orders.getUid(), retJson);
            return result;
        } else {
            //插入订单失败处理
            throw new RuntimeException("下单异常");
        }

    }

    @Override
    public void processOrductException(Orders orders) {
        //从限流名单中剔除,腾出位置
        redisTemplate.opsForList().rightPop(Constants.REDIS_LIMIT + orders.getGoodsid());
        //从已经购买商品中剔除
        redisTemplate.delete(Constants.REDIS_BOUGHT + orders.getGoodsid() + ":" + orders.getUid());
        //恢复减少的库存
        redisTemplate.opsForValue().increment(Constants.REDIS_STORE + orders.getGoodsid(), 1);
        //在redis中写一个service失败的标记返回前端
        Map retMap = new HashMap();
        retMap.put("errorCode", Constants.ZERO);
        retMap.put("errorMessage", "秒杀失败!");
        retMap.put("data", orders);
        String retJson = JSONObject.toJSONString(retMap);
        redisTemplate.opsForValue().set(Constants.REDIS_RESULT + orders.getGoodsid() + ":" + orders.getUid(), retJson);
    }

}

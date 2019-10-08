package com.bjpowernode.seckill.listener;


import com.alibaba.fastjson.JSONObject;
import com.bjpowernode.seck.model.Orders;
import com.bjpowernode.seckill.service.OrdersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import javax.jms.Message;
import javax.jms.TextMessage;

@Service
public class SecKillListener {
    @Autowired
    private OrdersService ordersService;

    @JmsListener(destination = "${spring.jms.template.default-destination}")
    public void receiveMsg(Message message){
        try {
            if(message instanceof TextMessage){
                String ordersJSON = ((TextMessage) message).getText();
                System.out.println("接收到异步消息 订单信息为"+ordersJSON);
                Orders orders = JSONObject.parseObject(ordersJSON, Orders.class);
                //调用orderService进行数据库下单操作
                try {
                    ordersService.saveOrders(orders);
                } catch (Exception e) {
                    ordersService.processOrductException(orders);
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}

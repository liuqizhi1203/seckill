package com.bjpowernode.seckill.service;

import com.bjpowernode.seck.model.Orders;

/**
 * ClassName:OrdersService
 * Package:com.bjpowernode.seckill.service
 *
 * @Description: 描述
 * @Author: Mr.Liu
 * @Date: 2019/7/30 17:42
 */

public interface OrdersService {

   int saveOrders(Orders orders);

    void processOrductException(Orders orders);
}

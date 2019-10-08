package com.bjpowernode.seckill.task;

import com.alibaba.fastjson.JSONObject;
import com.bjpowernode.seck.constants.Constants;
import com.bjpowernode.seck.model.Product;
import com.bjpowernode.seckill.mapper.ProductMapper;
import com.sun.glass.events.WheelEvent;
import jdk.nashorn.internal.objects.annotations.Where;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * ClassName:RedisTask
 * Package:com.bjpowernode.seckill.task
 *
 * @Description: 执行定时任务
 * @Author: Mr.Liu
 * @Date: 2019/7/27 20:48
 * @Configuration注解用于定义配置类，可替换XML配置文件，被 注解的类内部包含有一个或者多个被@Bean注解的方法，这些方法将会
 * 被ApplicationContext上下文类进行扫描，并构建对应的bean,
 * 加入到Spring容器之中进行管理。
 */
@Configuration
@EnableScheduling
public class RedisTask {

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /*
     * 每五秒执行一次定时任务,初始化一遍秒杀商品信息
     * 缓存预热
     * */
    @Scheduled(cron = "0/5 * * * * *")
    public void initRedisproduct() {
        System.out.println("缓存预热------------");
        //查询所有秒杀商品的信息
        List<Product> productList = productMapper.queryAllproduct();
        //放入到redis中
        for (Product product : productList) {
            String productJosn = JSONObject.toJSONString(product);
            redisTemplate.opsForValue().set(Constants.REDIS_PRODUCT + product.getId(), productJosn);
            //把商品库存存入redis中,如果redis里面有该值 就不添加 如果没有就添加
            redisTemplate.opsForValue().setIfAbsent(Constants.REDIS_STORE + product.getId(), String.valueOf(product.getStore()));
        }
    }


    //每3秒同步redis中库存数量到数据库
    @Scheduled(cron = "0/3 * * * * *")
    public void synRedisStoreToDB() {


        System.out.println("同步开始----------------");
        //查询redis中库存信息
        Set<String> setKeys = redisTemplate.keys(Constants.REDIS_STORE + "*");


        for (String key : setKeys) {
            //根据Redis的商品库存key，获取商品的库存
            int store = Integer.valueOf(redisTemplate.opsForValue().get(key));
            //获取商品的id   在Redis中存放商品库存的格式  redis:store:id
            int productId = Integer.valueOf(key.split(":")[2]);
            //同步到数据库
            Product product=new Product();
            product.setStore(store);
            product.setId(productId);
            productMapper.updateByPrimaryKeySelective(product);
        }
    }

}

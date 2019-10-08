package com.bjpowernode.seck.constants;

/**
 * ClassName:Constants
 * Package:com.bjpowernode.seck.constants
 *
 * @Description: 常量
 * @Author: Mr.Liu
 * @Date: 2019/7/27 21:49
 */

public class Constants {
    //redis中秒杀商品key前缀
    public static final String REDIS_PRODUCT="redis:product:";
    // 0代表秒杀失败码
    public static final int ZERO = 0;
    //1代表秒杀成功码
    public static final int ONE = 1;
    //redis中秒杀库存数量前缀
    public static final String REDIS_STORE ="redis:store:" ;
    //redis中用户购买记录前缀
    public static final String REDIS_BOUGHT = "redis:bought:";
    //redis中用户队列数量记录前缀
    public static final String REDIS_LIMIT = "redis:limit:";
    //最大队列数量
    public static final Long MAX_LIMIT = 5000L;
    //秒杀处理结果前缀
    public static final String REDIS_RESULT ="redis:result:" ;
}

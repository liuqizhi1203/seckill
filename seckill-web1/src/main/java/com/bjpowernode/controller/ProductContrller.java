package com.bjpowernode.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bjpowernode.seck.constants.Constants;
import com.bjpowernode.seck.model.Orders;
import com.bjpowernode.seck.model.Product;

import com.bjpowernode.seck.model.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;


import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.servlet.http.HttpSession;

import java.util.*;

/**
 * ClassName:productContrller
 * Package:com.bjpowernode.controller
 *
 * @Description: 描述
 * @Author: Mr.Liu
 * @Date: 2019/7/29 17:26
 */
@Controller
public class ProductContrller {

    /* @Autowired
     private ProductService productService;*/
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private JmsTemplate jmsTemplate;

    @GetMapping("/seckill/product")
    public String index(Model model) {
        //在高并发的情况下 用户可能会连续刷页面 如果我们用查询数据库的方式去获取数据 会导致数据库压力特别大
        //用redis把我们的商品数据信息 先缓存--->>缓存预热
        //商品信息有可能变化 如果还用缓存 隔一段时间就要更新缓存里面的信息productService
        Set<String> keys = redisTemplate.keys(Constants.REDIS_PRODUCT + "*");
        List<String> productJsonList = redisTemplate.opsForValue().multiGet(keys);
        List<Product> productList = new ArrayList<>();
        for (String productJson : productJsonList) {
            Product product = JSONObject.parseObject(productJson, Product.class);
            productList.add(product);
        }

        model.addAttribute("productList", productList);
        return "product";
    }

    @GetMapping("/seckill/detail/{id}")
    public String detail(@PathVariable("id") Integer id, Model model, HttpSession session) {
        //根据商品id,从redis中获取商品信息
        String productJson = redisTemplate.opsForValue().get(Constants.REDIS_PRODUCT + id);
        Product product = JSONObject.parseObject(productJson, Product.class);
        User user = new User();
        user.setId(1688);
        user.setName("刘老二");
        session.setAttribute("user", user);

        model.addAttribute("product", product);
        //因为详情页面需要显示秒杀按钮,所以我们将服务器的是时间传递给前端
        model.addAttribute("currentTime", System.currentTimeMillis());
        return "detail";
    }

    @PostMapping("/seckill/random/{productId}")
    @ResponseBody
    public Object random(@PathVariable("productId") Integer productId, Model model) {
        //根据商品的id获取商品
        String productJson = redisTemplate.opsForValue().get(Constants.REDIS_PRODUCT + productId);
        Product product = JSONObject.parseObject(productJson, Product.class);
        //验证秒杀时间是否真正开始
        Long currentTime = System.currentTimeMillis();
        Long startTime = product.getStarttime().getTime();
        Long endTime = product.getEndtime().getTime();
        Map retmap = new HashMap();
        if (currentTime < startTime) {
            //秒杀哈没有开始
            retmap.put("errorCode", Constants.ZERO);
            retmap.put("errorMessage", "秒杀还未开始");

        } else if (currentTime > endTime) {
            retmap.put("errorCode", Constants.ZERO);
            retmap.put("errorMessage", "秒杀已经结束");
        } else {
            retmap.put("errorCode", Constants.ONE);
            retmap.put("data", product.getRandomname());
        }
        return retmap;
    }

    @PostMapping("/seckill/gooods/{frontRandom}/{productId}")
    @ResponseBody
    public Object seckill(@PathVariable("frontRandom") String frontRandom,
                                       @PathVariable("productId") Integer productId,
                                       HttpSession session) {

        Map<String, Object> retMap = new HashMap<>();
        //判断random与数据库中是否一致
        String productJson = redisTemplate.opsForValue().get(Constants.REDIS_PRODUCT + productId);
        Product product = JSONObject.parseObject(productJson, Product.class);
        String productRandomname = product.getRandomname();
        if (!frontRandom.equals(productRandomname)) {
            retMap.put("errorCode", Constants.ZERO);
            retMap.put("errorMessage", "参数不对");

            return retMap;
        }
        //判断用户是否登录
        User user = (User) session.getAttribute("user");
        Integer userId = null;
        if (user != null) {
            userId = user.getId();

        }
        //判断是否还有库存
        String redisStore = redisTemplate.opsForValue().get(Constants.REDIS_STORE + productId);
        int totalStore = StringUtils.isEmpty(redisStore) ? 0 : Integer.parseInt(redisStore);
        if (totalStore <= 0) {
            //商品卖完了
            retMap.put("errorCode", Constants.ZERO);
            retMap.put("errorMessage", "来晚了,都卖光咯");

            return retMap;
        }
        //判断用户是否已经秒杀过该商品--去redis中查看是否有购买记录,
        //定义用户购买的记录的标记:redis:bought:productId:uid
        String bought = redisTemplate.opsForValue().get(Constants.REDIS_BOUGHT + productId + ":" + userId);
        if (StringUtils.isEmpty(bought)) {
            //说明用户已经买过了
            retMap.put("errorCode", Constants.ZERO);
            retMap.put("errorMessage", "本产品只允许购买一次!");

            return retMap;


        }
        //服务器的限流
        //在进入请求某个单品限流队列的时候看size是否超过我们限流的配置
        Long redisLimit = redisTemplate.opsForValue().size(Constants.REDIS_LIMIT + productId);
        //如果超过返回信息给前端
        if (redisLimit > Constants.MAX_LIMIT) {
            //队列数量超过最大量
            retMap.put("errorCode", Constants.ZERO);
            retMap.put("errorMessage", "人数太多,稍后再来吧");

            return retMap;


        }
        //先向Redis的限流List中放一条数据  返回放完数据之后List的长度
        redisTemplate.opsForList().leftPush(Constants.REDIS_LIMIT + productId, String.valueOf(userId));
        //减少库存
        Long leafStore = redisTemplate.opsForValue().decrement(Constants.REDIS_STORE + productId, 1);

        if (leafStore>=0){
            //说明可以卖  然后往redis当中添加该用户购买的标记 便于第三步进行判断
            //购买过的商品id+用户的id---用户id
            redisTemplate.opsForValue().set(Constants.REDIS_BOUGHT+productId+":"+userId,String.valueOf(userId));
            //创建一个订单

            Orders orders = new Orders();
            orders.setBuynum(1);
            orders.setBuyprice(product.getPrice());
            orders.setCreatetime(new Date());
            orders.setGoodsid(productId);
            orders.setOrdermoney(product.getPrice());
            orders.setStatus(1);
            orders.setUid(userId);
            String ordersJSON = JSONObject.toJSONString(orders);
            //通过mq发送消息,采用消息队列进行下单  ----流量削峰
            jmsTemplate.send(new MessageCreator() {
                @Override
                public Message createMessage(Session session) throws JMSException {
                    TextMessage orderMsg = session.createTextMessage(ordersJSON);
                    return orderMsg;
                }
            });
            //返回一个信息给前端
            retMap.put("errorCode", Constants.ONE);
            retMap.put("errorMessage", "秒杀成功,请稍后系统正在处理!");

            return retMap;


        }else {
            //说明已经卖光啦
            //返回一个信息给前端
            //恢复库存 -1 变为0 恢复不恢复不是很影响功能 但影响我看数据
            redisTemplate.opsForValue().increment(Constants.REDIS_STORE + productId, 1);
            retMap.put("errorCode", Constants.ZERO);
            retMap.put("errorMessage", "已经卖光啦!");

            return retMap;

        }

    }
        @PostMapping("/seckill/querySecKillResult/{productId}")
        @ResponseBody
        public Object secKillResult(@PathVariable("productId")Integer productId,
                                    HttpSession session){
            User user = (User) session.getAttribute("user");
            //取出redis中订单处理标记
            String ret2FrontObjectJSON=redisTemplate.opsForValue().get(Constants.REDIS_RESULT+productId+":"+user.getId());
            Map retMap=new HashMap();
            if (ret2FrontObjectJSON!=null) {
               retMap = JSONObject.parseObject(ret2FrontObjectJSON, Map.class);
                for (Object obj : retMap.keySet()) {
                    System.out.println("key为：" + obj + "值为：" + retMap.get(obj));
                }
            }

            return retMap;
        }
}


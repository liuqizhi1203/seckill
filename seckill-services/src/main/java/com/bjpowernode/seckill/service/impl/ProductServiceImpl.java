package com.bjpowernode.seckill.service.impl;

import com.bjpowernode.seck.model.Product;
import com.bjpowernode.seckill.mapper.ProductMapper;
import com.bjpowernode.seckill.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ClassName:ProductServiceImpl
 * Package:com.bjpowernode.seckill.service.impl
 *
 * @Description: 描述
 * @Author: Mr.Liu
 * @Date: 2019/7/27 19:52
 */
@Service
public class ProductServiceImpl implements ProductService {
    @Autowired
    private ProductMapper productMapper;


}

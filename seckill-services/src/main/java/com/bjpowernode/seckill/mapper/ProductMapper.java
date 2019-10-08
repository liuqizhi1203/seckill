package com.bjpowernode.seckill.mapper;

import com.bjpowernode.seck.model.Product;
import com.bjpowernode.seck.model.ProductExample;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
@Mapper
public interface ProductMapper {
    long countByExample(ProductExample example);

    int deleteByExample(ProductExample example);

    int insert(Product record);

    int insertSelective(Product record);

    List<Product> selectByExample(ProductExample example);

    int updateByExampleSelective(@Param("record") Product record, @Param("example") ProductExample example);

    int updateByExample(@Param("record") Product record, @Param("example") ProductExample example);
    /**
    *@Description:查询所有商品
    *@Author Mr.Liu
    *@Date 2019/7/27
    *@Time 20:12
    */
    List<Product> queryAllproduct();

    void updateByPrimaryKeySelective(Product product);
}
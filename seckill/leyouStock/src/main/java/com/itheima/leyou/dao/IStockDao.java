package com.itheima.leyou.dao;

import java.util.ArrayList;
import java.util.Map;

public interface IStockDao {

    /**
     * 商品列表页展示
     * @return arraylist，包含多个map，每个map都是一个商品
     */
    public ArrayList<Map<String, Object>> getStockList();


    /**
     * 查询商品详情，为了前端商品详情页展示
     * @param sku_id
     * @return Arraylist,包含一个商品的map
     */
    public ArrayList<Map<String, Object>> getStock(String sku_id);


    public boolean insertLimitPolicy(Map<String, Object> policyInfo);
}

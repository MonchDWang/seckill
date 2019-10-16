package com.itheima.leyou.service;

import java.util.Map;

public interface IStockService {


    /**
     * 商品列表页展示
     * @return arraylist，包含多个map，每个map都是一个商品
     */
    public Map<String, Object> getStockList();


    /**
     * 查询商品详情，为了前端商品详情页展示
     * @param sku_id
     * @return Arraylist,包含一个商品的map
     */
    public Map<String, Object> getStock(String sku_id);

    public Map<String, Object> insertLimitPolicy(Map<String, Object> policyInfo);

}

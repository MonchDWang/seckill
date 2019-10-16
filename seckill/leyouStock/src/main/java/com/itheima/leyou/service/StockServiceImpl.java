package com.itheima.leyou.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.itheima.leyou.dao.IStockDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class StockServiceImpl implements IStockService{

    @Autowired
    private IStockDao iStockDao;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 商品列表页展示
     * @return arraylist，包含多个map，每个map都是一个商品
     */
    public Map<String, Object> getStockList(){
        Map<String, Object> resultMap = new HashMap<String, Object>();

        //1、取自dao层的方法
        ArrayList<Map<String, Object>> list = iStockDao.getStockList();

        //2、如果没有取出数据，返回错误信息
        if (list==null||list.size()==0){
            resultMap.put("result", false);
            resultMap.put("msg", "没有取出数据！");
            return resultMap;
        }

        //3、取redis政策
        resultMap = getLimitPolicy(list);

        //4、返回正常信息
        resultMap.put("sku_list", list);
        /*resultMap.put("result", true);
        resultMap.put("msg", "");*/
        return resultMap;
    }


    /**
     * 查询商品详情，为了前端商品详情页展示
     * @param sku_id
     * @return Arraylist,包含一个商品的map
     */
    public Map<String, Object> getStock(String sku_id){

        Map<String, Object> resultMap = new HashMap<String, Object>();

        //1、传入参数的判断
        if (sku_id==null||sku_id.equals("")){
            resultMap.put("result", false);
            resultMap.put("msg", "前端传入的什么东东！");
            return resultMap;
        }

        //2、取dao层的方法
        ArrayList<Map<String, Object>> list = iStockDao.getStock(sku_id);

        //3、如果没有取出数据，返回错误信息
        if (list==null||list.size()==0){
            resultMap.put("result", false);
            resultMap.put("msg", "没有取出数据！");
            return resultMap;
        }

        //3、取redis政策
        resultMap = getLimitPolicy(list);

        //4、返回正常信息
        resultMap.put("sku", list);
        /*resultMap.put("result", true);
        resultMap.put("msg", "");*/
        return resultMap;
    }


    @Transactional
    public Map<String, Object> insertLimitPolicy(Map<String, Object> policyInfo){
        Map<String, Object> resultMap = new HashMap<String, Object>();

        //1、传入参数的判断
        if (policyInfo==null||policyInfo.isEmpty()){
            resultMap.put("result", false);
            resultMap.put("msg", "前端传入的什么东东！");
            return resultMap;
        }

        //2、取dao层的方法
        boolean result = iStockDao.insertLimitPolicy(policyInfo);

        //3、如果写入失败，返回错误信息
        if (!result){
            resultMap.put("result", false);
            resultMap.put("msg", "写入数据库失败！");
            return resultMap;
        }

        //4、写入redis
        //4.1、有效期，  16:50 - 17:00  16:54  10，6   结束时间减去当前时间，
        long diff = 0;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String now = restTemplate.getForObject("http://leyou-time-server/getTime", String.class);
        try {
            Date end_time = simpleDateFormat.parse(policyInfo.get("end_time").toString());
            Date now_time = simpleDateFormat.parse(now);

            diff = (end_time.getTime() - now_time.getTime())/1000;

            if (diff<0){
                resultMap.put("result", false);
                resultMap.put("msg", "结束时间不能小于当前时间！");
                return resultMap;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        String policy = JSON.toJSONString(policyInfo);
        stringRedisTemplate.opsForValue().set("LIMIT_POLICY_"+policyInfo.get("sku_id").toString(), policy, diff, TimeUnit.SECONDS);

        //商品信息写入redis
        ArrayList<Map<String, Object>> list = iStockDao.getStock(policyInfo.get("sku_id").toString());
        String sku = JSON.toJSONString(list.get(0));
        stringRedisTemplate.opsForValue().set("SKU_"+policyInfo.get("sku_id").toString(), sku, diff, TimeUnit.SECONDS);

        //5、返回正常信息
        resultMap.put("result", true);
        resultMap.put("msg", "政策已经写入！");
        return resultMap;
    }


    private Map<String, Object> getLimitPolicy(ArrayList<Map<String, Object>> list){
        Map<String, Object> resultMap = new HashMap<String, Object>();


        //3.1、先取，如果取出来则给list赋值
        for (Map<String, Object> skuMap: list){
            String policy = stringRedisTemplate.opsForValue().get("LIMIT_POLICY_"+skuMap.get("sku_id").toString());
            if (policy!=null&&!policy.equals("")){
                //3.2、取政策，判断，开始时间<=当前时间，当前时间<=结束时间

                Map<String, Object> policyMap = JSONObject.parseObject(policy, Map.class);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                String now = restTemplate.getForObject("http://leyou-time-server/getTime", String.class);
                try {
                    Date begin_time = simpleDateFormat.parse(policyMap.get("begin_time").toString());
                    Date end_time = simpleDateFormat.parse(policyMap.get("end_time").toString());
                    Date now_time = simpleDateFormat.parse(now);

                    if (begin_time.getTime() <= now_time.getTime() && now_time.getTime() <= end_time.getTime()){
                        //limitPrice  limitQuanty   limitBeginTime   limitEndTime  nowTime
                        skuMap.put("limitPrice", policyMap.get("price"));
                        skuMap.put("limitQuanty", policyMap.get("quanty"));
                        skuMap.put("limitBeginTime", policyMap.get("begin_time"));
                        skuMap.put("limitEndTime", policyMap.get("end_time"));
                        skuMap.put("nowTime", now);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }

        resultMap.put("result", true);
        resultMap.put("msg", "");
        return resultMap;
    }
}

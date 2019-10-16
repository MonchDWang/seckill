package com.itheima.leyou.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.itheima.leyou.dao.IOrderDao;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class OrderServiceImpl implements IOrderService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private IOrderDao iOrderDao;

    public Map<String, Object> createOrder(String sku_id, String user_id){
        Map<String, Object> resultMap = new HashMap<String, Object>();


        //1、传入参数的判断
        if (sku_id==null||sku_id.equals("")){
            resultMap.put("result", false);
            resultMap.put("msg", "前端传过来的什么东东？");
            return resultMap;
        }

        if (user_id==null||user_id.equals("")){
            resultMap.put("result", false);
            resultMap.put("msg", "会员必须进行登录才能购买！");
            return resultMap;
        }

        String order_id = String.valueOf(System.currentTimeMillis());

        //2、从redis取政策
        String policy = stringRedisTemplate.opsForValue().get("LIMIT_POLICY_"+sku_id);
        if (policy!=null&&!policy.equals("")){

            //3、判断政策时间，开始时间<=当前时间，当前时间<=结束时间
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            Map<String, Object> policyMap = JSONObject.parseObject(policy, Map.class);
            String now = restTemplate.getForObject("http://leyou-time-server/getTime", String.class);

            try {
                Date begin_time = simpleDateFormat.parse(policyMap.get("begin_time").toString());
                Date end_time = simpleDateFormat.parse(policyMap.get("end_time").toString());
                Date now_time = simpleDateFormat.parse(now);

                if (begin_time.getTime() <= now_time.getTime() && now_time.getTime() <= end_time.getTime()){

                    long limitQuanty = Long.parseLong(policyMap.get("quanty").toString());

                    //4、通过redis计数
                    // +1+1+1+1  1-10000相加  -- 999 +1 +1

                    //先查询实际占用数量(900)，再查询限制数量(1000)，更新实际占用数量(901)
                    if (stringRedisTemplate.opsForValue().increment("SKU_QUANTY_"+sku_id, 1)<=limitQuanty){


                        // tb_order: order_id, total_fee, actual_fee, post_fee, payment_type, user_id, status, create_time
                        // tb_order_detail: order_id, sku_id, num, title, own_spec, price, image, create_time
                        // tb_sku: sku_id, title, images, stock, price, indexes, own_spec

                        Map<String, Object> orderInfo = new HashMap<String, Object>();
                        orderInfo.put("order_id", order_id);

                        String sku = stringRedisTemplate.opsForValue().get("SKU_"+sku_id);
                        Map<String, Object> skuMap = JSONObject.parseObject(sku, Map.class);

                        orderInfo.put("total_fee", skuMap.get("price"));
                        orderInfo.put("actual_fee", policyMap.get("price"));

                        orderInfo.put("post_fee", 0);
                        orderInfo.put("payment_type", 0);
                        orderInfo.put("user_id", user_id);
                        orderInfo.put("status", 1);
                        orderInfo.put("create_time", now);

                        orderInfo.put("sku_id", skuMap.get("sku_id"));
                        orderInfo.put("num", 1);
                        orderInfo.put("title", skuMap.get("title"));
                        orderInfo.put("own_spec", skuMap.get("own_spec"));
                        orderInfo.put("price", policyMap.get("price"));
                        orderInfo.put("image", skuMap.get("images"));

                        //5、写入队列、并且写入redis
                        try {
                            String order = JSON.toJSONString(orderInfo);
                            amqpTemplate.convertAndSend("order_queue", order);
                            stringRedisTemplate.opsForValue().set("ORDER_"+order_id, order);
                        }catch (Exception e){
                            resultMap.put("result", false);
                            resultMap.put("msg", "你真的点儿背，写队列没写成功！");
                            return resultMap;
                        }

                    }else {
                        //6、没有通过计数的，提示商品已经售完
                        resultMap.put("result", false);
                        resultMap.put("msg", "商品已经售完，踢回去3亿9！");
                        return resultMap;
                    }
                }else {
                    //7、当时间判断有误时，提示活动已经过期
                    resultMap.put("result", false);
                    resultMap.put("msg", "活动已经过期！");
                    return resultMap;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }else {
            //8、没取到政策，提示活动已经过期
            resultMap.put("result", false);
            resultMap.put("msg", "活动已经过期！");
            return resultMap;
        }

        //9、返回正常信息，并且要返回order_id
        resultMap.put("order_id", order_id);
        resultMap.put("result", true);
        resultMap.put("msg", "订单已经提交成功！");
        return resultMap;
    }




    public Map<String, Object> insertOrder(Map<String, Object> orderInfo) {
        return iOrderDao.insertOrder(orderInfo);
    }


    public Map<String, Object> getOrder(String order_id){
        String order = stringRedisTemplate.opsForValue().get("ORDER_"+order_id);
        return JSONObject.parseObject(order, Map.class);
    }


    public Map<String, Object> payOrder(String order_id, String sku_id){
        Map<String, Object> resultMap = new HashMap<String, Object>();

        try {
            amqpTemplate.convertAndSend("order_status_queue", order_id);
            amqpTemplate.convertAndSend("storage_queue", sku_id);
        }catch (Exception e){
            resultMap.put("result", false);
            resultMap.put("msg", "写入队列失败！");
            return resultMap;
        }

        resultMap.put("result", true);
        resultMap.put("msg", "支付成功！");
        return resultMap;
    }

    public Map<String, Object> updateOrderStatus(String order_id) {

        Map<String, Object> resultMap = new HashMap<String, Object>();

        boolean result = iOrderDao.updateOrderStatus(order_id);

        if (!result){
            resultMap.put("result", false);
            resultMap.put("msg", "更新订单状态时失败！");
            return resultMap;
        }

        return resultMap;
    }

}

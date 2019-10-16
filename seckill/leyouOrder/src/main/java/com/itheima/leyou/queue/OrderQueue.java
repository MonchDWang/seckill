package com.itheima.leyou.queue;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.itheima.leyou.service.IOrderService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class OrderQueue {

    @Autowired
    private IOrderService iOrderService;

    @RabbitListener(queues = "order_queue")
    public void insertOrder(String msg){
        //1、接收消息并输出
        System.out.println("order_queue接收消息："+msg);

        //2、调用service的一个insertOrder方法
        Map<String, Object> orderInfo = JSONObject.parseObject(msg, Map.class);
        Map<String, Object> resultMap = new HashMap<String, Object>();

        resultMap.put("result", true);

// iOrderService.insertOrder(orderInfo);

        //3、如果写入失败则输出失败信息
        if (!(Boolean) resultMap.get("result")){
            System.out.println("order_queue处理消息失败："+resultMap.get("msg").toString());
        }else {
            //4、如果成功输出成功信息
            System.out.println("order_queue处理消息成功！");
        }

    }

    @RabbitListener(queues = "order_status_queue")
    public void updateOrderStatus(String msg){
        //1、接收消息并输出
        System.out.println("order_status_queue接收消息："+msg);

        //2、调用service的一个insertOrder方法
        Map<String, Object> resultMap = iOrderService.updateOrderStatus(msg);

        //3、如果写入失败则输出失败信息
        if (!(Boolean) resultMap.get("result")){
            System.out.println("order_status_queue处理消息失败："+resultMap.get("msg").toString());
        }else {
            //4、如果成功输出成功信息
            System.out.println("order_status_queue处理消息成功！");
        }
    }
}

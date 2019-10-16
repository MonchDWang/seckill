package com.itheima.leyou.service;

import com.itheima.leyou.dao.IStorageDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class StorageServiceImpl implements IStorageService{

    @Autowired
    private IStorageDao iStorageDao;

    public Map<String, Object>
    insertStorage(String sku_id, double in_quanty, double out_quanty){
        Map<String, Object> resultMap = new HashMap<String, Object>();

        //1、判断传入的参数
        if (sku_id==null||sku_id.equals("")){
            resultMap.put("result", false);
            resultMap.put("msg", "传入参数是什么东东！");
            return resultMap;
        }

        if (in_quanty==0&&out_quanty==0){
            resultMap.put("result", false);
            resultMap.put("msg", "入库数量和出库数量不能同时为0！");
            return resultMap;
        }

        //2、取dao层的方法
        resultMap = iStorageDao.insertStorage(sku_id, in_quanty, out_quanty);

        //3、如果没有成功，或者已经成功，直接返回
        return resultMap;

    }
}

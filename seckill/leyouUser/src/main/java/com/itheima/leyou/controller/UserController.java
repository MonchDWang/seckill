package com.itheima.leyou.controller;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import com.itheima.leyou.service.IUserService;
import com.netflix.discovery.shared.resolver.ResolverUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;

@RestController
public class UserController {

    @Autowired
    private IUserService iUserService;

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public Map<String, Object> login(String username, String password, HttpServletRequest request){
        //1、调用service里getUser
        Map<String, Object> userMap = iUserService.getUser(username, password);

        //2、如果会员没有，调用service里insertUser
        if (!(Boolean) userMap.get("result")){
            userMap = iUserService.insertUser(username, password);

            //3、insert没有成功返回错误信息
            if (!(Boolean) userMap.get("result")){
                return userMap;
            }
        }

        //4、写入session
        HttpSession httpSession = request.getSession();
        String user = JSON.toJSONString(userMap);
        httpSession.setAttribute("user", user);

        //5、返回正常信息
        return userMap;
    }
}

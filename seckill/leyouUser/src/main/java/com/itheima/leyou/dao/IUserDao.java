package com.itheima.leyou.dao;

import java.util.ArrayList;
import java.util.Map;

public interface IUserDao {

    public ArrayList<Map<String, Object>> getUser(String username, String password);

    int insertUser(String username, String password);
}

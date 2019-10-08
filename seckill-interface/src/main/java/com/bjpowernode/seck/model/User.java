package com.bjpowernode.seck.model;

/**
 * ClassName:User
 * Package:com.bjpowernode.seck.model
 *
 * @Description: 描述
 * @Author: Mr.Liu
 * @Date: 2019/7/30 14:52
 */

public class User {
    private int id;
    private String name;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}

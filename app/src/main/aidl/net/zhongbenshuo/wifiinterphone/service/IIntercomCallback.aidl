package net.zhongbenshuo.wifiinterphone.service;

interface IIntercomCallback {

    void findNewUser(String ipAddress,String name);
    void removeUser(String ipAddress,String name);
}

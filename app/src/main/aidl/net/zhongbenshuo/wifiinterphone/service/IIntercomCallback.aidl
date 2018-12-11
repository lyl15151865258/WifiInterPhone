package net.zhongbenshuo.wifiinterphone.service;

interface IIntercomCallback {

    void findNewUser(String ipAddress);
    void removeUser(String ipAddress);
}

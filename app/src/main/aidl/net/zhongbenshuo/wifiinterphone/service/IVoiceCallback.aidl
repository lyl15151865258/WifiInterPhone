package net.zhongbenshuo.wifiinterphone.service;

interface IVoiceCallback {

    void findNewUser(String ipAddress,String name);
    void removeUser(String ipAddress,String name);
}

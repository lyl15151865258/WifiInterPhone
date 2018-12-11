package net.zhongbenshuo.wifiinterphone.service;

import net.zhongbenshuo.wifiinterphone.service.IIntercomCallback;

interface IIntercomService {

    void startRecord();
    void stopRecord();
    void leaveGroup();
    void registerCallback(IIntercomCallback callback);
    void unRegisterCallback(IIntercomCallback callback);
}

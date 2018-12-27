package net.zhongbenshuo.wifiinterphone.service;

import net.zhongbenshuo.wifiinterphone.service.IVoiceCallback;

interface IVoiceService {

    void startRecord();
    void stopRecord();
    void leaveGroup();
    void registerCallback(IVoiceCallback callback);
    void unRegisterCallback(IVoiceCallback callback);
}

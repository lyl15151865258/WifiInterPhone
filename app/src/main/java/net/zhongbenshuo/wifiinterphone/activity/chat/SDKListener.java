package net.zhongbenshuo.wifiinterphone.activity.chat;

/**
 * Created by Administrator on 2017/11/24.
 */

public interface SDKListener {

    void onInitSDK(int state, String errorinfo);

    void onJoinRoom(int state, String errorinfo);

    void onNotifyUserJoinRoom(String userlist);

    void onNotifyUserLeaveRoom();

}
package net.zhongbenshuo.wifiinterphone.activity.chat;

import android.text.TextUtils;

import com.reechat.voiceengine.SdkVoiceListener;
import com.reechat.voiceengine.ReceiveDataFromC;

import net.zhongbenshuo.wifiinterphone.utils.LogUtils;

import org.json.JSONObject;

/**
 * Created by Administrator on 2017/11/24.
 */

public class HandleUtil {

    public ReceiveDataFromC receiveDataFromC = new ReceiveDataFromC();

    private static final HandleUtil instance = new HandleUtil();

    private SDKListener mSDKListener = null;

    private String userList = "";
    private String downloadUrl = "";

    public HandleUtil() {
    }

    public static HandleUtil getInstance() {
        return instance;
    }

    public SDKListener getSDKLiveListener() {
        return mSDKListener;
    }

    public void setListener(SDKListener liveListener) {
        this.mSDKListener = liveListener;
        receiveDataFromC.set_voice_listener(myListener);
    }

    private static FileData getDataFromJson(String msg) {
        try {
            if (TextUtils.isEmpty(msg))
                return null;
            JSONObject jsonObject = new JSONObject(msg);
            String url = jsonObject.getString("url");
            String duration = jsonObject.getString("duration");
            String filesize = jsonObject.getString("filesize");
            String text = jsonObject.getString("text");
            String labelid = jsonObject.getString("labelid");
            FileData user = new FileData();
            user.setDuration(duration);
            user.setUrl(url);
            user.setFilesize(filesize);
            user.setText(text);
            user.setLabelid(labelid);
            return user;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getUserList() {
        return userList;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    private SdkVoiceListener myListener = (cmdType, error, dataPtr, dataSize) -> {
        LogUtils.d("HandleUtil", "cmdType:" + cmdType + "，dataPtr:" + dataPtr);
        switch (cmdType) {
            case 1:
                //初始化
                mSDKListener.onInitSDK(error, dataPtr);
                break;
            case 7:
                //自己进入房间成功
                mSDKListener.onJoinRoom(error, dataPtr);
                break;
            case 16:
                //自己退出房间成功，用户列表清空
                mSDKListener.onLeaveRoom(error, dataPtr);
                break;
            case 17:
                //用户列表发生变化（有用户进入或退出，自己初次进入的时候也会触发）
                userList = dataPtr;
                break;
            case 18:
                //有用户进入房间
                userList = dataPtr;
                mSDKListener.onNotifyUserJoinRoom(dataPtr);
            case 19:
                //有用户离开房间
                userList = dataPtr;
                mSDKListener.onNotifyUserLeaveRoom(dataPtr);
                break;
            case 25:
                //录音结束，上传成功
                FileData fileData = getDataFromJson(dataPtr);
                if (fileData == null) {
                    downloadUrl = null;
                } else {
                    downloadUrl = fileData.getUrl();
                }
                break;
            case 35:
                //播放结束

                break;
            default:
                break;
        }
    };

}

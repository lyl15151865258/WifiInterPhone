package net.zhongbenshuo.wifiinterphone.activity.chat;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import com.reechat.voiceengine.EventInterface;
import com.reechat.voiceengine.NativeVoiceEngine;

import net.zhongbenshuo.wifiinterphone.R;
import net.zhongbenshuo.wifiinterphone.activity.BaseActivity;
import net.zhongbenshuo.wifiinterphone.contentprovider.SPHelper;
import net.zhongbenshuo.wifiinterphone.utils.ActivityController;
import net.zhongbenshuo.wifiinterphone.widget.MyToolbar;

import java.util.Random;

/**
 * 聊天室页面
 * Created at 2018/12/26 14:58
 *
 * @author Li Yuliang0
 * @version 1.0
 */

public class ChatRoomActivity extends BaseActivity {

    private NativeVoiceEngine rtChatSdk;
    private static final int kRoomType = 0x03 | 0x60 | 0x1800;
    private boolean blouderspeak = false,bunmute = true;
    private ImageButton toggleMuteButton, toggleSpeakerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);
        MyToolbar toolbar = findViewById(R.id.myToolbar);
        toolbar.initToolBar(this, toolbar, "聊天室测试", R.drawable.back_white, onClickListener);

        ImageButton disconnectButton = findViewById(R.id.button_call_disconnect);
        toggleMuteButton = findViewById(R.id.button_call_toggle_mic);
        toggleSpeakerButton = findViewById(R.id.button_call_toggle_speak);
        toggleMuteButton.setOnClickListener(onClickListener);
        toggleSpeakerButton.setOnClickListener(onClickListener);
        disconnectButton.setOnClickListener(onClickListener);


        rtChatSdk = NativeVoiceEngine.getInstance();
        rtChatSdk.register(this);
        rtChatSdk.setDebugLogEnabled(true);

        HandleUtil.getInstance().setListener(sdkListener);

        String userName = SPHelper.getString("UserName", "Not Defined");
        rtChatSdk.SetUserInfo(getRandomString(5), userName);
        rtChatSdk.SetRoomType(kRoomType, 4);
        rtChatSdk.RequestJoinRoom("ZBS");
    }

    public static String getRandomString(int strLength) {
        String sourceChar = "0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strLength; i++) {
            sb.append(sourceChar.charAt(random.nextInt(sourceChar.length())));
        }
        return sb.toString();
    }

    private View.OnClickListener onClickListener = (v) -> {
        switch (v.getId()) {
            case R.id.iv_left:
                ActivityController.finishActivity(this);
                break;
            case R.id.button_call_toggle_mic:
                // 本地禁音接口
                bunmute = !bunmute;
                NativeVoiceEngine.getInstance().SetSendVoice(bunmute);
                toggleMuteButton.setAlpha(bunmute ? 1.0f : 0.3f);
                break;
            case R.id.button_call_toggle_speak:
                // 开关扬声器
                blouderspeak = !blouderspeak;
                NativeVoiceEngine.getInstance().UseLoudSpeaker(blouderspeak);
                toggleSpeakerButton.setAlpha(blouderspeak ? 1.0f : 0.3f);
                break;
            case R.id.button_call_disconnect:
                // 请求离开房间
                leaveRoom();
                break;
            default:
                break;
        }
    };

    private void leaveRoom() {
        NativeVoiceEngine.getInstance().registerEventHandler(null);
        NativeVoiceEngine.getInstance().RequestQuitRoom();
        if (rtChatSdk != null) {
            rtChatSdk.stopAudioManager();
            rtChatSdk.unRegister();
            rtChatSdk = null;
        }
        ActivityController.finishActivity(this);
    }

    private SDKListener sdkListener = new SDKListener() {
        @Override
        public void onInitSDK(int state, String errorinfo) {
            String msg = "SDK初始化失败:";
            int ret = rtChatSdk.GetSdkState();
            if (state == 1) {
                msg = "SDK初始化成功:";
            }

            if (state == 0 && ret > 0) {
                msg = "SDK重复初始化:";
            }
            showToast(msg + errorinfo);
        }

        @Override
        public void onJoinRoom(int state, String errorinfo) {
            String msg = "用户进入房间失败:";
            if (state == 1) {
                msg = "用户进入房间成功:";

                //开关扬声器
                NativeVoiceEngine.getInstance().UseLoudSpeaker(blouderspeak);
                toggleSpeakerButton.setAlpha(blouderspeak ? 1.0f : 0.3f);
                //本地禁音接口
                NativeVoiceEngine.getInstance().SetSendVoice(bunmute);
                toggleMuteButton.setAlpha(bunmute ? 1.0f : 0.3f);

                //注册回调
                NativeVoiceEngine.getInstance().registerEventHandler(mListener);

            }
            showToast(msg + errorinfo);
        }

        @Override
        public void onLeaveRoom(int state, String errorinfo) {

        }

        @Override
        public void onNotifyUserJoinRoom(String userlist) {

        }

        @Override
        public void onNotifyUserLeaveRoom(String userlist) {

        }

    };

    private EventInterface mListener = new EventInterface() {
        @Override
        public void onEventUserJoinRoom(String uid) {
            String uuid = uid.split("\"")[1];
            showToast(uuid + "进入房间");
        }

        @Override
        public void onEventUserLeaveRoom(String uid) {
            String uuid = uid.split("\"")[1];
            showToast(uuid + "离开房间");
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        leaveRoom();
        this.finish();
    }

}

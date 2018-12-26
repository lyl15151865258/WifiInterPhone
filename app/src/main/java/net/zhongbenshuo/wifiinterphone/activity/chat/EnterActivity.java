package net.zhongbenshuo.wifiinterphone.activity.chat;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.reechat.voiceengine.NativeVoiceEngine;

import net.zhongbenshuo.wifiinterphone.R;
import net.zhongbenshuo.wifiinterphone.activity.BaseActivity;
import net.zhongbenshuo.wifiinterphone.contentprovider.SPHelper;
import net.zhongbenshuo.wifiinterphone.utils.ActivityController;
import net.zhongbenshuo.wifiinterphone.widget.MyToolbar;

import java.util.Random;

public class EnterActivity extends BaseActivity {

    private static final int CONNECTION_REQUEST = 1000;
    private NativeVoiceEngine rtChatSdk;
    private String roomId = "";
    private String userName = "";
    private EditText roomEditText;
    private boolean isLunched = false;
    private static final int kRoomType = 0x03 | 0x60 | 0x1800;
    private Button btnRecord;
    private Button btnPlay;
    private boolean isRecording = false;
    private boolean isPlaying = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter);

        MyToolbar toolbar = findViewById(R.id.myToolbar);
        toolbar.initToolBar(this, toolbar, "多人语音对讲测试", R.drawable.back_white, onClickListener);

        btnRecord = findViewById(R.id.btnRecord);
        btnPlay = findViewById(R.id.btnPlay);
        findViewById(R.id.btnRecord).setOnClickListener(onClickListener);
        findViewById(R.id.btnPlay).setOnClickListener(onClickListener);
        findViewById(R.id.brnEnterRoom).setOnClickListener(onClickListener);

        //获取实例
        rtChatSdk = NativeVoiceEngine.getInstance();
        rtChatSdk.register(this);
        rtChatSdk.setDebugLogEnabled(true);

        HandleUtil.getInstance().setListener(sdkListener);

        roomEditText = findViewById(R.id.room_edittext);
        roomEditText.requestFocus();

    }

    @Override
    public void onResume() {
        super.onResume();
        String room = SPHelper.getString(getString(R.string.pref_room_key), "");
        roomEditText.setText(room);
        isLunched = false;
    }

    private View.OnClickListener onClickListener = (v) -> {
        switch (v.getId()) {
            case R.id.iv_left:
                rtChatSdk.stopAudioManager();
                rtChatSdk.unRegister();
                rtChatSdk = null;
                ActivityController.finishActivity(this);
                break;
            case R.id.btnRecord:
                if (isRecording) {
                    //停止录音接口
                    rtChatSdk.StopIMRecord();
                    btnRecord.setText("已经停止录音");
                } else {
                    //开始录音接口
                    boolean ret = rtChatSdk.StartIMRecord(false, false, 1);
                    showToast(" startRecordVoice  code = " + ret);
                    btnRecord.setText("正在录音");
                }
                isRecording = !isRecording;
                break;
            case R.id.btnPlay:
                if (isPlaying) {
                    //停止播放接口
                    rtChatSdk.StopPlayIMVoice();
                    btnPlay.setText("已经停止播放");
                } else {
                    String downloadUrlLocal = HandleUtil.getInstance().getDownloadUrl();
                    if (downloadUrlLocal == null) {
                        return;
                    }
                    //开始播放接口
                    rtChatSdk.StartPlayIMVoice(downloadUrlLocal);
                    btnPlay.setText("正在播放");
                }
                isPlaying = !isPlaying;
                break;
            case R.id.brnEnterRoom:
                //如果Im在录音播放状态，先停止录音播放，再加入房间；
                if (isRecording) {
                    //停止录音接口
                    rtChatSdk.StopIMRecord();
                    btnRecord.setText("已经停止录音");
                }
                if (isPlaying) {
                    //停止播放接口
                    rtChatSdk.StopPlayIMVoice();
                    btnPlay.setText("已经停止播放");
                }
                this.userName = getRandomString(5);
                rtChatSdk.SetUserInfo(userName,SPHelper.getString("UserName", "Not Defined"));
//                this.userName = SPHelper.getString("UserName", "Not Defined");
//                rtChatSdk.SetUserInfo(userName, getRandomString(6));
                connectToRoom(roomEditText.getText().toString(), kRoomType);
                break;
            default:
                break;
        }
    };

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
            if (state == 1 && !isLunched) {
                msg = "用户进入房间成功:";
                launchVideoActivity();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        rtChatSdk.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onPause() {
        super.onPause();
        String room = roomEditText.getText().toString();
        SPHelper.save(getString(R.string.pref_room_key), room);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (rtChatSdk != null) {
            rtChatSdk.stopAudioManager();
            rtChatSdk.unRegister();
            rtChatSdk = null;
        }
        this.finish();
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

    private void launchVideoActivity() {
        Intent intent = new Intent(this, VideoActivity.class);
        intent.putExtra("username", this.userName);
        intent.putExtra("roomId", this.roomId);
        isLunched = true;
        startActivityForResult(intent, CONNECTION_REQUEST);
    }

    private void connectToRoom(String roomId, int mediaType) {
        int retCode;
        if (roomId == null) {
            showToast("请输入房间号");
        } else {
            this.roomId = roomId;
            //请求加入房间
            rtChatSdk.SetRoomType(mediaType, 4);
            retCode = rtChatSdk.RequestJoinRoom(roomId);
        }
    }

}

package net.zhongbenshuo.wifiinterphone.activity.chat;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.reechat.videoengine.NativeVideoEngine;
import com.reechat.videoengine.VideoEngineImpl;
import com.reechat.voiceengine.NativeVoiceEngine;
import com.reechat.voiceengine.EventInterface;

import net.zhongbenshuo.wifiinterphone.R;
import net.zhongbenshuo.wifiinterphone.activity.BaseActivity;
import net.zhongbenshuo.wifiinterphone.utils.ActivityController;
import net.zhongbenshuo.wifiinterphone.utils.DeviceUtil;
import net.zhongbenshuo.wifiinterphone.utils.LogUtils;
import net.zhongbenshuo.wifiinterphone.widget.MyToolbar;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VideoActivity extends BaseActivity {

    private final String TAG = "VideoActivityTag";
    private ImageButton toggleMuteButton, toggleSpeakerButton;
    private RelativeLayout remoteRelLayout;
    private List<Integer> removeId = new ArrayList<>();
    private Map<String, SurfaceView> surfaceViewMap;
    private String username;
    private boolean blouderspeak = false;
    private boolean bunmute = true;
    private VideoEngineImpl mVideoEngineImp;
    private NativeVideoEngine mNVEngine;
    private int count = 10;
    private int camera_index_ = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videosfu);
        MyToolbar toolbar = findViewById(R.id.myToolbar);
        toolbar.initToolBar(this, toolbar, "视频通话页面", R.drawable.back_white, onClickListener);
        final Intent intent = getIntent();

        username = intent.getStringExtra("username");
        String roomId = intent.getStringExtra("roomId");

        //获取实例
        mNVEngine = NativeVideoEngine.getInstance();

        mVideoEngineImp = VideoEngineImpl.getInstance();
        surfaceViewMap = new HashMap<>();

        remoteRelLayout = findViewById(R.id.remoteContainer);

        TextView tvRoomId = findViewById(R.id.tv_roomId);
        TextView tvUserName = findViewById(R.id.tv_userName);

        tvRoomId.setText("当前房间号：" + roomId);
        tvUserName.setText("当前用户ID：" + username);

        ImageButton disconnectButton = findViewById(R.id.button_call_disconnect);
        toggleMuteButton = findViewById(R.id.button_call_toggle_mic);
        toggleSpeakerButton = findViewById(R.id.button_call_toggle_speak);

        findViewById(R.id.btnAddUser).setOnClickListener(onClickListener);
        findViewById(R.id.btnRemoveUser).setOnClickListener(onClickListener);
        findViewById(R.id.btnShareScreen).setOnClickListener(onClickListener);
        findViewById(R.id.btnChangeCamera).setOnClickListener(onClickListener);

        //开关扬声器
        NativeVoiceEngine.getInstance().UseLoudSpeaker(blouderspeak);
        toggleSpeakerButton.setAlpha(blouderspeak ? 1.0f : 0.3f);
        //本地禁音接口
        NativeVoiceEngine.getInstance().SetSendVoice(bunmute);
        toggleMuteButton.setAlpha(bunmute ? 1.0f : 0.3f);

        //注册回调
        NativeVoiceEngine.getInstance().registerEventHandler(mListener);
        toggleMuteButton.setOnClickListener(onClickListener);
        toggleSpeakerButton.setOnClickListener(onClickListener);
        disconnectButton.setOnClickListener(onClickListener);

        // 初始化视频
        initVideo();

        String userlist = HandleUtil.getInstance().getUserList();
        if (userlist.length() != 0) {
            try {
                JSONArray userList = new JSONArray(userlist);
                for (int i = 0; i < userList.length(); ++i) {
                    String uuid = userList.getString(i);
                    addSurfaceView(uuid);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private View.OnClickListener onClickListener = (v) -> {
        EditText et = new EditText(this);
        switch (v.getId()) {
            case R.id.iv_left:
                ActivityController.finishActivity(this);
                break;
            case R.id.btnAddUser:
                // 添加用户
                new AlertDialog.Builder(this).setTitle("输入观看用户id：")
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setView(et)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                String input = et.getText().toString();
                                if (input.equals("")) {
                                    showToast("用户id空！" + input);
                                } else {
                                    if (!input.equals(username))
                                        addSurfaceView(input);
                                }
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
                break;
            case R.id.btnRemoveUser:
                // 删除用户
                new AlertDialog.Builder(this).setTitle("输入删除用户id：")
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setView(et)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                String input = et.getText().toString();
                                if (input.equals("")) {
                                    showToast("用户id空！" + input);
                                } else {
                                    if (!input.equals(username))
                                        removeSurfaceView(input);
                                }
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
                break;
            case R.id.btnShareScreen:
                // 分享屏幕，屏幕共享接口支持Android 5.0
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
                    mVideoEngineImp.startScreenCapture(this);
                } else {
                    showToast("您的手机不支持分享屏幕");
                }
                break;
            case R.id.btnChangeCamera:
                // 切换前后摄像头
                camera_index_ = camera_index_ == 1 ? 2 : 1;
                mNVEngine.SwitchVideoSource(camera_index_);
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
                NativeVoiceEngine.getInstance().registerEventHandler(null);
                NativeVoiceEngine.getInstance().RequestQuitRoom();
                for (String key : surfaceViewMap.keySet()) {
                    SurfaceView mSurfaceView = surfaceViewMap.get(key);
                    if (mSurfaceView != null) {
                        remoteRelLayout.removeView(mSurfaceView);
                    }
                    //销毁渲染窗口
                    mNVEngine.DestroyAVideoWindow(mSurfaceView);
                }
                surfaceViewMap.clear();
                removeId.clear();
                ActivityController.finishActivity(this);
                break;
            default:
                break;
        }
    };

    @Override
    protected void onDestroy() {
        NativeVoiceEngine.getInstance().registerEventHandler(null);
        NativeVoiceEngine.getInstance().RequestQuitRoom();
        for (String key : surfaceViewMap.keySet()) {
            SurfaceView mSurfaceView = surfaceViewMap.get(key);
            if (mSurfaceView == null)
                return;
            remoteRelLayout.removeView(mSurfaceView);
            //销毁渲染窗口
            mNVEngine.DestroyAVideoWindow(mSurfaceView);
        }
        surfaceViewMap.clear();
        removeId.clear();
        ActivityController.finishActivity(this);
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mVideoEngineImp.onActivityResult(requestCode, resultCode, data, surfaceViewMap.get(username));
        }
    }

    private void setLayoutRule(SurfaceView mSurfaceView, int index) {
        LogUtils.d(TAG, "增加SurfaceView");
        RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(mWidth/2, DeviceUtil.dp2px(this, 180));
        int margin = DeviceUtil.dp2px(this, 2);
        param.setMargins(margin, margin, margin, margin);
        mSurfaceView.setId(index);
        switch (index) {
            case 10: {
                param.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                param.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                break;
            }
            case 11: {
                param.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                param.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                break;
            }
            case 12: {
                param.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                param.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                break;
            }
            case 13: {
                param.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                param.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                break;
            }
            default:
                break;
        }
        remoteRelLayout.addView(mSurfaceView, param);
    }

    private void addLocalSurfaceView() {
        if (surfaceViewMap.containsKey(username)) {
            return;
        }
        LogUtils.d(TAG, "addLocalSurfaceView:" + username);
        //创建渲染窗口
        SurfaceView mSurfaceView = mNVEngine.CreateAVideoWindow(1);
        int addCount = count;
        boolean removeIdFlag = false;
        if (removeId.isEmpty()) {
            removeIdFlag = true;
        } else {
            int min = 20;
            for (int it : removeId) {
                min = it < min ? it : min;
            }
            addCount = min;
            removeId.remove(Integer.valueOf(min));
        }

        setLayoutRule(mSurfaceView, addCount);

        surfaceViewMap.put(username, mSurfaceView);
        if (removeIdFlag)
            count++;

        mSurfaceView.setVisibility(View.VISIBLE);
    }

    private void addSurfaceView(String userId) {
        if (surfaceViewMap.containsKey(userId)) {
            return;
        }
        LogUtils.d(TAG, "addSurfaceView:" + userId);
        //创建渲染窗口
        SurfaceView mSurfaceView = mNVEngine.CreateAVideoWindow(1);
        int addCount = count;
        boolean removeIdFlag = false;
        if (removeId.isEmpty()) {
            removeIdFlag = true;
        } else {
            int min = 20;
            for (int it : removeId) {
                min = it < min ? it : min;
            }
            addCount = min;
            removeId.remove(Integer.valueOf(min));
        }
        //观看远端视频
        int ret = mNVEngine.ObserverRemoteTargetVideoV1(userId, mSurfaceView);
        if (ret == 0) {
            //销毁渲染窗口
            mNVEngine.DestroyAVideoWindow(mSurfaceView);
            return;
        }
        setLayoutRule(mSurfaceView, addCount);

        surfaceViewMap.put(userId, mSurfaceView);
        if (removeIdFlag)
            count++;

        mSurfaceView.setVisibility(View.VISIBLE);
    }

    private void removeSurfaceView(String userId) {
        SurfaceView mSurfaceView = surfaceViewMap.get(userId);
        if (mSurfaceView == null)
            return;
        //停止观看远端视频
        mNVEngine.ObserverRemoteTargetVideoV1(userId, null);
        mSurfaceView.setVisibility(View.INVISIBLE);
        int id = mSurfaceView.getId();
        if (id == (count - 1)) {
            count--;
        } else {
            removeId.add(id);
        }
        remoteRelLayout.removeView(mSurfaceView);
        //销毁渲染窗口
        mNVEngine.DestroyAVideoWindow(mSurfaceView);
        surfaceViewMap.remove(userId);
    }

    /**
     * 初始化视频
     */
    private void initVideo() {
        //创建localSurfaceView
        addLocalSurfaceView();
        //观看本地视频，
        mNVEngine.ObserverLocalVideoWindow(true, surfaceViewMap.get(username));
        //发送本地视频
        mNVEngine.StartSendVideo();
        addSurfaceView("");
    }

    private EventInterface mListener = new EventInterface() {
        @Override
        public void onEventUserJoinRoom(String uid) {
            showToast(uid + "进入房间");
            String uuid = uid.split("\\[")[1].split("\\]")[0].split("\"")[1];
            if (!uuid.equals(username))
                addSurfaceView(uuid);
        }

        @Override
        public void onEventUserLeaveRoom(String uid) {
            String uuid = uid.split("\\[")[1].split("\\]")[0].split("\"")[1];
            showToast(uid + "离开房间");
            removeSurfaceView(uuid);
        }
    };

    @Override
    public void onBackPressed() {
        NativeVoiceEngine.getInstance().registerEventHandler(null);
        NativeVoiceEngine.getInstance().RequestQuitRoom();
        for (String key : surfaceViewMap.keySet()) {
            SurfaceView mSurfaceView = surfaceViewMap.get(key);
            if (mSurfaceView == null)
                return;
            remoteRelLayout.removeView(mSurfaceView);
            //销毁渲染窗口
            mNVEngine.DestroyAVideoWindow(mSurfaceView);
        }
        surfaceViewMap.clear();
        removeId.clear();
        super.onBackPressed();
    }

}

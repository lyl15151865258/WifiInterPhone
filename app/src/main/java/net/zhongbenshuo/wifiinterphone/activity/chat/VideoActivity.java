package net.zhongbenshuo.wifiinterphone.activity.chat;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import net.zhongbenshuo.wifiinterphone.adapter.VideoAdapter;
import net.zhongbenshuo.wifiinterphone.bean.Video;
import net.zhongbenshuo.wifiinterphone.utils.ActivityController;
import net.zhongbenshuo.wifiinterphone.utils.DeviceUtil;
import net.zhongbenshuo.wifiinterphone.utils.LogUtils;
import net.zhongbenshuo.wifiinterphone.widget.MyToolbar;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class VideoActivity extends BaseActivity {

    private final String TAG = "VideoActivityTag";
    private ImageButton toggleMuteButton, toggleSpeakerButton;
    private List<Video> videoList = new ArrayList<>();
    private VideoAdapter videoAdapter;
    private String username;
    private boolean blouderspeak = false;
    private boolean bunmute = true;
    private VideoEngineImpl mVideoEngineImp;
    private NativeVideoEngine mNVEngine;
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

        RecyclerView remoteContainer = findViewById(R.id.remoteContainer);
        GridLayoutManager layoutManage = new GridLayoutManager(this, 2);
        remoteContainer.setLayoutManager(layoutManage);
        videoAdapter = new VideoAdapter(this, videoList);
        remoteContainer.setAdapter(videoAdapter);

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
                leaveRoom();
                break;
            default:
                break;
        }
    };

    private void leaveRoom() {
        NativeVoiceEngine.getInstance().registerEventHandler(null);
        NativeVoiceEngine.getInstance().RequestQuitRoom();
        for (Video video : videoList) {
            SurfaceView mSurfaceView = video.getSurfaceView();
            if (mSurfaceView != null) {
                //销毁渲染窗口
                mNVEngine.DestroyAVideoWindow(mSurfaceView);
            }
        }
        videoList.clear();
        videoAdapter.notifyDataSetChanged();
        ActivityController.finishActivity(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        leaveRoom();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            SurfaceView mSurfaceView = null;
            for (int i = 0; i < videoList.size(); i++) {
                if (videoList.get(i).getVideoNumber().equals(username)) {
                    mSurfaceView = videoList.get(i).getSurfaceView();
                }
            }
            if (mSurfaceView != null) {
                mVideoEngineImp.onActivityResult(requestCode, resultCode, data, mSurfaceView);
            }
        }
    }

    private void setLayoutRule(SurfaceView mSurfaceView, int index) {
        LogUtils.d(TAG, "增加SurfaceView");
        RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(mWidth / 2, DeviceUtil.dp2px(this, 180));
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
    }

    private void addLocalSurfaceView() {
        if (!videoList.contains(new Video(username, null))) {
            LogUtils.d(TAG, "addLocalSurfaceView:" + username);
            //创建渲染窗口
            SurfaceView mSurfaceView = mNVEngine.CreateAVideoWindow(1);
            if (mSurfaceView!=null){
            videoList.add(new Video(username, mSurfaceView));
            videoAdapter.notifyItemInserted(videoList.size() - 1);}
        }
    }

    private void addSurfaceView(String userId) {
        if (!videoList.contains(new Video(userId, null))) {
            LogUtils.d(TAG, "addSurfaceView:" + userId);
            //创建渲染窗口
            SurfaceView mSurfaceView = mNVEngine.CreateAVideoWindow(1);
            videoList.add(new Video(userId, mSurfaceView));
            videoAdapter.notifyItemInserted(videoList.size() - 1);

            //观看远端视频
            int ret = mNVEngine.ObserverRemoteTargetVideoV1(userId, mSurfaceView);
            if (ret == 0) {
                //销毁渲染窗口
                mNVEngine.DestroyAVideoWindow(mSurfaceView);
            }
        }
    }

    private void removeSurfaceView(String userId) {
        int position = -1;
        SurfaceView mSurfaceView = null;
        for (int i = 0; i < videoList.size(); i++) {
            if (videoList.get(i).getVideoNumber().equals(userId)) {
                mSurfaceView = videoList.get(i).getSurfaceView();
                position = i;
            }
        }
        if (mSurfaceView != null) {
            //停止观看远端视频
            mNVEngine.ObserverRemoteTargetVideoV1(userId, null);
            mSurfaceView.setVisibility(View.INVISIBLE);

            //销毁渲染窗口
            mNVEngine.DestroyAVideoWindow(mSurfaceView);

            videoList.remove(position);
            videoAdapter.notifyItemRemoved(position);
        }
    }

    /**
     * 初始化视频
     */
    private void initVideo() {
        //创建localSurfaceView
        addLocalSurfaceView();
        //观看本地视频
        SurfaceView mSurfaceView = null;
        for (int i = 0; i < videoList.size(); i++) {
            if (videoList.get(i).getVideoNumber().equals(username)) {
                mSurfaceView = videoList.get(i).getSurfaceView();
            }
        }
        mNVEngine.ObserverLocalVideoWindow(true, mSurfaceView);
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

}

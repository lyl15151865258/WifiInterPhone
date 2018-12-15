package net.zhongbenshuo.wifiinterphone.activity;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.support.v7.content.res.AppCompatResources;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.zhongbenshuo.wifiinterphone.broadcast.BaseBroadcastReceiver;
import net.zhongbenshuo.wifiinterphone.broadcast.MediaButtonReceiver;
import net.zhongbenshuo.wifiinterphone.constant.Permission;
import net.zhongbenshuo.wifiinterphone.R;
import net.zhongbenshuo.wifiinterphone.contentprovider.SPHelper;
import net.zhongbenshuo.wifiinterphone.fragment.ContactsFragment;
import net.zhongbenshuo.wifiinterphone.fragment.MalfunctionFragment;
import net.zhongbenshuo.wifiinterphone.service.IIntercomService;
import net.zhongbenshuo.wifiinterphone.service.IntercomService;
import net.zhongbenshuo.wifiinterphone.adapter.SelectModuleAdapter;
import net.zhongbenshuo.wifiinterphone.utils.ActivityController;
import net.zhongbenshuo.wifiinterphone.utils.LogUtils;
import net.zhongbenshuo.wifiinterphone.utils.WifiUtil;
import net.zhongbenshuo.wifiinterphone.widget.NoScrollViewPager;
import net.zhongbenshuo.wifiinterphone.widget.dialog.CommonWarningDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * 主页面
 * Created at 2018/11/20 13:38
 *
 * @author LiYuliang
 * @version 1.0
 */

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";
    private Context mContext;
    private TextView tvSSID, tvIp, tvMessage;
    private NoScrollViewPager mViewpager;
    private ImageView ivTitleIndicator;
    private List<TextView> textViews;
    private Vibrator vibrator;
    private IIntercomService intercomService;
    private static final int REQUEST_PERMISSION = 1;

    private CommonWarningDialog commonWarningDialog;

    private MediaPlayer mediaPlayer;
    private AudioManager mAudioManager;
    private ComponentName mComponentName;

    private NetworkStatusReceiver networkStatusReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        initView();
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        PackageManager pkgManager = getPackageManager();
        boolean audioSatePermission = pkgManager.checkPermission(Manifest.permission.RECORD_AUDIO, getPackageName()) == PackageManager.PERMISSION_GRANTED;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !audioSatePermission) {
            ActivityCompat.requestPermissions(this, Permission.MICROPHONE, REQUEST_PERMISSION);
        }

        Intent intent = new Intent(mContext, IntercomService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mComponentName = new ComponentName(getPackageName(), MediaButtonReceiver.class.getName());

        networkStatusReceiver = new NetworkStatusReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkStatusReceiver, intentFilter);

        // 初始化耳机按键标记
        SPHelper.save("KEY_STATUS_UP", true);
    }

    //焦点问题
    private AudioManager.OnAudioFocusChangeListener focusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:// 长时间失去
                    mAudioManager.unregisterMediaButtonEventReceiver(mComponentName);
                    mAudioManager.abandonAudioFocus(focusChangeListener);//放弃焦点监听
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:// 短时间失去
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:// 短时间失去，但可以共用
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:// 获得音频焦点
                    mAudioManager.registerMediaButtonEventReceiver(mComponentName);
                    break;

            }
        }
    };

    private void initView() {
        textViews = new ArrayList<>();
        mViewpager = findViewById(R.id.viewpager);
        ivTitleIndicator = findViewById(R.id.ivTitleIndicator);
        TextView tvContacts = findViewById(R.id.tvContacts);
        TextView tvWarning = findViewById(R.id.tvWarning);
        textViews.add(tvContacts);
        textViews.add(tvWarning);
        tvContacts.setOnClickListener(onClickListener);
        tvWarning.setOnClickListener(onClickListener);
        tvSSID = findViewById(R.id.tvSSID);
        tvIp = findViewById(R.id.tvIp);
        tvMessage = findViewById(R.id.tvMessage);
        findViewById(R.id.btnSpeak).setOnTouchListener(onTouchListener);
        findViewById(R.id.ivSetting).setOnClickListener(onClickListener);

        List<Fragment> fragments = new ArrayList<>();
        fragments.add(new ContactsFragment());
        fragments.add(new MalfunctionFragment());
        mViewpager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                changeIndicator(position, positionOffset);
            }

            @Override
            public void onPageSelected(int position) {
                //所有文本设置为浅灰色
                for (TextView textView : textViews) {
                    textView.setTextColor(AppCompatResources.getColorStateList(mContext, R.color.gray_slight));
                    TextPaint paint = textView.getPaint();
                    paint.setFakeBoldText(false);
                }
                textViews.get(position).setTextColor(AppCompatResources.getColorStateList(mContext, R.color.colorBluePrimary));
                //设置选中项文本字体为粗体
                TextPaint paint = textViews.get(position).getPaint();
                paint.setFakeBoldText(true);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        SelectModuleAdapter adapter = new SelectModuleAdapter(getSupportFragmentManager(), fragments);
        mViewpager.setAdapter(adapter);
        mViewpager.setOffscreenPageLimit(textViews.size());
        mViewpager.setIntercept(true);
        initItem(0);
    }

    /**
     * 设置初始化的位置
     *
     * @param position 初始化显示的Fragment
     */
    private void initItem(int position) {
        changeIndicator(position, 0);
        mViewpager.setCurrentItem(position);
        //所有文本设置为浅灰色
        for (TextView textView : textViews) {
            textView.setTextColor(AppCompatResources.getColorStateList(mContext, R.color.gray_slight));
            TextPaint paint = textView.getPaint();
            paint.setFakeBoldText(false);
        }
        textViews.get(position).setTextColor(AppCompatResources.getColorStateList(mContext, R.color.colorBluePrimary));
        //设置选中项文本字体为粗体
        TextPaint paint = textViews.get(position).getPaint();
        paint.setFakeBoldText(true);
        //当前子Fragment

    }

    /**
     * 标题指示器（下划线）位置的改变
     * 这里通过改变ivTitleIndicator的左外边距来改变其位置
     *
     * @param position 选中的位置
     * @param offset   偏移量
     */
    private void changeIndicator(int position, float offset) {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) ivTitleIndicator.getLayoutParams();
        //下划线横向长度
        lp.width = mWidth / textViews.size();
        //下划线距离屏幕左侧距离
        lp.leftMargin = (int) (offset * (mWidth * 1.0 / textViews.size()) + position * (mWidth / textViews.size()));
        ivTitleIndicator.setLayoutParams(lp);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (WifiUtil.WifiConnected(mContext)) {
            tvSSID.setText(WifiUtil.getSSID(mContext));
            tvIp.setText(WifiUtil.getLocalIPAddress());
            Intent intent = new Intent(mContext, IntercomService.class);
            bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        } else {
            //提示是否连接WiFi
            showConnectWifiDialog();
        }

        //当应用开始播放的时候首先需要请求焦点，调用该方法后，原先获取焦点的应用会释放焦点
        mAudioManager.requestAudioFocus(focusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        //对媒体播放按钮进行封装
        if (mComponentName == null) {
            mComponentName = new ComponentName(getPackageName(), MediaButtonReceiver.class.getName());
        }
        //注册封装的ComponentName
        mAudioManager.registerMediaButtonEventReceiver(mComponentName);

//        if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAudioManager.requestAudioFocus(focusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)) {
//            mAudioManager.registerMediaButtonEventReceiver(mComponentName);
//        }

    }

    /**
     * 显示连接Wifi的弹窗
     */
    private void showConnectWifiDialog() {
        if (commonWarningDialog == null) {
            commonWarningDialog = new CommonWarningDialog(mContext, getString(R.string.notification_connect_wifi));
            commonWarningDialog.setCancelable(false);
            commonWarningDialog.setOnDialogClickListener(new CommonWarningDialog.OnDialogClickListener() {
                @Override
                public void onOKClick() {
                    //进入WiFi连接页面
                    Intent wifiSettingsIntent = new Intent("android.settings.WIFI_SETTINGS");
                    startActivity(wifiSettingsIntent);
                }

                @Override
                public void onCancelClick() {
                    ActivityController.finishActivity(MainActivity.this);
                }
            });
        }
        if (!commonWarningDialog.isShowing()) {
            commonWarningDialog.show();
        }
    }

    /**
     * 取消显示连接Wifi的弹窗
     */
    private void dismissWifiDialog() {
        if (commonWarningDialog != null && commonWarningDialog.isShowing()) {
            commonWarningDialog.dismiss();
        }
    }

    /**
     * onServiceConnected和onServiceDisconnected运行在UI线程中
     */
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            intercomService = IIntercomService.Stub.asInterface(service);
            LogUtils.d(TAG, "绑定Service成功");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            intercomService = null;
        }
    };

    private View.OnClickListener onClickListener = (v) -> {
        switch (v.getId()) {
            case R.id.tvContacts:
                mViewpager.setCurrentItem(0);
                break;
            case R.id.tvWarning:
                mViewpager.setCurrentItem(1);
                break;
            case R.id.ivSetting:
                // 设置
                openActivity(SettingActivity.class);
                break;
            default:
                break;
        }
    };

    private View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                tvMessage.setText(getString(R.string.releaseFinish));
                vibrator.vibrate(50);
                if (intercomService != null) {
                    try {
                        if (SPHelper.getBoolean("CANT_SPEAK", true)) {
                            // 被标记为不能讲话
                            LogUtils.d(TAG, "你现在不能讲话");
                            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                                mediaPlayer.stop();
                                mediaPlayer.release();
                                mediaPlayer = null;
                            }
                            try {
                                mediaPlayer = MediaPlayer.create(mContext, R.raw.dududu);
                                mediaPlayer.setLooping(false);
                                mediaPlayer.start();
                                mediaPlayer.setVolume(0.1f, 0.1f);
                            } catch (IllegalStateException e) {
                                e.printStackTrace();
                            }
                        } else {
                            intercomService.startRecord();
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                v.performClick();
                tvMessage.setText(getString(R.string.pressToSpeak));
                vibrator.vibrate(50);
                if (intercomService != null) {
                    try {
                        intercomService.stopRecord();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
            return false;
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission Granted
            } else {
                showToast(R.string.microphonePermission);
            }
        }
    }

    //网络状态广播
    public class NetworkStatusReceiver extends BaseBroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 监听wifi的打开与关闭，与wifi的连接无关
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
                if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
                    //wifi关闭
                    LogUtils.d(TAG, "wifi已关闭");
                    // 清除TextView内容，弹出Dialog
                    tvIp.setText("");
                    tvMessage.setText("");
                    tvSSID.setText("");
                    showConnectWifiDialog();
                } else if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
                    //wifi开启
                    LogUtils.d(TAG, "wifi已开启");
                } else if (wifiState == WifiManager.WIFI_STATE_ENABLING) {
                    //wifi开启中
                    LogUtils.d(TAG, "wifi开启中");
                } else if (wifiState == WifiManager.WIFI_STATE_DISABLING) {
                    //wifi关闭中
                    LogUtils.d(TAG, "wifi关闭中");
                }
            }
            // 监听wifi的连接状态即是否连上了一个有效无线路由
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                Parcelable parcelableExtra = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (parcelableExtra != null) {
                    NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
                    if (networkInfo.getState() == NetworkInfo.State.CONNECTED) {
                        //已连接网络
                        LogUtils.d(TAG, "wifi 已连接网络");
                        if (networkInfo.isAvailable()) {//并且网络可用
                            LogUtils.d(TAG, "wifi 已连接网络，并且可用");
                        } else {//并且网络不可用
                            LogUtils.d(TAG, "wifi 已连接网络，但不可用");
                        }
                        // 取消显示Dialog
                        dismissWifiDialog();
                        // TextView显示网络信息
                        if (WifiUtil.WifiConnected(mContext)) {
                            tvSSID.setText(WifiUtil.getSSID(mContext));
                            tvIp.setText(WifiUtil.getLocalIPAddress());
                        }
                    } else {
                        //网络未连接
                        LogUtils.d(TAG, "wifi 未连接网络");
                    }
                } else {
                    LogUtils.d(TAG, "wifi parcelableExtra为空");
                }
            }
            // 监听网络连接，总网络判断，即包括wifi和移动网络的监听
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                NetworkInfo networkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                //连上的网络类型判断：wifi还是移动网络
                if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    LogUtils.d(TAG, "总网络 连接的是wifi网络");
                } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                    LogUtils.d(TAG, "总网络 连接的是移动网络");
                }
                //具体连接状态判断
                checkNetworkStatus(networkInfo);
            }
        }

        private void checkNetworkStatus(NetworkInfo networkInfo) {
            if (networkInfo != null) {
                LogUtils.d(TAG, "总网络 info非空");
                if (networkInfo.getState() == NetworkInfo.State.CONNECTED) {//已连接网络
                    LogUtils.d(TAG, "总网络 已连接网络");
                    if (networkInfo.isAvailable()) {//并且网络可用
                        LogUtils.d(TAG, "总网络 已连接网络，并且可用");
                    } else {//并且网络不可用
                        LogUtils.d(TAG, "总网络 已连接网络，但不可用");
                    }
                } else if (networkInfo.getState() == NetworkInfo.State.DISCONNECTED) {//网络未连接
                    LogUtils.d(TAG, "总网络 未连接网络");
                }
            } else {
                LogUtils.d(TAG, "总网络 info为空");
            }
        }
    }

    @Override
    public void onBackPressed() {
        // 发送离开群组消息
        try {
            intercomService.leaveGroup();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAudioManager.unregisterMediaButtonEventReceiver(mComponentName);
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        if (networkStatusReceiver != null) {
            mContext.unregisterReceiver(networkStatusReceiver);
        }
    }

}

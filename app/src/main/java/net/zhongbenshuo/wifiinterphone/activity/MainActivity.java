package net.zhongbenshuo.wifiinterphone.activity;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.support.v7.content.res.AppCompatResources;
import android.text.TextPaint;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.zhongbenshuo.wifiinterphone.broadcast.MediaButtonReceiver;
import net.zhongbenshuo.wifiinterphone.constant.Permission;
import net.zhongbenshuo.wifiinterphone.R;
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

    private MediaButtonReceiver mediaButtonReceiver;

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

        mediaButtonReceiver = new MediaButtonReceiver(this);
    }

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
            CommonWarningDialog commonWarningDialog = new CommonWarningDialog(mContext, getString(R.string.notification_connect_wifi));
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
            commonWarningDialog.show();
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
                        intercomService.startRecord();
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

//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if ((keyCode == KeyEvent.KEYCODE_F2 || keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
//            tvMessage.setText(getString(R.string.releaseFinish));
//            if (intercomService != null) {
//                try {
//                    intercomService.startRecord();
//                } catch (RemoteException e) {
//                    e.printStackTrace();
//                }
//            }
//            return true;
//        }
//        return super.onKeyDown(keyCode, event);
//    }
//
//    @Override
//    public boolean onKeyUp(int keyCode, KeyEvent event) {
//        if ((keyCode == KeyEvent.KEYCODE_F2 || keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
//            tvMessage.setText(getString(R.string.pressToSpeak));
//            if (intercomService != null) {
//                try {
//                    intercomService.stopRecord();
//                } catch (RemoteException e) {
//                    e.printStackTrace();
//                }
//            }
//            return true;
//        }
//        return super.onKeyUp(keyCode, event);
//    }

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
        mediaButtonReceiver.unregisterHeadsetReceiver();
        if (mediaButtonReceiver != null) {
            unregisterReceiver(mediaButtonReceiver);
        }
    }

}

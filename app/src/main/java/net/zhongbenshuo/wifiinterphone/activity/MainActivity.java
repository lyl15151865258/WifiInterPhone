package net.zhongbenshuo.wifiinterphone.activity;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
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

import net.zhongbenshuo.wifiinterphone.constant.Permission;
import net.zhongbenshuo.wifiinterphone.R;
import net.zhongbenshuo.wifiinterphone.constant.VoiceConstant;
import net.zhongbenshuo.wifiinterphone.fragment.ContactsFragment;
import net.zhongbenshuo.wifiinterphone.fragment.MalfunctionFragment;
import net.zhongbenshuo.wifiinterphone.service.VoiceService;
import net.zhongbenshuo.wifiinterphone.adapter.SelectModuleAdapter;
import net.zhongbenshuo.wifiinterphone.utils.ActivityController;
import net.zhongbenshuo.wifiinterphone.utils.IPUtil;
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

    private Context mContext;
    private TextView tvSSID, tvIp, tvMessage;
    private NoScrollViewPager mViewpager;
    private ImageView ivTitleIndicator;
    private List<TextView> textViews;
    private ServiceConnection serviceConnection;
    private VoiceService voiceService;
    private Vibrator vibrator;
    private final int REQUEST_PERMISSION = 1;

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
            tvIp.setText(IPUtil.getLocalIPAddress(mContext));

            Intent intent = new Intent(mContext, VoiceService.class);
            intent.putExtra("ip", IPUtil.getBroadcastIPAddress(mContext));
            intent.putExtra("port", 30000);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            if (voiceService == null) {
                bindVoiceService();
            }
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
     * 绑定service
     */
    private void bindVoiceService() {
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                VoiceService.VoiceServiceBinder binder = (VoiceService.VoiceServiceBinder) iBinder;
                voiceService = binder.getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                voiceService = null;
            }
        };
        Intent intent = new Intent(mContext, VoiceService.class);
        intent.putExtra("ip", IPUtil.getBroadcastIPAddress(mContext));
        intent.putExtra("port", VoiceConstant.PORT_BROADCAST);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

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
                if (voiceService != null) {
                    voiceService.setIsSending(true);
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                v.performClick();
                tvMessage.setText(getString(R.string.pressToSpeak));
                vibrator.vibrate(50);
                if (voiceService != null) {
                    voiceService.setIsSending(false);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceConnection != null) {
            unbindService(serviceConnection);
        }
    }
}

package net.zhongbenshuo.wifiinterphone.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import net.zhongbenshuo.wifiinterphone.R;
import net.zhongbenshuo.wifiinterphone.activity.chat.ChatRoomActivity;
import net.zhongbenshuo.wifiinterphone.activity.chat.EnterActivity;
import net.zhongbenshuo.wifiinterphone.contentprovider.SPHelper;
import net.zhongbenshuo.wifiinterphone.utils.ActivityController;
import net.zhongbenshuo.wifiinterphone.utils.ApkUtils;
import net.zhongbenshuo.wifiinterphone.widget.MyToolbar;

/**
 * 设置页面
 * Created at 2018/11/20 13:38
 *
 * @author LiYuliang
 * @version 1.0
 */

public class SettingActivity extends BaseActivity {

    private Context mContext;
    private TextView tvSendMethod, tvSetName, tvSetSpeakTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        mContext = this;
        MyToolbar toolbar = findViewById(R.id.myToolbar);
        toolbar.initToolBar(this, toolbar, getString(R.string.settings), R.drawable.back_white, onClickListener);
        findViewById(R.id.ll_serverSettings).setOnClickListener(onClickListener);
        findViewById(R.id.ll_setName).setOnClickListener(onClickListener);
        findViewById(R.id.ll_wifiSettings).setOnClickListener(onClickListener);
        findViewById(R.id.ll_languageSettings).setOnClickListener(onClickListener);
        findViewById(R.id.ll_setSpeakTime).setOnClickListener(onClickListener);
        findViewById(R.id.llSendMode).setOnClickListener(onClickListener);
        findViewById(R.id.btn_exit).setOnClickListener(onClickListener);
        ToggleButton toggleUseSpeakers = findViewById(R.id.toggle_useSpeakers);
        toggleUseSpeakers.setOnCheckedChangeListener(onCheckedChangeListener);
        ((TextView) findViewById(R.id.tvVersion)).setText(ApkUtils.getVersionName(mContext));
        tvSetName = findViewById(R.id.tvSetName);
        tvSetSpeakTime = findViewById(R.id.tvSetSpeakTime);
        tvSendMethod = findViewById(R.id.tvSendMethod);
    }

    @Override
    protected void onResume() {
        super.onResume();
        switch (SPHelper.getInt("broadcast", 0)) {
            case 0:
                tvSendMethod.setText(getString(R.string.send_wlan_unicast));
                break;
            case 1:
                tvSendMethod.setText(getString(R.string.send_wlan_broadcast));
                break;
            case 2:
                tvSendMethod.setText(getString(R.string.send_wan));
                break;
            default:
                tvSendMethod.setText("");
                break;
        }
        tvSetName.setText(SPHelper.getString("UserName", "Not Defined"));
        tvSetSpeakTime.setText(String.valueOf(SPHelper.getInt("SpeakTime", 30)));
    }

    private View.OnClickListener onClickListener = (v) -> {
        switch (v.getId()) {
            case R.id.iv_left:
                ActivityController.finishActivity(this);
                break;
            case R.id.ll_serverSettings:
                // 语音测试
//                openActivity(MediaTestActivity.class);
//                openActivity(EnterActivity.class);
                openActivity(ChatRoomActivity.class);
                break;
            case R.id.ll_setName:
                // 姓名修改
                openActivity(SetNameActivity.class);
                break;
            case R.id.ll_wifiSettings:
                // Wifi设置
                Intent wifiSettingsIntent = new Intent("android.settings.WIFI_SETTINGS");
                startActivity(wifiSettingsIntent);
                break;
            case R.id.ll_languageSettings:
                // 语言设置
                openActivity(LanguageActivity.class);
                break;
            case R.id.ll_setSpeakTime:
                // 设置单次说话时长
                openActivity(SetSpeakTimeActivity.class);
                break;
            case R.id.llSendMode:
                // 声音传输方式
                openActivity(SendMethodActivity.class);
                break;
            case R.id.btn_exit:
                // 退出程序
                ActivityController.exit();
                break;
            default:
                break;
        }
    };

    private CompoundButton.OnCheckedChangeListener onCheckedChangeListener = (buttonView, isChecked) -> {
        switch (buttonView.getId()) {
            case R.id.toggle_useSpeakers:
                //是否使用扬声器播放

                break;
            default:
                break;
        }
    };

}

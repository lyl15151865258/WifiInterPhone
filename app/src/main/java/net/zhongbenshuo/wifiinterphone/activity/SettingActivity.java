package net.zhongbenshuo.wifiinterphone.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import net.zhongbenshuo.wifiinterphone.R;
import net.zhongbenshuo.wifiinterphone.utils.ActivityController;
import net.zhongbenshuo.wifiinterphone.utils.SharedPreferencesUtil;
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
    private SharedPreferencesUtil sharedPreferencesUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        mContext = this;
        sharedPreferencesUtil = SharedPreferencesUtil.getInstance();
        MyToolbar toolbar = findViewById(R.id.myToolbar);
        toolbar.initToolBar(this, toolbar, getString(R.string.settings), R.drawable.back_white, onClickListener);
        findViewById(R.id.ll_serverSettings).setOnClickListener(onClickListener);
        findViewById(R.id.ll_wifiSettings).setOnClickListener(onClickListener);
        findViewById(R.id.ll_languageSettings).setOnClickListener(onClickListener);
        ((ToggleButton) findViewById(R.id.toggle_useSpeakers)).setOnCheckedChangeListener(onCheckedChangeListener);
    }

    private View.OnClickListener onClickListener = (v) -> {
        switch (v.getId()) {
            case R.id.iv_left:
                ActivityController.finishActivity(this);
                break;
            case R.id.ll_serverSettings:
                // 语音测试
                openActivity(VoiceTestActivity.class);
                break;
            case R.id.ll_wifiSettings:
                // Wifi设置
                Intent wifiSettingsIntent = new Intent("android.settings.WIFI_SETTINGS");
                startActivity(wifiSettingsIntent);
                break;
            case R.id.ll_languageSettings:
                // 语言设置
                openActivity(ChooseLanguageActivity.class);
                break;
            default:
                break;
        }
    };

    private CompoundButton.OnCheckedChangeListener onCheckedChangeListener = (buttonView, isChecked) -> {
        switch (buttonView.getId()) {
            case R.id.toggle_useSpeakers:
                //是否使用扬声器播放
                sharedPreferencesUtil.saveData(getString(R.string.userSpeakers), buttonView.isChecked());
                break;
            default:
                break;
        }
    };

}

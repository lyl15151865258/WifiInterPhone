package net.zhongbenshuo.wifiinterphone.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import net.zhongbenshuo.wifiinterphone.R;
import net.zhongbenshuo.wifiinterphone.utils.ActivityController;
import net.zhongbenshuo.wifiinterphone.voice.VoiceBuilder;
import net.zhongbenshuo.wifiinterphone.voice.VoicePlay;
import net.zhongbenshuo.wifiinterphone.voice.VoiceTextTemplate;
import net.zhongbenshuo.wifiinterphone.widget.MyToolbar;

/**
 * 语音测试页面
 * Created at 2018/11/28 13:34
 *
 * @author LiYuliang
 * @version 1.0
 */

public class MediaTestActivity extends BaseActivity {

    private boolean mCheckNum;
    private EditText editText;
    private TextView tvText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_test);
        MyToolbar toolbar = findViewById(R.id.myToolbar);
        toolbar.initToolBar(this, toolbar, getString(R.string.MediaTest), R.drawable.back_white, onClickListener);
        editText = findViewById(R.id.edittext);
        tvText = findViewById(R.id.tvText);
        findViewById(R.id.bt_play).setOnClickListener(onClickListener);
        findViewById(R.id.bt_del).setOnClickListener(onClickListener);
        ((Switch) findViewById(R.id.switch_view)).setOnCheckedChangeListener((compoundButton, b) -> mCheckNum = b);
    }

    private View.OnClickListener onClickListener = (v) -> {
        switch (v.getId()) {
            case R.id.iv_left:
                ActivityController.finishActivity(this);
                break;
            case R.id.bt_play:
                String amount = editText.getText().toString().trim();
                if (TextUtils.isEmpty(amount)) {
                    showToast("请输入金额");
                    return;
                }
                VoicePlay.with(MediaTestActivity.this).play(amount, mCheckNum);
                VoiceBuilder voiceBuilder = new VoiceBuilder.Builder()
                        .start("success")
                        .money(amount)
                        .unit("yuan")
                        .checkNum(mCheckNum)
                        .builder();
                StringBuffer text = new StringBuffer().append("输入金额: ").append(amount).append("\n");
                if (mCheckNum) {
                    text.append("全数字式: ").append(VoiceTextTemplate.genVoiceList(voiceBuilder).toString());
                } else {
                    text.append("中文样式: ").append(VoiceTextTemplate.genVoiceList(voiceBuilder).toString());
                }
                tvText.setText(text);
                editText.setText("");
                break;
            case R.id.bt_del:
                tvText.setText("");
                break;
            default:
                break;
        }
    };

}

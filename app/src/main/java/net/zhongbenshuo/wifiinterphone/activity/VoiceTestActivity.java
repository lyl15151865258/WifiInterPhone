package net.zhongbenshuo.wifiinterphone.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

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

public class VoiceTestActivity extends BaseActivity {
    private boolean mCheckNum;

    private EditText editText;
    private Button btPlay;
    private Button btDel;
    private LinearLayout llMoneyList;
    private Switch switchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_test);
        initView();
        initClick();
    }

    void initView() {
        MyToolbar toolbar = findViewById(R.id.myToolbar);
        toolbar.initToolBar(this, toolbar, getString(R.string.VoiceTest), R.drawable.back_white, onClickListener);
        editText = findViewById(R.id.edittext);
        btPlay = findViewById(R.id.bt_play);
        btDel = findViewById(R.id.bt_del);
        llMoneyList = findViewById(R.id.ll_money_list);
        switchView = findViewById(R.id.switch_view);
    }

    private View.OnClickListener onClickListener = (v) -> {
        switch (v.getId()) {
            case R.id.iv_left:
                ActivityController.finishActivity(this);
                break;
            default:
                break;
        }
    };

    void initClick() {
        btPlay.setOnClickListener(view -> {
            String amount = editText.getText().toString().trim();
            if (TextUtils.isEmpty(amount)) {
                Toast.makeText(VoiceTestActivity.this, "请输入金额", Toast.LENGTH_SHORT).show();
                return;
            }

            VoicePlay.with(VoiceTestActivity.this).play(amount, mCheckNum);

            llMoneyList.addView(getTextView(amount), 0);
            editText.setText("");
        });

        btDel.setOnClickListener(view -> llMoneyList.removeAllViews());

        switchView.setOnCheckedChangeListener((compoundButton, b) -> mCheckNum = b);
    }

    TextView getTextView(String amount) {
        VoiceBuilder voiceBuilder = new VoiceBuilder.Builder()
                .start("success")
                .money(amount)
                .unit("yuan")
                .checkNum(mCheckNum)
                .builder();

        StringBuffer text = new StringBuffer()
                .append("角标: ").append(llMoneyList.getChildCount())
                .append("\n")
                .append("输入金额: ").append(amount)
                .append("\n");
        if (mCheckNum) {
            text.append("全数字式: ").append(VoiceTextTemplate.genVoiceList(voiceBuilder).toString());
        } else {
            text.append("中文样式: ").append(VoiceTextTemplate.genVoiceList(voiceBuilder).toString());
        }

        TextView view = new TextView(VoiceTestActivity.this);
        view.setPadding(0, 8, 0, 0);
        view.setText(text.toString());
        return view;
    }
}

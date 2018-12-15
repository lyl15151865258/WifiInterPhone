package net.zhongbenshuo.wifiinterphone.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import net.zhongbenshuo.wifiinterphone.R;
import net.zhongbenshuo.wifiinterphone.contentprovider.SPHelper;
import net.zhongbenshuo.wifiinterphone.utils.ActivityController;
import net.zhongbenshuo.wifiinterphone.widget.MyToolbar;

/**
 * 单次说话时间设置
 * Created at 2018-12-15 15:05
 *
 * @author LiYuliang
 * @version 1.0
 */

public class SetSpeakTimeActivity extends BaseActivity {

    private EditText etSpeakTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_speak_time);
        MyToolbar toolbar = findViewById(R.id.myToolbar);
        toolbar.initToolBar(this, toolbar, getString(R.string.SetSpeakTime), R.drawable.back_white, onClickListener);
        Button btnModify = findViewById(R.id.btn_modify);
        btnModify.setOnClickListener(onClickListener);
        etSpeakTime = findViewById(R.id.etSpeakTime);
        etSpeakTime.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (TextUtils.isEmpty(charSequence.toString())) {
                    btnModify.setEnabled(false);
                } else {
                    btnModify.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        etSpeakTime.setText(String.valueOf(SPHelper.getInt("SpeakTime", 30)));
    }

    private View.OnClickListener onClickListener = (v) -> {
        switch (v.getId()) {
            case R.id.iv_left:
                ActivityController.finishActivity(this);
                break;
            case R.id.btn_modify:
                SPHelper.save("SpeakTime", Integer.valueOf(etSpeakTime.getText().toString()));
                ActivityController.finishActivity(this);
                break;
            default:
                break;
        }
    };

}

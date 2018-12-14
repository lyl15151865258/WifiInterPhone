package net.zhongbenshuo.wifiinterphone.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;

import net.zhongbenshuo.wifiinterphone.R;
import net.zhongbenshuo.wifiinterphone.contentprovider.SPHelper;
import net.zhongbenshuo.wifiinterphone.utils.ActivityController;
import net.zhongbenshuo.wifiinterphone.widget.MyToolbar;

/**
 * 设置默认导航软件
 * Created at 2018/7/13 0013 11:38
 *
 * @author LiYuliang
 * @version 1.0
 */

public class SendMethodActivity extends BaseActivity {

    private CheckBox cbUnicast, cbBroadcast, cbWan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default_send_method);
        MyToolbar toolbar = findViewById(R.id.myToolbar);
        toolbar.initToolBar(this, toolbar, getString(R.string.SendMode), R.drawable.back_white, onClickListener);
        cbUnicast = findViewById(R.id.cbUnicast);
        cbBroadcast = findViewById(R.id.cbBroadcast);
        cbWan = findViewById(R.id.cbWan);
        switch (SPHelper.getInt("broadcast", 0)) {
            case 0:
                cbUnicast.setChecked(true);
                cbBroadcast.setChecked(false);
                cbWan.setChecked(false);
                break;
            case 1:
                cbUnicast.setChecked(false);
                cbBroadcast.setChecked(true);
                cbWan.setChecked(false);
                break;
            case 2:
                cbUnicast.setChecked(false);
                cbBroadcast.setChecked(false);
                cbWan.setChecked(true);
                break;
            default:
                break;
        }
        cbUnicast.setOnClickListener(onClickListener);
        cbBroadcast.setOnClickListener(onClickListener);
        cbWan.setOnClickListener(onClickListener);
    }

    private View.OnClickListener onClickListener = (v) -> {
        switch (v.getId()) {
            case R.id.iv_left:
                ActivityController.finishActivity(this);
                break;
            case R.id.cbUnicast:
                cbUnicast.setChecked(true);
                cbBroadcast.setChecked(false);
                cbWan.setChecked(false);
                SPHelper.save("broadcast", 0);
                break;
            case R.id.cbBroadcast:
                cbUnicast.setChecked(false);
                cbBroadcast.setChecked(true);
                cbWan.setChecked(false);
                SPHelper.save("broadcast", 1);
                break;
            case R.id.cbWan:
                cbUnicast.setChecked(false);
                cbBroadcast.setChecked(false);
                cbWan.setChecked(true);
                SPHelper.save("broadcast", 2);
                break;
            default:
                break;
        }
    };

}

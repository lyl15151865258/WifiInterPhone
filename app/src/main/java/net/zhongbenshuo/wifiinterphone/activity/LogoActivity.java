package net.zhongbenshuo.wifiinterphone.activity;

import android.os.Bundle;

import net.zhongbenshuo.wifiinterphone.R;
import net.zhongbenshuo.wifiinterphone.utils.ActivityController;

/**
 * Logo页面
 * Created at 2018/11/20 13:37
 *
 * @author LiYuliang
 * @version 1.0
 */

public class LogoActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logo);
        try {
            Thread.sleep(1500);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            openActivity(MainActivity.class);
            ActivityController.finishActivity(this);
        }
    }

    /**
     * Logo页面不允许退出
     */
    @Override
    public void onBackPressed() {

    }

}

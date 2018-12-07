package net.zhongbenshuo.wifiinterphone.service;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import net.zhongbenshuo.wifiinterphone.utils.LogUtils;

/**
 * 按键监听Service（无障碍辅助功能）
 * Created at 2018/12/6 16:57
 *
 * @author LiYuliang
 * @version 1.0
 */

public class KeyEventService extends AccessibilityService {

    private static final String TAG = "KeyEventService";

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        LogUtils.d(TAG, "onKeyEvent：" + event.getKeyCode());
        if ((event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            Intent intent = new Intent();
            switch (event.getAction()) {
                case KeyEvent.ACTION_DOWN:
                    intent.setAction("KEY_DOWN");
                    sendBroadcast(intent);
                    LogUtils.d(TAG, "按下");
                    break;
                case KeyEvent.ACTION_UP:
                    intent.setAction("KEY_UP");
                    sendBroadcast(intent);
                    LogUtils.d(TAG, "抬起");
                    break;
                default:
                    break;
            }
            return true;
        }
        return super.onKeyEvent(event);
    }

    @Override
    public void onInterrupt() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

    }

}

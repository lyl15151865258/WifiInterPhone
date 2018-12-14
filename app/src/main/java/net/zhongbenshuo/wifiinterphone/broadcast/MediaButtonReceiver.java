package net.zhongbenshuo.wifiinterphone.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

import net.zhongbenshuo.wifiinterphone.utils.LogUtils;

public class MediaButtonReceiver extends BroadcastReceiver {

    private static final String TAG = "MediaButtonReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            KeyEvent keyEvent = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

            LogUtils.d(TAG, "KeyEvent----->" + keyEvent.getKeyCode() + "，KeyAction----->" + keyEvent.getAction());

            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_HEADSETHOOK) {

            } else if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_MEDIA_NEXT) {

            } else if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {

            }
        }
    }
}
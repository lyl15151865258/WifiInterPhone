package net.zhongbenshuo.wifiinterphone.broadcast;

import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

import net.zhongbenshuo.wifiinterphone.utils.LogUtils;

public class MediaButtonReceiver extends BaseBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();
        LogUtils.d("MediaButtonReceiver", "收到按键事件");
        if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null) {
                return;
            }

            int keycode = event.getKeyCode();
            int action = event.getAction();
            long eventtime = event.getEventTime();

            //按键处理逻辑

            // single quick press: pause/resume.
            // double press: next track
            // long press: start auto-shuffle mode.

            String command = null;
            switch (keycode) {
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    //耳机中间按键，可作为来电接听键，还可以作为音乐 播放/暂停 切换按键。
                    if (action == KeyEvent.ACTION_DOWN) {
                        LogUtils.d("MediaButtonReceiver", "按键按下");
                    }
                    if (action == KeyEvent.ACTION_UP) {
                        LogUtils.d("MediaButtonReceiver", "按键抬起");
                    }
                    break;
                default:
                    break;
            }

        }
    }
}

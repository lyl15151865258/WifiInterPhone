package net.zhongbenshuo.wifiinterphone.broadcast;

import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * 监听Wifi信号强度
 * Created at 2018/11/20 13:41
 *
 * @author LiYuliang
 * @version 1.0
 */

public class WifiReceiver extends BaseBroadcastReceiver {
    private static final String TAG = "wifiReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (WifiManager.RSSI_CHANGED_ACTION.equals(intent.getAction())) {
            Log.i(TAG, "wifi信号强度变化");
        }
        //wifi连接上与否
        if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (info.getState().equals(NetworkInfo.State.DISCONNECTED)) {
                showToast("wifi断开");
            } else if (info.getState().equals(NetworkInfo.State.CONNECTED)) {
                WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                //获取当前wifi名称
                showToast("连接到网络 " + wifiInfo.getSSID());
            }
        }
        //wifi打开与否
        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            int wifistate = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED);
            if (wifistate == WifiManager.WIFI_STATE_DISABLED) {
                showToast("系统关闭wifi");
            } else if (wifistate == WifiManager.WIFI_STATE_ENABLED) {
                showToast("系统开启wifi");
            }
        }
    }
}

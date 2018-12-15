package net.zhongbenshuo.wifiinterphone.job;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;

import net.zhongbenshuo.wifiinterphone.WifiInterPhoneApplication;
import net.zhongbenshuo.wifiinterphone.broadcast.BaseBroadcastReceiver;
import net.zhongbenshuo.wifiinterphone.constant.Constants;
import net.zhongbenshuo.wifiinterphone.data.AudioData;
import net.zhongbenshuo.wifiinterphone.data.MessageQueue;
import net.zhongbenshuo.wifiinterphone.network.wlan.Unicast;
import net.zhongbenshuo.wifiinterphone.utils.LogUtils;
import net.zhongbenshuo.wifiinterphone.utils.WifiUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * 单播接收线程
 * Created at 2018/12/12 13:04
 *
 * @author LiYuliang
 * @version 1.0
 */

public class UnicastSender extends JobHandler {

    private MyReceiver myReceiver;
    private List<String> ipList = new ArrayList<>();

    public UnicastSender(Handler handler) {
        super(handler);
        myReceiver = new MyReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("CHANGE_SEND_IP");
        WifiInterPhoneApplication.getInstance().registerReceiver(myReceiver, filter);
    }

    @Override
    public void run() {
        AudioData audioData;
        while ((audioData = MessageQueue.getInstance(MessageQueue.SENDER_DATA_QUEUE_UNICAST).take()) != null) {
            try {
                LogUtils.d("UnicastSender", ipList.get(0));
                for (int i = 0; i < ipList.size(); i++) {
                    // 往除了本机IP以外的IP发送
                    if (!ipList.get(i).equals(WifiUtil.getLocalIPAddress())) {
                        DatagramPacket datagramPacket = new DatagramPacket(
                                audioData.getEncodedData(), audioData.getEncodedData().length,
                                InetAddress.getByName(ipList.get(i)), Constants.UNICAST_PORT);
                        Unicast.getUnicast().getUnicastDatagramSocket().send(datagramPacket);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void free() {
        Unicast.getUnicast().free();
        if (myReceiver != null) {
            WifiInterPhoneApplication.getInstance().unregisterReceiver(myReceiver);
        }
    }

    public class MyReceiver extends BaseBroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("CHANGE_SEND_IP".equals(intent.getAction())) {
                ipList.clear();
                ipList.addAll(intent.getStringArrayListExtra("IP"));
                LogUtils.d("UnicastSender", "收到广播，列表长度为：" + ipList.size());
            }
        }
    }
}

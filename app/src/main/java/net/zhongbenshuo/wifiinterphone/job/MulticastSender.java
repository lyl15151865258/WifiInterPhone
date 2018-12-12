package net.zhongbenshuo.wifiinterphone.job;

import android.os.Handler;

import net.zhongbenshuo.wifiinterphone.constant.Constants;
import net.zhongbenshuo.wifiinterphone.data.AudioData;
import net.zhongbenshuo.wifiinterphone.data.MessageQueue;
import net.zhongbenshuo.wifiinterphone.network.wlan.Multicast;

import java.io.IOException;
import java.net.DatagramPacket;

/**
 * 组播发送线程
 * Created at 2018/12/12 13:03
 *
 * @author LiYuliang
 * @version 1.0
 */

public class MulticastSender extends JobHandler {

    public MulticastSender(Handler handler) {
        super(handler);
    }

    @Override
    public void run() {
        AudioData audioData;
        while ((audioData = MessageQueue.getInstance(MessageQueue.SENDER_DATA_QUEUE_BROADCAST).take()) != null) {
            DatagramPacket datagramPacket = new DatagramPacket(
                    audioData.getEncodedData(), audioData.getEncodedData().length,
                    Multicast.getMulticast().getInetAddress(), Constants.MULTI_BROADCAST_PORT);
            try {
                Multicast.getMulticast().getMulticastSocket().send(datagramPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void free() {
        Multicast.getMulticast().free();
    }
}

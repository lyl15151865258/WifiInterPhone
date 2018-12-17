package net.zhongbenshuo.wifiinterphone.job;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import net.zhongbenshuo.wifiinterphone.constant.Command;
import net.zhongbenshuo.wifiinterphone.constant.Constants;
import net.zhongbenshuo.wifiinterphone.contentprovider.SPHelper;
import net.zhongbenshuo.wifiinterphone.data.AudioData;
import net.zhongbenshuo.wifiinterphone.data.MessageQueue;
import net.zhongbenshuo.wifiinterphone.network.wlan.Unicast;
import net.zhongbenshuo.wifiinterphone.service.IntercomService;
import net.zhongbenshuo.wifiinterphone.speex.Speex;
import net.zhongbenshuo.wifiinterphone.utils.LogUtils;
import net.zhongbenshuo.wifiinterphone.utils.WifiUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Arrays;

/**
 * 单播接收线程
 * Created at 2018/12/12 13:04
 *
 * @author LiYuliang
 * @version 1.0
 */

public class UnicastReceiver extends JobHandler {

    private boolean flag = true;

    public UnicastReceiver(Handler handler) {
        super(handler);
    }

    @Override
    public void run() {
        while (flag) {
            // 设置接收缓冲段
            byte[] receivedData = new byte[Speex.getInstance().getFrameSize()];
            DatagramPacket datagramPacket = new DatagramPacket(receivedData, 0, receivedData.length);
            try {
                // 接收数据报文
                Unicast.getUnicast().getUnicastDatagramSocket().receive(datagramPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // 判断数据报文类型，并做相应处理
            String content = new String(datagramPacket.getData()).trim();
            if (content.startsWith(Command.DISC_REQUEST) || content.startsWith(Command.DISC_LEAVE) || content.startsWith(Command.DISC_RESPONSE)) {
                handleCommandData(datagramPacket);
            } else {
                handleAudioData(datagramPacket);
            }
        }
    }

    /**
     * 处理命令数据
     *
     * @param packet 命令数据包
     */
    private void handleCommandData(DatagramPacket packet) {
        String content = new String(packet.getData()).trim();
        LogUtils.d("PackageContent", "UnicastReceiver:" + content);
        if (content.startsWith(Command.DISC_REQUEST) &&
                !packet.getAddress().toString().equals("/" + WifiUtil.getLocalIPAddress())) {
            byte[] feedback = (Command.DISC_RESPONSE + "," + SPHelper.getString("UserName", "")).getBytes();
            // 发送数据
            DatagramPacket sendPacket = new DatagramPacket(feedback, feedback.length,
                    packet.getAddress(), Constants.UNICAST_PORT);
            try {
                Unicast.getUnicast().getUnicastDatagramSocket().send(sendPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
            String name = content.split(",")[1];
            // 发送Handler消息
            sendMsg2MainThread(packet.getAddress().toString(), name, IntercomService.DISCOVERING_RECEIVE);
        } else if (content.startsWith(Command.DISC_RESPONSE) &&
                !packet.getAddress().toString().equals("/" + WifiUtil.getLocalIPAddress())) {
            // 发送Handler消息
            String name = content.split(",")[1];
            sendMsg2MainThread(packet.getAddress().toString(), name, IntercomService.DISCOVERING_RECEIVE);
        } else if (content.startsWith(Command.DISC_LEAVE) &&
                !packet.getAddress().toString().equals("/" + WifiUtil.getLocalIPAddress())) {
            sendMsg2MainThread(packet.getAddress().toString(), "", IntercomService.DISCOVERING_LEAVE);
        }
    }

    /**
     * 处理音频数据
     *
     * @param packet 音频数据包
     */
    private void handleAudioData(DatagramPacket packet) {
        byte[] encodedData = Arrays.copyOf(packet.getData(), packet.getLength());
        AudioData audioData = new AudioData(encodedData, packet.getAddress().toString().replace("/", ""));
        MessageQueue.getInstance(MessageQueue.DECODER_DATA_QUEUE).put(audioData);
    }

    /**
     * 发送Handler消息
     *
     * @param address IP地址
     * @param name    姓名
     */
    private void sendMsg2MainThread(String address, String name, int msgWhat) {
        Message msg = new Message();
        msg.what = msgWhat;
        Bundle bundle = new Bundle();
        bundle.putString("address", address);
        bundle.putString("name", name);
        msg.setData(bundle);
        handler.sendMessage(msg);
    }

    @Override
    public void free() {
        flag = false;
        Unicast.getUnicast().free();
    }
}

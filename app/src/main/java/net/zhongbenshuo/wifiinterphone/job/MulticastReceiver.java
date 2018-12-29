package net.zhongbenshuo.wifiinterphone.job;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import net.zhongbenshuo.wifiinterphone.WifiInterPhoneApplication;
import net.zhongbenshuo.wifiinterphone.constant.Command;
import net.zhongbenshuo.wifiinterphone.constant.Constants;
import net.zhongbenshuo.wifiinterphone.contentprovider.SPHelper;
import net.zhongbenshuo.wifiinterphone.data.AudioData;
import net.zhongbenshuo.wifiinterphone.data.MessageQueue;
import net.zhongbenshuo.wifiinterphone.network.wlan.Multicast;
import net.zhongbenshuo.wifiinterphone.service.VoiceService;
import net.zhongbenshuo.wifiinterphone.speex.Speex;
import net.zhongbenshuo.wifiinterphone.utils.LogUtils;
import net.zhongbenshuo.wifiinterphone.utils.WifiUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 组播接收线程
 * Created at 2018/12/12 13:03
 *
 * @author LiYuliang
 * @version 1.0
 */

public class MulticastReceiver extends JobHandler {

    private final String TAG = "MulticastReceiver";

    private Map<String, Boolean> speakStatusMap;

    private boolean flag = true;

    public MulticastReceiver(Handler handler) {
        super(handler);
    }

    @Override
    public void run() {
        speakStatusMap = new HashMap<>();
        while (flag) {
            // 设置接收缓冲段
            byte[] receivedData = new byte[Speex.getInstance().getFrameSize()];
            DatagramPacket datagramPacket = new DatagramPacket(receivedData, receivedData.length);
            try {
                // 接收数据报文
                Multicast.getMulticast().getMulticastSocket().receive(datagramPacket);
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
        LogUtils.d("PackageContent", "MulticastReceiver:" + content);
        if (content.startsWith(Command.DISC_REQUEST) &&
                !packet.getAddress().toString().equals("/" + WifiUtil.getLocalIPAddress())) {
            byte[] feedback = (Command.DISC_RESPONSE + ","
                    + SPHelper.getString("UserName", "Not Defined") + ","
                    + SPHelper.getString("SpeakStatus", "0")).getBytes();
            // 发送数据
            DatagramPacket sendPacket = new DatagramPacket(feedback, feedback.length,
                    packet.getAddress(), Constants.MULTI_BROADCAST_PORT);
            try {
                Multicast.getMulticast().getMulticastSocket().send(sendPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
            String name = content.split(",")[1];
            // 发送Handler消息
            sendMsg2MainThread(packet.getAddress().toString(), name, VoiceService.DISCOVERING_RECEIVE);
        } else if (content.startsWith(Command.DISC_RESPONSE) &&
                !packet.getAddress().toString().equals("/" + WifiUtil.getLocalIPAddress())) {
            String ip = packet.getAddress().toString().split("[.]")[3];
            String name = content.split(",")[1];
            String speakStatus = content.split(",")[2];

            AudioManager audioManager = (AudioManager) WifiInterPhoneApplication.getInstance().getSystemService(Context.AUDIO_SERVICE);
            int defaultVolume = SPHelper.getInt("defaultVolume", audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            if ("0".equals(speakStatus)) {
                // 这个IP的用户没有说话
                speakStatusMap.put(ip, false);
                // 判断是不是所有人都不讲话了
                boolean someoneSpeaking = false;
                for (Map.Entry<String, Boolean> entry : speakStatusMap.entrySet()) {
                    LogUtils.d(TAG, "Key = " + entry.getKey() + ", Value = " + entry.getValue());
                    if (entry.getValue()) {
                        someoneSpeaking = true;
                    }
                }
                SPHelper.save("SomeoneSpeaking", someoneSpeaking);
                // 如果所有人都不讲话了
                if (!someoneSpeaking) {
                    LogUtils.d(TAG, "没有人讲话，设置音量值：" + defaultVolume);
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, defaultVolume, AudioManager.FLAG_VIBRATE);
                }
            } else {
                // 这个IP的用户正在说话
                speakStatusMap.put(ip, true);
                LogUtils.d(TAG, "有人讲话，设置音量值：" + defaultVolume / 2);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, defaultVolume / 2, AudioManager.FLAG_VIBRATE);
                SPHelper.save("SomeoneSpeaking", true);
            }
            // 发送Handler消息
            sendMsg2MainThread(packet.getAddress().toString(), name, VoiceService.DISCOVERING_RECEIVE);
        } else if (content.startsWith(Command.DISC_LEAVE) &&
                !packet.getAddress().toString().equals("/" + WifiUtil.getLocalIPAddress())) {
            sendMsg2MainThread(packet.getAddress().toString(), "", VoiceService.DISCOVERING_LEAVE);
        }
    }

    /**
     * 处理音频数据
     *
     * @param packet 音频数据包
     */
    private void handleAudioData(DatagramPacket packet) {
        byte[] encodedData = Arrays.copyOf(packet.getData(), packet.getLength());
        if (packet.getAddress() != null) {
            AudioData audioData = new AudioData(encodedData, packet.getAddress().toString().replace("/", ""));
            MessageQueue.getInstance(MessageQueue.DECODER_DATA_QUEUE).put(audioData);
        }
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
        Multicast.getMulticast().free();
    }
}

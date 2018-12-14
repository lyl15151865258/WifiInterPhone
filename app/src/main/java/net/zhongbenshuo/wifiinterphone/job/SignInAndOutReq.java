package net.zhongbenshuo.wifiinterphone.job;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import net.zhongbenshuo.wifiinterphone.constant.Command;
import net.zhongbenshuo.wifiinterphone.constant.Constants;
import net.zhongbenshuo.wifiinterphone.network.wlan.Multicast;
import net.zhongbenshuo.wifiinterphone.service.IntercomService;
import net.zhongbenshuo.wifiinterphone.utils.LogUtils;

import java.io.IOException;
import java.net.DatagramPacket;

/**
 * 发送进入和离开消息的线程
 * Created at 2018/12/12 13:05
 *
 * @author LiYuliang
 * @version 1.0
 */

public class SignInAndOutReq extends JobHandler {

    private String command;

    public SignInAndOutReq(Handler handler) {
        super(handler);
    }

    public void setCommand(String command) {
        this.command = command;
    }

    @Override
    public void run() {
        if (command != null) {
            byte[] data = command.getBytes();
            DatagramPacket datagramPacket = new DatagramPacket(
                    data, data.length, Multicast.getMulticast().getInetAddress(), Constants.MULTI_BROADCAST_PORT);
            try {
                Multicast.getMulticast().getMulticastSocket().send(datagramPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (command.startsWith(Command.DISC_REQUEST)) {
                LogUtils.d("PackageContent", "SignInAndOutReq:" + command);
                String name = command.split(",")[1];
                Bundle bundle = new Bundle();
                bundle.putString("address", datagramPacket.getAddress().toString());
                bundle.putString("name", name);
                sendMsg2MainThread(bundle);
            } else if (command.equals(Command.DISC_LEAVE)) {
                setCommand(Command.DISC_REQUEST);
            }
        }
    }

    /**
     * 发送消息到主线程
     */
    private void sendMsg2MainThread(Bundle bundle) {
        Message message = new Message();
        message.what = IntercomService.DISCOVERING_SEND;
        message.setData(bundle);
        handler.sendMessage(message);
    }

}

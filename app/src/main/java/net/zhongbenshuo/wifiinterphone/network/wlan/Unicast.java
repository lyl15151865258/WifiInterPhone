package net.zhongbenshuo.wifiinterphone.network.wlan;

import net.zhongbenshuo.wifiinterphone.constant.Constants;

import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Created by yanghao1 on 2017/5/15.
 */

public class Unicast {

    private DatagramSocket datagramSocket;

    private static final Unicast unicast = new Unicast();

    private Unicast() {
        try {
            // 初始化接收Socket
            datagramSocket = new DatagramSocket(Constants.UNICAST_PORT);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public static Unicast getUnicast() {
        return unicast;
    }

    public DatagramSocket getUnicastDatagramSocket() {
        return datagramSocket;
    }

    public void free() {
        if (datagramSocket != null) {
            datagramSocket.close();
            datagramSocket = null;
        }
    }
}

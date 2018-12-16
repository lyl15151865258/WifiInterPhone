package net.zhongbenshuo.wifiinterphone.job;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import net.zhongbenshuo.wifiinterphone.utils.ActivityController;
import net.zhongbenshuo.wifiinterphone.utils.LogUtils;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class WebSocket extends JobHandler {

    private final static String TAG = "WebSocketService";

    private WebSocketClient mSocketClient;
    private String serverHost, webSocketPort;

    public WebSocket(Handler handler, String serverHost, String webSocketPort) {
        super(handler);
        this.serverHost = serverHost;
        this.webSocketPort = webSocketPort;
    }

    @Override
    public void run() {
        try {
            mSocketClient = new WebSocketClient(new URI("ws://" + serverHost + ":" + webSocketPort), new Draft_6455()) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    //通道打开
                    LogUtils.d(TAG, "建立连接");
                }

                @Override
                public void onMessage(String message) {
                    LogUtils.d(TAG, message);
                    //判断当前栈顶Activity，再判断数据类型，决定是否需要发送数据
                    AppCompatActivity currentActivity = (AppCompatActivity) ActivityController.getInstance().getCurrentActivity();

                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    //通道关闭

                    LogUtils.d(TAG, "连接关闭");
                }

                @Override
                public void onError(Exception ex) {
                    //发生错误

                    LogUtils.d(TAG, "发生错误");
                }
            };
            mSocketClient.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭WebSocket
     */
    public void closeWebSocket() {
        if (mSocketClient != null) {
            if (mSocketClient.isOpen()) {
                mSocketClient.close();
            }
            mSocketClient = null;
        }
    }

    /**
     * WebSocket发送消息
     *
     * @param msg 需要发送的信息
     */
    public void sendMessage(String msg) {
        if (mSocketClient != null) {
            mSocketClient.send(msg);
        }
    }

    public boolean isOpen() {
        return mSocketClient != null && mSocketClient.isOpen();
    }

    @Override
    public void free() {

    }
}

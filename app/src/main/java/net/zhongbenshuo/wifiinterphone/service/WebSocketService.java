package net.zhongbenshuo.wifiinterphone.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;

import net.zhongbenshuo.wifiinterphone.bean.WebsocketMsg;
import net.zhongbenshuo.wifiinterphone.utils.ActivityController;
import net.zhongbenshuo.wifiinterphone.utils.GsonUtils;
import net.zhongbenshuo.wifiinterphone.utils.LogUtils;
import net.zhongbenshuo.wifiinterphone.voice.MusicPlay;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 接收服务器异常信息的WebSocket
 * Created at 2018-12-17 13:50
 *
 * @author LiYuliang
 * @version 1.0
 */

public class WebSocketService extends Service {

    private final static String TAG = "WebSocketService";
    private WebSocketServiceBinder webSocketServiceBinder;

    private WebSocketClient mSocketClient;
    private String serverHost, webSocketPort;

    private Runnable runnable;
    private ExecutorService threadPool;

    @Override
    public IBinder onBind(Intent intent) {
        return webSocketServiceBinder;
    }

    public class WebSocketServiceBinder extends Binder {
        /**
         * WebSocketServiceBinder
         *
         * @return SocketService对象
         */
        public WebSocketService getService() {
            return WebSocketService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        webSocketServiceBinder = new WebSocketServiceBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            serverHost = intent.getStringExtra("ServerHost");
            webSocketPort = intent.getStringExtra("WebSocketPort");
        }
        initWebSocket();
        return START_STICKY;
    }

    public boolean isOpen() {
        return mSocketClient != null && mSocketClient.isOpen();
    }

    /**
     * 初始化并启动启动WebSocket
     */
    public void initWebSocket() {
        threadPool = Executors.newScheduledThreadPool(1);
        runnable = () -> {
            try {
                if (mSocketClient != null) {
                    try {
                        if (mSocketClient.isOpen()) {
                            mSocketClient.close();
                        }
                        mSocketClient = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
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


                        WebsocketMsg websocketMsg = GsonUtils.parseJSON(message, WebsocketMsg.class);
                        List<String> voiceList = websocketMsg.getTitle();
                        String serverAddress = websocketMsg.getServerAddress();
                        if (voiceList != null && voiceList.size() > 0) {
                            if (websocketMsg.getPlayCount() > 0) {
                                for (int i = 0; i < websocketMsg.getPlayCount(); i++) {
                                    LogUtils.d(TAG, "第" + i + "次循环");
                                    List<String> newPathList = new ArrayList<>();
                                    for (String voiceName : voiceList) {
                                        LogUtils.d(TAG, "音乐名称：" + voiceName);
                                        newPathList.add("http://" + serverAddress + "/andonvoicedata/01_Japanese/" + voiceName);
                                    }
                                    MusicPlay.with(WebSocketService.this).play(newPathList);
                                }
                            }
                        }
                    }

                    @Override
                    public void onClose(int code, String reason, boolean remote) {
                        //通道关闭
                        LogUtils.d(TAG, "连接关闭");
                        reConnect();
                    }

                    @Override
                    public void onError(Exception ex) {
                        //发生错误
                        LogUtils.d(TAG, "发生错误");
//                        reConnect();
                    }
                };
                mSocketClient.connect();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        };
        threadPool.execute(runnable);
    }

    /**
     * 重连WebSocket
     */
    private void reConnect() {
        threadPool.shutdown();
        if (runnable != null) {
            runnable = null;
        }
        // 等待3秒再重连
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            initWebSocket();
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
        if (isOpen()) {
            mSocketClient.send(msg);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeWebSocket();
        threadPool.shutdown();
    }

}
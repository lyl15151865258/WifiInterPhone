package net.zhongbenshuo.wifiinterphone.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import net.zhongbenshuo.wifiinterphone.R;
import net.zhongbenshuo.wifiinterphone.activity.MainActivity;
import net.zhongbenshuo.wifiinterphone.broadcast.BaseBroadcastReceiver;
import net.zhongbenshuo.wifiinterphone.constant.Command;
import net.zhongbenshuo.wifiinterphone.contentprovider.SPHelper;
import net.zhongbenshuo.wifiinterphone.job.Decoder;
import net.zhongbenshuo.wifiinterphone.job.Encoder;
import net.zhongbenshuo.wifiinterphone.job.MulticastReceiver;
import net.zhongbenshuo.wifiinterphone.job.Recorder;
import net.zhongbenshuo.wifiinterphone.job.MulticastSender;
import net.zhongbenshuo.wifiinterphone.job.SignInAndOutReq;
import net.zhongbenshuo.wifiinterphone.job.Tracker;
import net.zhongbenshuo.wifiinterphone.job.UnicastReceiver;
import net.zhongbenshuo.wifiinterphone.job.UnicastSender;
import net.zhongbenshuo.wifiinterphone.utils.LogUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 局域网通信服务
 * Created at 2018/12/12 13:06
 *
 * @author LiYuliang
 * @version 1.0
 */

public class IntercomService extends Service {

    private final static String TAG = "IntercomService";

    // 创建循环任务线程用于间隔的发送上线消息，获取局域网内其他的用户
    private ScheduledExecutorService discoverService = Executors.newScheduledThreadPool(1);
    // 创建8个线程的固定大小线程池，分别执行DiscoverServer，以及输入、输出音频
    private ExecutorService threadPool = Executors.newCachedThreadPool();

    // 加入、退出组播组消息
    private SignInAndOutReq signInAndOutReq;

    // 音频输入
    private Recorder recorder;
    private Encoder encoder;
    private MulticastSender multicastSender;
    private UnicastSender unicastSender;

    // 音频输出
    private MulticastReceiver multicastReceiver;
    private UnicastReceiver unicastReceiver;
    private Decoder decoder;
    private Tracker tracker;

    public static final int DISCOVERING_SEND = 0;
    public static final int DISCOVERING_RECEIVE = 1;
    public static final int DISCOVERING_LEAVE = 2;

    private KeyEventBroadcastReceiver keyEventBroadcastReceiver;

    private MyHandler handler = new MyHandler(this);

    /**
     * Service与Runnable的通信
     */
    private static class MyHandler extends Handler {

        private IntercomService service;

        private MyHandler(IntercomService service) {
            this.service = service;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == DISCOVERING_SEND) {
                Log.i("IntercomService", "发送消息");
            } else if (msg.what == DISCOVERING_RECEIVE) {
                Bundle bundle = msg.getData();
                String address = bundle.getString("address");
                String name = bundle.getString("name");
                service.findNewUser(address, name);
            } else if (msg.what == DISCOVERING_LEAVE) {
                Bundle bundle = msg.getData();
                String address = bundle.getString("address");
                String name = bundle.getString("name");
                service.removeUser(address, name);
            }
        }
    }

    /**
     * 发现新的组播成员
     *
     * @param ipAddress IP地址
     * @param name      用户姓名
     */
    private void findNewUser(String ipAddress, String name) {
        final int size = mCallbackList.beginBroadcast();
        for (int i = 0; i < size; i++) {
            IIntercomCallback callback = mCallbackList.getBroadcastItem(i);
            if (callback != null) {
                try {
                    callback.findNewUser(ipAddress, name);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        mCallbackList.finishBroadcast();
    }

    /**
     * 删除用户显示
     *
     * @param ipAddress IP地址
     * @param name      用户姓名
     */
    private void removeUser(String ipAddress, String name) {
        final int size = mCallbackList.beginBroadcast();
        for (int i = 0; i < size; i++) {
            IIntercomCallback callback = mCallbackList.getBroadcastItem(i);
            if (callback != null) {
                try {
                    callback.removeUser(ipAddress, name);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        mCallbackList.finishBroadcast();
    }

    private RemoteCallbackList<IIntercomCallback> mCallbackList = new RemoteCallbackList<>();

    public IIntercomService.Stub mBinder = new IIntercomService.Stub() {
        @Override
        public void startRecord() throws RemoteException {
            if (!recorder.isRecording()) {
                recorder.setRecording(true);
                tracker.setPlaying(false);
                threadPool.execute(recorder);
            }
        }

        @Override
        public void stopRecord() throws RemoteException {
            if (recorder.isRecording()) {
                recorder.setRecording(false);
                tracker.setPlaying(true);
            }
        }

        @Override
        public void leaveGroup() throws RemoteException {
            // 发送离线消息
            signInAndOutReq.setCommand(Command.DISC_LEAVE);
            threadPool.execute(signInAndOutReq);
        }

        @Override
        public void registerCallback(IIntercomCallback callback) throws RemoteException {
            mCallbackList.register(callback);
        }

        @Override
        public void unRegisterCallback(IIntercomCallback callback) throws RemoteException {
            mCallbackList.unregister(callback);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        initData();

        keyEventBroadcastReceiver = new KeyEventBroadcastReceiver();
        IntentFilter filter1 = new IntentFilter();
        filter1.addAction("KEY_DOWN");
        filter1.addAction("KEY_UP");
        registerReceiver(keyEventBroadcastReceiver, filter1);

        showNotification();
    }

    /**
     * 发送数据包，检测局域网内其他设备
     */
    private void initData() {
        // 初始化探测线程
        signInAndOutReq = new SignInAndOutReq(handler);
        String name = SPHelper.getString("UserName", "");
        signInAndOutReq.setCommand(Command.DISC_REQUEST + "," + name);
        // 启动探测局域网内其余用户的线程（每5秒扫描一次）
        discoverService.scheduleAtFixedRate(signInAndOutReq, 0, 5, TimeUnit.SECONDS);
        // 初始化JobHandler
        initJobHandler();
    }

    /**
     * 初始化JobHandler
     */
    private void initJobHandler() {
        // 初始化音频输入节点
        recorder = new Recorder(handler);
        encoder = new Encoder(handler);
        multicastSender = new MulticastSender(handler);
        unicastSender = new UnicastSender(handler);
        multicastReceiver = new MulticastReceiver(handler);
        unicastReceiver = new UnicastReceiver(handler);
        decoder = new Decoder(handler);
        tracker = new Tracker(handler);
        // 开启音频输入、输出
        threadPool.execute(encoder);
        threadPool.execute(multicastSender);
        threadPool.execute(unicastSender);
        threadPool.execute(multicastReceiver);
        threadPool.execute(unicastReceiver);
        threadPool.execute(decoder);
        threadPool.execute(tracker);
    }

    /**
     * 前台Service
     */
    private void showNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
//        notificationIntent.setAction(Command.MAIN_ACTION);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        Notification notification = new NotificationCompat.Builder(getApplicationContext())
                .setContentTitle("对讲机")
                .setTicker("对讲机")
                .setContentText("正在使用对讲机")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(pendingIntent)
                .setOngoing(true).build();

        startForeground(Command.FOREGROUND_SERVICE, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("IntercomService", "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    // 按键事件广播
    private class KeyEventBroadcastReceiver extends BaseBroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            super.onReceive(context, intent);
            if (("KEY_DOWN").equals(intent.getAction())) {
                LogUtils.d(TAG, "收到KEY_DOWN广播");
                try {
                    mBinder.startRecord();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else if (("KEY_UP").equals(intent.getAction())) {
                LogUtils.d(TAG, "收到KEY_UP广播");
                try {
                    mBinder.stopRecord();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 释放资源
        free();
        if (keyEventBroadcastReceiver != null) {
            unregisterReceiver(keyEventBroadcastReceiver);
        }
        // 停止前台Service
        stopForeground(true);
        stopSelf();
    }

    /**
     * 释放系统资源
     */
    private void free() {
        // 释放线程资源
        recorder.free();
        encoder.free();
        multicastSender.free();
        unicastSender.free();
        multicastReceiver.free();
        unicastReceiver.free();
        decoder.free();
        tracker.free();
        // 释放线程池
        discoverService.shutdown();
        threadPool.shutdown();
    }
}

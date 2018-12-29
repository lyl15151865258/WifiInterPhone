package net.zhongbenshuo.wifiinterphone.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
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
import net.zhongbenshuo.wifiinterphone.job.MulticastReceiver;
import net.zhongbenshuo.wifiinterphone.job.MulticastSender;
import net.zhongbenshuo.wifiinterphone.job.SignInAndOutReq;
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

public class VoiceService extends Service {

    private final static String TAG = "VoiceService";

    // 创建循环任务线程用于间隔的发送上线消息，获取局域网内其他的用户
    private ScheduledExecutorService discoverService;
    // 创建8个线程的固定大小线程池，分别执行DiscoverServer，以及输入、输出音频
    private ExecutorService threadPool = Executors.newCachedThreadPool();

    // 加入、退出组播组消息
    private SignInAndOutReq signInAndOutReq;
    private MulticastSender multicastSender;
    private MulticastReceiver multicastReceiver;

    public static final int DISCOVERING_SEND = 0;
    public static final int DISCOVERING_RECEIVE = 1;
    public static final int DISCOVERING_LEAVE = 2;

    private KeyEventBroadcastReceiver keyEventBroadcastReceiver;
    private ChangeNameReceiver changeNameReceiver;

    private MyHandler handler = new MyHandler(this);

    /**
     * Service与Runnable的通信
     */
    private static class MyHandler extends Handler {

        private VoiceService service;

        private MyHandler(VoiceService service) {
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
                String speakStatus = bundle.getString("speakStatus");
                service.findNewUser(address, name, speakStatus);
            } else if (msg.what == DISCOVERING_LEAVE) {
                Bundle bundle = msg.getData();
                String address = bundle.getString("address");
                String name = bundle.getString("name");
                String speakStatus = bundle.getString("speakStatus");
                service.removeUser(address, name, speakStatus);
            }
        }
    }

    private BroadcastReceiver headsetPlugReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if (BluetoothProfile.STATE_DISCONNECTED == adapter.getProfileConnectionState(BluetoothProfile.HEADSET)) {
                    // 蓝牙耳机移除
                    LogUtils.d(TAG, "蓝牙耳机移除");
                }
            } else if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {
                // 有线耳机移除，标记耳机按键状态为抬起并发送广播，停止录音
                Intent intent1 = new Intent();
                intent1.setAction("KEY_UP");
                context.sendBroadcast(intent1);
                SPHelper.save("KEY_STATUS_UP", true);
                LogUtils.d(TAG, "有线耳机移除");
            }
        }
    };

    /**
     * 发现新的组播成员
     *
     * @param ipAddress   IP地址
     * @param name        用户姓名
     * @param speakStatus 讲话状态
     */
    private void findNewUser(String ipAddress, String name, String speakStatus) {
        final int size = mCallbackList.beginBroadcast();
        for (int i = 0; i < size; i++) {
            IVoiceCallback callback = mCallbackList.getBroadcastItem(i);
            if (callback != null) {
                try {
                    callback.findNewUser(ipAddress, name, speakStatus);
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
     * @param ipAddress   IP地址
     * @param name        用户姓名
     * @param speakStatus 讲话状态
     */
    private void removeUser(String ipAddress, String name, String speakStatus) {
        final int size = mCallbackList.beginBroadcast();
        for (int i = 0; i < size; i++) {
            IVoiceCallback callback = mCallbackList.getBroadcastItem(i);
            if (callback != null) {
                try {
                    callback.removeUser(ipAddress, name, speakStatus);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        mCallbackList.finishBroadcast();
    }

    private RemoteCallbackList<IVoiceCallback> mCallbackList = new RemoteCallbackList<>();

    public IVoiceService.Stub mBinder = new IVoiceService.Stub() {
        @Override
        public void startRecord() throws RemoteException {
            // 开始录音

        }

        @Override
        public void stopRecord() throws RemoteException {
            // 结束录音

        }

        @Override
        public void leaveGroup() throws RemoteException {
            // 发送离线消息
            signInAndOutReq.setCommand(Command.DISC_LEAVE);
            threadPool.execute(signInAndOutReq);
        }

        @Override
        public void registerCallback(IVoiceCallback callback) throws RemoteException {
            mCallbackList.register(callback);
        }

        @Override
        public void unRegisterCallback(IVoiceCallback callback) throws RemoteException {
            mCallbackList.unregister(callback);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        initThread();

        keyEventBroadcastReceiver = new KeyEventBroadcastReceiver();
        IntentFilter filter1 = new IntentFilter();
        filter1.addAction("KEY_DOWN");
        filter1.addAction("KEY_UP");
        registerReceiver(keyEventBroadcastReceiver, filter1);

        changeNameReceiver = new ChangeNameReceiver();
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction("CHANGE_NAME");
        filter2.addAction("SPEAK_STATUS");
        registerReceiver(changeNameReceiver, filter2);

        IntentFilter filter3 = new IntentFilter();
        filter3.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        filter3.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(headsetPlugReceiver, filter3);

        showNotification();
    }

    /**
     * 开启多线程任务
     */
    private void initThread() {
        // 开启发送自身信息的线程
        initDiscoverThread();
        // 初始化JobHandler
        initJobHandler();
    }

    /**
     * 开启发现线程
     */
    private void initDiscoverThread() {
        if (signInAndOutReq != null) {
            signInAndOutReq = null;
        }
        if (discoverService != null) {
            discoverService.shutdown();
            discoverService = null;
        }
        discoverService = Executors.newScheduledThreadPool(1);
        // 初始化探测线程
        signInAndOutReq = new SignInAndOutReq(handler);
        signInAndOutReq.setCommand(Command.DISC_REQUEST + ","
                + SPHelper.getString("UserName", "Not Defined") + ","
                + SPHelper.getString("SpeakStatus", "0"));
        LogUtils.d(TAG, "新的姓名：" + SPHelper.getString("UserName", "Not Defined") + "，新的讲话状态：" + SPHelper.getString("SpeakStatus", "0"));
        // 启动探测局域网内其余用户的线程（每2秒扫描一次）
        discoverService.scheduleAtFixedRate(signInAndOutReq, 0, 2, TimeUnit.SECONDS);
    }

    /**
     * 初始化JobHandler
     */
    private void initJobHandler() {
        // 初始化音频输入节点
        multicastSender = new MulticastSender(handler);
        multicastReceiver = new MulticastReceiver(handler);
        threadPool.execute(multicastSender);
        threadPool.execute(multicastReceiver);
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

    // 修改姓名或者讲话状态发生改变收到的广播
    public class ChangeNameReceiver extends BaseBroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("CHANGE_NAME".equals(intent.getAction()) || "SPEAK_STATUS".equals(intent.getAction())) {
                initDiscoverThread();
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
        if (changeNameReceiver != null) {
            unregisterReceiver(changeNameReceiver);
        }
        if (headsetPlugReceiver != null) {
            unregisterReceiver(headsetPlugReceiver);
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
        multicastSender.free();
        multicastReceiver.free();
        // 释放线程池
        discoverService.shutdown();
        threadPool.shutdown();
    }
}

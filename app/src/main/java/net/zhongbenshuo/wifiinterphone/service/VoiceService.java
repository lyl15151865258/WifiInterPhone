package net.zhongbenshuo.wifiinterphone.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;

import net.zhongbenshuo.wifiinterphone.R;
import net.zhongbenshuo.wifiinterphone.activity.MainActivity;
import net.zhongbenshuo.wifiinterphone.bean.DataPacket;
import net.zhongbenshuo.wifiinterphone.broadcast.BaseBroadcastReceiver;
import net.zhongbenshuo.wifiinterphone.constant.VoiceConstant;
import net.zhongbenshuo.wifiinterphone.speex.Speex;
import net.zhongbenshuo.wifiinterphone.utils.ByteUtil;
import net.zhongbenshuo.wifiinterphone.utils.LogUtils;
import net.zhongbenshuo.wifiinterphone.utils.WifiUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 对讲机采集和播放语音的Service
 * Created at 2018/11/28 13:49
 *
 * @author LiYuliang
 * @version 1.0
 */

public class VoiceService extends Service {

    private static final String TAG = "VoiceService";

    private VoiceServiceBinder voiceServiceBinder = new VoiceServiceBinder();
    private static ExecutorService executorService;

    private MulticastLock multicastLock;
    private InetAddress inetAddress;

    private Speex speex;
    private int headSize = 15, frameSize;
    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private AcousticEchoCanceler acousticEchoCanceler;
    private AutomaticGainControl automaticGainControl;
    private NoiseSuppressor noiseSuppressor;
    private byte[] recordReceiveBytes;
    private boolean isRunning = true, isSending = false;
    //定义信息头
    private String thisDevInfo;
    private MediaPlayer mMediaPlayer;

    private MulticastSocket multicastSocket;
    private WifiReceiver wifiReceiver;
    private KeyEventBroadcastReceiver keyEventBroadcastReceiver;

    @Override
    public IBinder onBind(Intent intent) {
        thisDevInfo = WifiUtil.getLocalIPAddress(this);
        return voiceServiceBinder;
    }

    public class VoiceServiceBinder extends Binder {
        /**
         * 返回SocketService 在需要的地方可以通过ServiceConnection获取到SocketService
         *
         * @return SocketService对象
         */
        public VoiceService getService() {
            return VoiceService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        executorService = new ThreadPoolExecutor(5, 10, 60, TimeUnit.SECONDS,
                new SynchronousQueue<>(), (r) -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        });
        // 初始化Speex
        initSpeex();

        // 初始化声音采集和声音播放
        initAudioRecord();
        initAudioTrack();

        // 初始化Android自带的回声消除、自动增益控制、噪声抑制器
        setAec();
        setAGC();
        setNC();

        // 获取组播锁
        openMulticastLock();

        // 显示一个前台Notification
        showNotification();

        // 播放无声音乐
        mMediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.silent);
        mMediaPlayer.setLooping(true);
        mMediaPlayer.start();


        registerBroadcastReceiver();
    }

    /**
     * 前台Service
     */
    private void showNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("voice_service", getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("局域网对讲机常驻服务");
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
            Intent intent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
            Notification notification = new NotificationCompat.Builder(this, "voice_service")
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.working))
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                    .setContentIntent(pendingIntent)
                    .build();
            startForeground(VoiceConstant.NOTICE_ID, notification);
        } else {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
            Notification notification = new NotificationCompat.Builder(getApplicationContext(), "voice_service")
                    .setContentTitle(getString(R.string.app_name))
                    .setTicker(getString(R.string.app_name))
                    .setContentText(getString(R.string.working))
                    .setSmallIcon(R.drawable.base_app_icon)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                    .setContentIntent(pendingIntent)
                    .setOngoing(true).build();
            startForeground(VoiceConstant.NOTICE_ID, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        thisDevInfo = WifiUtil.getLocalIPAddress(this);
        try {
            inetAddress = InetAddress.getByName(VoiceConstant.BROADCAST_IP);
            multicastSocket = new MulticastSocket(VoiceConstant.BROADCAST_PORT);
            multicastSocket.setNetworkInterface(NetworkInterface.getByName("wlan0"));
            // setTimeToLive(int ttl)
            // 当ttl为0时，指定数据报应停留在本地主机
            // 为1时，指定数据报发送到本地局域网
            // 为32时，发送到本站点的网络上
            // 为64时，发送到本地区
            // 128时，发送到本大洲
            // 255为全球
            if (multicastLock.isHeld()) {
                // 加入组播
                multicastSocket.setTimeToLive(32);
                multicastSocket.joinGroup(inetAddress);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        startSendVoiceThread();
        startReceiveVoiceThread();
        // 如果Service被终止
        // 当资源允许情况下，重启service
        return START_STICKY;
    }

    private void initSpeex() {
        speex = Speex.getInstance();
        frameSize = speex.getFrameSize();
        recordReceiveBytes = new byte[headSize + frameSize];
    }

    private void initAudioRecord() {
        //获取录制缓存大小
        int recordBufferSize = AudioRecord.getMinBufferSize(
                VoiceConstant.FREQUENCY,                           //采样率
                AudioFormat.CHANNEL_IN_MONO,                       //采样通道数,此处单声道
                VoiceConstant.ENCODING                             //采样输出格式
        );
        //初始化录音对象
        //AudioSource.CAMCORDER                               同方向的相机麦克风，若没有内置相机或无法识别，则使用预设的麦克风
        //MediaRecorder.AudioSource.DEFAULT                   默认音频来源
        //MediaRecorder.AudioSource.MIC                       主麦克风
        //MediaRecorder.AudioSource.VOICE_CALL                语音拨出的语音与对方说话的声音
        //MediaRecorder.AudioSource.VOICE_COMMUNICATION       摄像头旁边的麦克风
        //MediaRecorder.AudioSource.VOICE_DOWNLINK            下行声音
        //MediaRecorder.AudioSource.VOICE_RECOGNITION         语音识别
        //MediaRecorder.AudioSource.VOICE_UPLINK              上行声音
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,      //音源,此处为主麦克风
                VoiceConstant.FREQUENCY,                            //采样率
                AudioFormat.CHANNEL_IN_MONO,                        //采样通道数
                VoiceConstant.ENCODING,                             //采样输出格式
                recordBufferSize                                    //上述求得录制最小缓存
        );
    }

    private void initAudioTrack() {
        // 播放器
        int playerBufferSize = AudioTrack.getMinBufferSize(
                VoiceConstant.FREQUENCY,
                AudioFormat.CHANNEL_OUT_MONO,
                VoiceConstant.ENCODING);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                VoiceConstant.FREQUENCY,
                AudioFormat.CHANNEL_OUT_MONO,
                VoiceConstant.ENCODING,
                playerBufferSize,
                AudioTrack.MODE_STREAM
        );
    }

    //回声消除AEC
    public void setAec() {
        acousticEchoCanceler = AcousticEchoCanceler.create(audioRecord.getAudioSessionId());
        //判断设备是否支持回声消除(AEC)
        boolean isAec = AcousticEchoCanceler.isAvailable();
        LogUtils.d(TAG, "是否支持回声消除(AEC) ========== " + isAec);

        if (isAec && hasMicrophone()) {
            try {
                acousticEchoCanceler.setEnabled(true);//打开AEC
                LogUtils.d(TAG, "AEC is Enable");
            } catch (IllegalStateException e) {
                LogUtils.d(TAG, "setEnabled() in wrong state");
            }
        }
    }

    //自动增益控制(AGC)
    public void setAGC() {
        automaticGainControl = AutomaticGainControl.create(audioRecord.getAudioSessionId());
        //判断设备是否支持自动增益控制(AEC)
        boolean isAvailable = AutomaticGainControl.isAvailable();
        LogUtils.d(TAG, "是否支持自动增益控制(AGC) ========== " + isAvailable);

        if (isAvailable && hasMicrophone()) {
            try {
                automaticGainControl.setEnabled(true);
                LogUtils.d(TAG, "AGC is Enable");
            } catch (IllegalStateException e) {
                LogUtils.d(TAG, "setEnabled() in wrong state");
            }
        }
    }

    //噪声抑制器(NC)
    public void setNC() {
        boolean isAvailable = NoiseSuppressor.isAvailable();
        LogUtils.d(TAG, "是否支持噪声抑制器(NC) ========== " + isAvailable);

        noiseSuppressor = NoiseSuppressor.create(audioRecord.getAudioSessionId());

        if (isAvailable && hasMicrophone()) {
            try {
                noiseSuppressor.setEnabled(true);
                LogUtils.d(TAG, "AGC is Enable");
            } catch (IllegalStateException e) {
                LogUtils.d(TAG, "setEnabled() in wrong state");
            }
        }
    }

    private boolean hasMicrophone() {
        return this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE);
    }

    private void openMulticastLock() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        multicastLock = wifiManager.createMulticastLock("VoiceService");
        multicastLock.acquire();
    }

    private void startSendVoiceThread() {
        isRunning = true;
        Runnable sendVoiceRunnable = () -> {
            short[] audioData = new short[frameSize];
            while (isRunning) {
                if (isSending && WifiUtil.WifiConnected(VoiceService.this)) {
                    audioRecord.startRecording();
                    // 获取音频数据
                    byte[] encoded = new byte[frameSize];
                    int number = audioRecord.read(audioData, 0, frameSize);
                    // 获取有效长度并编码
                    short[] dst = Arrays.copyOfRange(audioData, 0, number);
                    int totalByte = speex.encode(dst, 0, encoded, number);
                    // 获取编码后的有效长度
                    byte[] result = Arrays.copyOfRange(encoded, 0, totalByte);

                    LogUtils.d(TAG, "编码成功，字节数组长度 = " + totalByte + "，设备信息长度：" + thisDevInfo.getBytes().length + "，result长度：" + result.length);

                    if (totalByte > 0) {
                        //构建数据包
                        DataPacket dataPacket = new DataPacket(headSize, totalByte, thisDevInfo.getBytes(), result);
                        //构建数据报文
                        DatagramPacket sendPacket = new DatagramPacket(
                                dataPacket.getAllData(),
                                dataPacket.getAllData().length,
                                inetAddress,
                                VoiceConstant.BROADCAST_PORT);
                        LogUtils.d(TAG, "构建数据报文成功,发送的音频长度为：" + dataPacket.getAllData().length);

                        // 发送并关闭录制
                        try {
                            multicastSocket.send(sendPacket);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        LogUtils.d(TAG, "发送语音");
                    } else {
                        LogUtils.d(TAG, "编码失败");
                    }
                }
            }
            multicastSocket.close();
        };
        executorService.submit(sendVoiceRunnable);
    }

    private void startReceiveVoiceThread() {
        isRunning = true;
        Runnable receiveVoiceRunnable = () -> {
            try {
                DatagramPacket receivePacket = new DatagramPacket(recordReceiveBytes, headSize + frameSize, inetAddress, VoiceConstant.BROADCAST_PORT);
                while (isRunning) {
                    if (!isSending && WifiUtil.WifiConnected(VoiceService.this)) {
                        try {
                            multicastSocket.receive(receivePacket);
                        } catch (IOException e) {
                            e.printStackTrace();
                            continue;
                        }
                        byte[] data = receivePacket.getData();
                        // 获得包头
                        byte[] head = Arrays.copyOf(data, headSize);
                        // 获得包体长度
                        byte[] bodyLength = Arrays.copyOfRange(data, headSize, headSize + 4);
                        // 获得包体
                        byte[] body = Arrays.copyOfRange(data, headSize + 4, headSize + 4 + ByteUtil.byteArrayToInt(bodyLength, 0));
                        // 获得头信息 通过头信息判断是否是自己发出的语音
                        String remoteDeviceInfo = new String(head).trim();
                        LogUtils.d(TAG, "收到来自:" + remoteDeviceInfo + "的语音");
                        if (!remoteDeviceInfo.equals(thisDevInfo)) {
                            short[] lin = new short[frameSize];
                            int size = speex.decode(body, lin, body.length);
                            if (size > 0) {
                                audioTrack.write(lin, 0, size);
                                audioTrack.setStereoVolume(1f, 1f);// 设置当前音量大小
                                audioTrack.play();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        executorService.submit(receiveVoiceRunnable);
    }

    public void setIsSending(boolean isSending) {
        this.isSending = isSending;
    }

    /**
     * 释放资源
     */
    private void release() {
        isRunning = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
        }
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
        }
        if (acousticEchoCanceler != null) {
            acousticEchoCanceler.release();
        }
        if (automaticGainControl != null) {
            automaticGainControl.release();
        }
        if (noiseSuppressor != null) {
            noiseSuppressor.release();
        }
        if (speex != null) {
            speex.close();
        }
        if (multicastLock != null && multicastLock.isHeld()) {
            multicastLock.release();
        }
    }

    public class WifiReceiver extends BaseBroadcastReceiver {

        private static final String TAG = "WifiReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                //拿到wifi的状态值
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_NEW_STATE, 0);
                LogUtils.d(TAG, "wifiState = " + wifiState);
                switch (wifiState) {
                    case WifiManager.WIFI_STATE_DISABLED:
                        break;
                    case WifiManager.WIFI_STATE_DISABLING:
                        break;
                    default:
                        break;
                }
            }
            //监听wifi的连接状态即是否连接的一个有效的无线路由
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                Parcelable parcelableExtra = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (parcelableExtra != null) {
                    // 获取联网状态的NetWorkInfo对象
                    NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
                    //获取的State对象则代表着连接成功与否等状态
                    NetworkInfo.State state = networkInfo.getState();
                    //判断网络是否已经连接
                    boolean isConnected = state == NetworkInfo.State.CONNECTED;
                    LogUtils.d(TAG, "isConnected:" + isConnected);
                    if (isConnected) {

                    } else {

                    }
                }
            }

            // 监听网络连接，包括wifi和移动数据的打开和关闭,以及连接上可用的连接都会接到监听
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                //获取联网状态的NetworkInfo对象
                NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                if (info != null) {
                    //如果当前的网络连接成功并且网络连接可用
                    if (NetworkInfo.State.CONNECTED == info.getState() && info.isAvailable()) {
                        if (info.getType() == ConnectivityManager.TYPE_WIFI || info.getType() == ConnectivityManager.TYPE_MOBILE) {
                            LogUtils.d(TAG, getConnectionType(info.getType()) + "连上");
                            thisDevInfo = WifiUtil.getLocalIPAddress(VoiceService.this);
                        }
                    } else {
                        LogUtils.d(TAG, getConnectionType(info.getType()) + "断开");
                    }
                }
            }
        }

        private String getConnectionType(int type) {
            String connType = "";
            if (type == ConnectivityManager.TYPE_MOBILE) {
                connType = "3G网络数据";
            } else if (type == ConnectivityManager.TYPE_WIFI) {
                connType = "WIFI网络";
            }
            return connType;
        }
    }

    // 按键事件广播
    private class KeyEventBroadcastReceiver extends BaseBroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            super.onReceive(context, intent);
            if (("KEY_DOWN").equals(intent.getAction())) {
                LogUtils.d(TAG, "收到KEY_DOWN广播");
                setIsSending(true);
            } else if (("KEY_UP").equals(intent.getAction())) {
                LogUtils.d(TAG, "收到KEY_UP广播");
                setIsSending(false);
            }
        }
    }

    /**
     * 注册广播
     */
    private void registerBroadcastReceiver() {
        wifiReceiver = new WifiReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(wifiReceiver, filter);

        keyEventBroadcastReceiver = new KeyEventBroadcastReceiver();
        IntentFilter filter1 = new IntentFilter();
        filter1.addAction("KEY_DOWN");
        filter1.addAction("KEY_UP");
        registerReceiver(keyEventBroadcastReceiver, filter1);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        isRunning = false;
        executorService.shutdown();
        release();

        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
        }

        if (wifiReceiver != null) {
            unregisterReceiver(wifiReceiver);
        }

        if (keyEventBroadcastReceiver != null) {
            unregisterReceiver(keyEventBroadcastReceiver);
        }

        stopForeground(true);

        // 如果Service被杀死，干掉通知
        NotificationManager mManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mManager.cancel(VoiceConstant.NOTICE_ID);

        // 重启自己
        Intent intent = new Intent(getApplicationContext(), VoiceService.class);
        startService(intent);
    }
}

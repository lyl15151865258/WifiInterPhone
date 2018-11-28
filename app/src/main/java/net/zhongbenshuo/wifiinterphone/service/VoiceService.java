package net.zhongbenshuo.wifiinterphone.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import net.zhongbenshuo.wifiinterphone.R;
import net.zhongbenshuo.wifiinterphone.activity.MainActivity;
import net.zhongbenshuo.wifiinterphone.bean.DataPacket;
import net.zhongbenshuo.wifiinterphone.constant.VoiceConstant;
import net.zhongbenshuo.wifiinterphone.speex.Speex;
import net.zhongbenshuo.wifiinterphone.utils.ByteUtil;
import net.zhongbenshuo.wifiinterphone.utils.IPUtil;
import net.zhongbenshuo.wifiinterphone.utils.LogUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
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

    private VoiceServiceBinder voiceServiceBinder = new VoiceServiceBinder();
    private static ExecutorService executorService;

    private String broadcastIp;
    private int broadcastPort;
    private Runnable sendVoiceRunnable, receiveVoiceRunnable;

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

    @Override
    public IBinder onBind(Intent intent) {
        thisDevInfo = IPUtil.getLocalIPAddress(this);
        if (intent != null) {
            String ip = intent.getStringExtra("ip");
            int port = intent.getIntExtra("port", -1);
            if (ip != null && !TextUtils.isEmpty(ip) && port != -1) {
                if (!ip.equals(broadcastIp) || port != broadcastPort) {
                    broadcastIp = ip;
                    broadcastPort = port;
                    stopSendVoiceThread();
                    stopReceiveVoiceThread();
                    startSendVoiceThread();
                    startReceiveVoiceThread();
                }
            }
        }
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

        // 显示一个前台Notification
        showNotification();
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
        thisDevInfo = IPUtil.getLocalIPAddress(this);
        if (intent != null) {
            String ip = intent.getStringExtra("ip");
            int port = intent.getIntExtra("port", -1);
            if (ip != null && !TextUtils.isEmpty(ip) && port != -1) {
                if (!ip.equals(broadcastIp) || port != broadcastPort) {
                    broadcastIp = ip;
                    broadcastPort = port;
                    stopSendVoiceThread();
                    stopReceiveVoiceThread();
                    startSendVoiceThread();
                    startReceiveVoiceThread();
                }
            }
        }
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
        LogUtils.d("VoiceService", "是否支持回声消除(AEC) ========== " + isAec);

        if (isAec && hasMicrophone()) {
            try {
                acousticEchoCanceler.setEnabled(true);//打开AEC
                LogUtils.d("VoiceService", "AEC is Enable");
            } catch (IllegalStateException e) {
                LogUtils.d("VoiceService", "setEnabled() in wrong state");
            }
        }
    }

    //自动增益控制(AGC)
    public void setAGC() {
        automaticGainControl = AutomaticGainControl.create(audioRecord.getAudioSessionId());
        //判断设备是否支持自动增益控制(AEC)
        boolean isAvailable = AutomaticGainControl.isAvailable();
        LogUtils.d("VoiceService", "是否支持自动增益控制(AGC) ========== " + isAvailable);

        if (isAvailable && hasMicrophone()) {
            try {
                automaticGainControl.setEnabled(true);
                LogUtils.d("VoiceService", "AGC is Enable");
            } catch (IllegalStateException e) {
                LogUtils.d("VoiceService", "setEnabled() in wrong state");
            }
        }
    }

    //噪声抑制器(NC)
    public void setNC() {
        boolean isAvailable = NoiseSuppressor.isAvailable();
        LogUtils.d("VoiceService", "是否支持噪声抑制器(NC) ========== " + isAvailable);

        noiseSuppressor = NoiseSuppressor.create(audioRecord.getAudioSessionId());

        if (isAvailable && hasMicrophone()) {
            try {
                noiseSuppressor.setEnabled(true);
                LogUtils.d("VoiceService", "AGC is Enable");
            } catch (IllegalStateException e) {
                LogUtils.d("VoiceService", "setEnabled() in wrong state");
            }
        }
    }

    private boolean hasMicrophone() {
        return this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE);
    }

    private void startSendVoiceThread() {
        isRunning = true;
        sendVoiceRunnable = () -> {
            try {
                DatagramSocket clientSocket = new DatagramSocket();
                short[] audioData = new short[frameSize];
                while (isRunning) {
                    if (isSending) {
                        audioRecord.startRecording();
                        //向本机所在的网段广播数据
                        InetAddress inetAddress = null;
                        try {
                            inetAddress = InetAddress.getByName(broadcastIp);
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }

                        // 获取音频数据
                        byte[] encoded = new byte[frameSize];
                        int number = audioRecord.read(audioData, 0, frameSize);
                        // 获取有效长度并编码
                        short[] dst = Arrays.copyOfRange(audioData, 0, number);
                        int totalByte = speex.encode(dst, 0, encoded, number);
                        // 获取编码后的有效长度
                        byte[] result = Arrays.copyOfRange(encoded, 0, totalByte);

                        LogUtils.d("VoiceService", "编码成功，字节数组长度 = " + totalByte + "，设备信息长度：" + thisDevInfo.getBytes().length + "，result长度：" + result.length);

                        if (totalByte > 0) {
                            //构建数据包
                            DataPacket dataPacket = new DataPacket(headSize, totalByte, thisDevInfo.getBytes(), result);
                            //构建数据报文
                            DatagramPacket sendPacket = new DatagramPacket(
                                    dataPacket.getAllData(),
                                    dataPacket.getAllData().length,
                                    inetAddress,
                                    broadcastPort);
                            LogUtils.d("VoiceService", "构建数据报文成功,发送的音频长度为：" + dataPacket.getAllData().length);

                            // 发送
                            try {
                                clientSocket.send(sendPacket);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            LogUtils.d("VoiceService", "发送语音");
                        } else {
                            LogUtils.d("VoiceService", "编码失败");
                        }
                    }
                }
                clientSocket.close();
            } catch (SocketException e) {
                e.printStackTrace();
            }
        };
        executorService.submit(sendVoiceRunnable);
    }

    private void stopSendVoiceThread() {
        if (sendVoiceRunnable != null) {
            isRunning = false;
            sendVoiceRunnable = null;
        }
    }

    private void startReceiveVoiceThread() {
        isRunning = true;
        receiveVoiceRunnable = () -> {
            try {
                DatagramSocket serverSocket = new DatagramSocket(broadcastPort);
                DatagramPacket receivePacket = new DatagramPacket(recordReceiveBytes, headSize + frameSize);
                while (isRunning) {
                    if (!isSending) {
                        try {
                            serverSocket.receive(receivePacket);
                        } catch (IOException e) {
                            e.printStackTrace();
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
                        LogUtils.d("VoiceService", "收到来自:" + remoteDeviceInfo + "的语音");
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
            } catch (SocketException e) {
                e.printStackTrace();
            }
        };
        executorService.submit(receiveVoiceRunnable);
    }

    private void stopReceiveVoiceThread() {
        if (receiveVoiceRunnable != null) {
            isRunning = false;
            receiveVoiceRunnable = null;
        }
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        isRunning = false;
        executorService.shutdown();
        release();

        // 如果Service被杀死，干掉通知
        NotificationManager mManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mManager.cancel(VoiceConstant.NOTICE_ID);

        // 重启自己
        Intent intent = new Intent(getApplicationContext(), VoiceService.class);
        startService(intent);
    }
}

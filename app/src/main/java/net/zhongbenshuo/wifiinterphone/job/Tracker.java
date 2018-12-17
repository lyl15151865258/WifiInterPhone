package net.zhongbenshuo.wifiinterphone.job;

import android.media.AudioTrack;
import android.os.Handler;

import net.zhongbenshuo.wifiinterphone.bean.MyAudioTrack;
import net.zhongbenshuo.wifiinterphone.constant.Constants;
import net.zhongbenshuo.wifiinterphone.data.AudioData;
import net.zhongbenshuo.wifiinterphone.data.MessageQueue;
import net.zhongbenshuo.wifiinterphone.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 音频播放线程
 * Created at 2018/12/12 13:04
 *
 * @author LiYuliang
 * @version 1.0
 */

public class Tracker extends JobHandler {

    private static final String TAG = "Tracker";
    private List<MyAudioTrack> audioTrackList;
    private int outAudioBufferSize;

    public Tracker(Handler handler) {
        super(handler);
        audioTrackList = new ArrayList<>();
        // 获取音频数据缓冲段大小
        outAudioBufferSize = AudioTrack.getMinBufferSize(
                Constants.sampleRateInHz,
                Constants.outputChannelConfig,
                Constants.audioFormat);
    }

    @Override
    public void run() {
        AudioData audioData;
        while ((audioData = MessageQueue.getInstance(MessageQueue.TRACKER_DATA_QUEUE).take()) != null) {
            short[] bytesPkg = audioData.getRawData();
            String ip = audioData.getIp();
            if (audioTrackList.contains(new MyAudioTrack(ip))) {
                for (int i = 0; i < audioTrackList.size(); i++) {
                    if (audioTrackList.get(i).getIp().equals(ip)) {
                        LogUtils.d(TAG, "数据IP地址为：" + ip + ",已有的AudioTrack");
                        try {
                            audioTrackList.get(i).getAudioTrack().write(bytesPkg, 0, bytesPkg.length);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                MyAudioTrack myAudioTrack = new MyAudioTrack(getAudioTrack(), ip);
                audioTrackList.add(myAudioTrack);
                LogUtils.d(TAG, "数据IP地址为：" + ip + ",新的AudioTrack");
                try {
                    myAudioTrack.getAudioTrack().write(bytesPkg, 0, bytesPkg.length);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private AudioTrack getAudioTrack() {
        AudioTrack audioTrack;
        // 初始化音频播放
        audioTrack = new AudioTrack(
                Constants.streamType,
                Constants.sampleRateInHz,
                Constants.outputChannelConfig,
                Constants.audioFormat,
                outAudioBufferSize,
                Constants.trackMode);
        audioTrack.play();
        return audioTrack;
    }

    @Override
    public void free() {
        for (MyAudioTrack myAudioTrack : audioTrackList) {
            AudioTrack audioTrack = myAudioTrack.getAudioTrack();
            audioTrack.stop();
            audioTrack.release();
        }
    }
}
package net.zhongbenshuo.wifiinterphone.job;

import android.media.AudioTrack;
import android.os.Handler;

import net.zhongbenshuo.wifiinterphone.constant.Constants;
import net.zhongbenshuo.wifiinterphone.data.AudioData;
import net.zhongbenshuo.wifiinterphone.data.MessageQueue;

/**
 * 音频播放线程
 * Created at 2018/12/12 13:04
 *
 * @author LiYuliang
 * @version 1.0
 */

public class Tracker extends JobHandler {

    private AudioTrack audioTrack;
    // 播放标志
    private boolean isPlaying = true;

    public Tracker(Handler handler) {
        super(handler);
        // 获取音频数据缓冲段大小
        int outAudioBufferSize = AudioTrack.getMinBufferSize(
                Constants.sampleRateInHz,
                Constants.outputChannelConfig,
                Constants.audioFormat);
        // 初始化音频播放
        audioTrack = new AudioTrack(
                Constants.streamType,
                Constants.sampleRateInHz,
                Constants.outputChannelConfig,
                Constants.audioFormat,
                outAudioBufferSize,
                Constants.trackMode);
        audioTrack.play();
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    @Override
    public void run() {
        AudioData audioData;
        while ((audioData = MessageQueue.getInstance(MessageQueue.TRACKER_DATA_QUEUE).take()) != null) {
            if (isPlaying()) {
                short[] bytesPkg = audioData.getRawData();
                try {
                    audioTrack.write(bytesPkg, 0, bytesPkg.length);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void free() {
        audioTrack.stop();
        audioTrack.release();
        audioTrack = null;
    }
}

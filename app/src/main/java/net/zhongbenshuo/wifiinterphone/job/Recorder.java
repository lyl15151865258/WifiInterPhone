package net.zhongbenshuo.wifiinterphone.job;

import android.media.AudioRecord;
import android.os.Handler;

import net.zhongbenshuo.wifiinterphone.constant.Constants;
import net.zhongbenshuo.wifiinterphone.data.AudioData;
import net.zhongbenshuo.wifiinterphone.data.MessageQueue;
import net.zhongbenshuo.wifiinterphone.speex.Speex;

/**
 * 音频录制线程
 * Created at 2018/12/12 13:04
 *
 * @author LiYuliang
 * @version 1.0
 */

public class Recorder extends JobHandler {

    private AudioRecord audioRecord;
    // 录音标志
    private boolean isRecording = false;

    public Recorder(Handler handler) {
        super(handler);
        // 获取音频数据缓冲段大小
        int inAudioBufferSize = AudioRecord.getMinBufferSize(
                Constants.sampleRateInHz,
                Constants.inputChannelConfig,
                Constants.audioFormat);
        // 初始化音频录制
        audioRecord = new AudioRecord(
                Constants.audioSource,
                Constants.sampleRateInHz,
                Constants.inputChannelConfig,
                Constants.audioFormat,
                inAudioBufferSize);
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void setRecording(boolean recording) {
        isRecording = recording;
    }

    @Override
    public void run() {
        while (isRecording) {
            if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED) {
                audioRecord.startRecording();
            }
            // 实例化音频数据缓冲
            short[] rawData = new short[Speex.getInstance().getFrameSize()];
            audioRecord.read(rawData, 0, Speex.getInstance().getFrameSize());
            AudioData audioData = new AudioData(rawData);
            MessageQueue.getInstance(MessageQueue.ENCODER_DATA_QUEUE).put(audioData);
        }
    }

    @Override
    public void free() {
        // 释放音频录制资源
        audioRecord.stop();
        audioRecord.release();
        audioRecord = null;
    }
}

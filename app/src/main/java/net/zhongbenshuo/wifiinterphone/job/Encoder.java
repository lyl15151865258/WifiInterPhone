package net.zhongbenshuo.wifiinterphone.job;

import android.os.Handler;

import net.zhongbenshuo.wifiinterphone.contentprovider.SPHelper;
import net.zhongbenshuo.wifiinterphone.data.AudioData;
import net.zhongbenshuo.wifiinterphone.data.MessageQueue;
import net.zhongbenshuo.wifiinterphone.utils.AudioDataUtil;

/**
 * 音频编码
 * Created at 2018/12/12 13:02
 *
 * @author LiYuliang
 * @version 1.0
 */

public class Encoder extends JobHandler {

    public Encoder(Handler handler) {
        super(handler);
    }

    @Override
    public void free() {
        AudioDataUtil.free();
    }

    @Override
    public void run() {
        AudioData data;
        // 在MessageQueue为空时，take方法阻塞
        while ((data = MessageQueue.getInstance(MessageQueue.ENCODER_DATA_QUEUE).take()) != null) {
            data.setEncodedData(AudioDataUtil.raw2spx(data.getRawData()));
            if (SPHelper.getInt("broadcast", 0) == 0) {
                MessageQueue.getInstance(MessageQueue.SENDER_DATA_QUEUE_UNICAST).put(data);
            } else {
                MessageQueue.getInstance(MessageQueue.SENDER_DATA_QUEUE_BROADCAST).put(data);
            }
        }
    }
}

package net.zhongbenshuo.wifiinterphone.bean;

/**
 * 语音包实体类
 * Created at 2018/11/24 13:40
 *
 * @author LiYuliang
 * @version 1.0
 */

public class DataPacket {
    private int headSize;              // 头信息大小
    private int bodySize;                   // 语音包大小
    private byte[] recordBytes;

    public DataPacket(int headSize, int bodySize, byte[] headInfo, byte[] bodyBytes) {
        this.headSize = headSize;
        this.bodySize = bodySize;
        recordBytes = new byte[headSize + bodySize];
        for (int i = 0; i < headInfo.length; i++) {
            recordBytes[i] = headInfo[i];
        }
        for (int i = 0; i < bodyBytes.length; i++) {
            recordBytes[i + headSize] = bodyBytes[i];
        }
    }

    public byte[] getHeadInfo() {
        byte[] head = new byte[headSize];
        for (int i = 0; i < head.length; i++) {
            head[i] = recordBytes[i];
        }
        return head;
    }

    public byte[] getBody() {
        byte[] body = new byte[bodySize];
        for (int i = 0; i < body.length; i++) {
            body[i] = recordBytes[i + headSize];
        }
        return body;
    }

    public byte[] getAllData() {
        byte[] data = new byte[headSize + bodySize];
        for (int i = 0; i < data.length; i++) {
            data[i] = recordBytes[i];
        }
        return data;
    }
}

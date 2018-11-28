package net.zhongbenshuo.wifiinterphone.bean;

import net.zhongbenshuo.wifiinterphone.utils.ByteUtil;

/**
 * 语音包实体类
 * Created at 2018/11/24 13:40
 *
 * @author LiYuliang
 * @version 1.0
 */

public class DataPacket {
    private int headSize;                   // 头信息大小
    private int length = 4;                 // 标记语音包大小的文字所占的大小
    private int bodySize;                   // 语音包大小
    private byte[] recordBytes;

    public DataPacket(int headSize, int bodySize, byte[] headInfo, byte[] bodyBytes) {
        this.headSize = headSize;
        this.bodySize = bodySize;
        recordBytes = new byte[headSize + length + bodySize];
        for (int i = 0; i < headInfo.length; i++) {
            recordBytes[i] = headInfo[i];
        }
        for (int i = 0; i < length; i++) {
            recordBytes[i + headSize] = ByteUtil.intToByteArray(bodySize)[i];
        }
        for (int i = 0; i < bodySize; i++) {
            recordBytes[i + headSize + length] = bodyBytes[i];
        }
    }

    public byte[] getHeadInfo() {
        byte[] head = new byte[headSize];
        for (int i = 0; i < head.length; i++) {
            head[i] = recordBytes[i];
        }
        return head;
    }

    public int getBodyLength() {
        byte[] bodyLength = new byte[length];
        for (int i = 0; i < bodyLength.length; i++) {
            bodyLength[i] = recordBytes[i + headSize];
        }
        return ByteUtil.byteArrayToInt(bodyLength, 0);
    }

    public byte[] getBody() {
        byte[] body = new byte[bodySize];
        for (int i = 0; i < body.length; i++) {
            body[i] = recordBytes[i + length + headSize];
        }
        return body;
    }

    public byte[] getAllData() {
        byte[] data = new byte[headSize + length + bodySize];
        for (int i = 0; i < data.length; i++) {
            data[i] = recordBytes[i];
        }
        return data;
    }
}

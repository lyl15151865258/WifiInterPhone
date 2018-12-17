package net.zhongbenshuo.wifiinterphone.data;

/**
 * Created by yanghao1 on 2017/4/25.
 */

public class AudioData {

    /**
     * 原始数据
     */
    private short[] rawData;

    /**
     * 加密数据
     */
    private byte[] encodedData;

    /**
     * 数据来源IP地址
     */
    private String ip;

    public AudioData() {
    }

    public AudioData(short[] rawData, String ip) {
        this.rawData = rawData;
        this.ip = ip;
    }

    public AudioData(byte[] encodedData, String ip) {
        this.encodedData = encodedData;
        this.ip = ip;
    }

    public short[] getRawData() {
        return rawData;
    }

    public void setRawData(short[] rawData) {
        this.rawData = rawData;
    }

    public byte[] getEncodedData() {
        return encodedData;
    }

    public void setEncodedData(byte[] encodedData) {
        this.encodedData = encodedData;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}

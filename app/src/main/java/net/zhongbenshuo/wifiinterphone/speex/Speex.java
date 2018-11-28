package net.zhongbenshuo.wifiinterphone.speex;

/**
 * Speex工具类
 * Created at 2018/11/28 13:49
 *
 * @author LiYuliang
 * @version 1.0
 */

public class Speex {

    private static Speex speex = null;

    /* quality
     * 1 : 4kbps (very noticeable artifacts, usually intelligible)
     * 2 : 6kbps (very noticeable artifacts, good intelligibility)
     * 4 : 8kbps (noticeable artifacts sometimes)
     * 6 : 11kpbs (artifacts usually only noticeable with headphones)
     * 8 : 15kbps (artifacts not usually noticeable)
     */
    //设置为4时压缩比为1/16(与编解码密切相关)
    private static final int DEFAULT_COMPRESSION = 8;

    static {
        try {
            System.loadLibrary("speex");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    Speex() {
        open(DEFAULT_COMPRESSION);
        initEcho(160, 160);//80,160,320
    }

    public static Speex getInstance() {
        if (speex == null) {
            synchronized (Speex.class) {
                if (speex == null) {
                    speex = new Speex();
                }
            }
        }
        return speex;
    }

    public native int open(int compression);

    public native int getFrameSize();

    /**
     * Decode
     *
     * @param encoded input
     * @param lin     output
     * @param size    output size
     */
    public native int decode(byte encoded[], short lin[], int size);

    /**
     * Eecode
     *
     * @param lin     input
     * @param offset  input start location
     * @param encoded output
     * @param size    input lin buffer size
     */
    public native int encode(short lin[], int offset, byte encoded[], int size);

    public native void close();

    public native void initEcho(int frameSize, int filterLength);

    public native void echoCancellation(short[] rec, short[] play, short[] out);

    public native int echoCancellationEncode(short[] rec, short[] play, byte[] encoded);

    public native void destroyEcho();

    public native int getAecStatus();

}

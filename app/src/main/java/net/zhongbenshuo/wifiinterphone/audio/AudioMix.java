package net.zhongbenshuo.wifiinterphone.audio;

/**
 * 音频混音器算法
 * Created at 2018-12-18 0:41
 *
 * @author LiYuliang
 * @version 1.0
 */

public class AudioMix {
    /**
     * b1与b2数组长度可以不相等
     *
     * @param b1 byte1[]
     * @param b2 byte2[]
     * @return b
     */
    public static byte[] remix(byte[] b1, byte[] b2) {
        int l1 = b1.length;
        int l2 = b2.length;
        byte[] bMax = null;
        byte[] bMin = null;
        if (l1 > l2) {
            bMax = b1;
            bMin = b2;
        } else {
            bMax = b2;
            bMin = b1;
        }
        byte[] b = new byte[bMax.length];
        for (int i = 0; i < bMax.length; i++) {
            if (i < bMin.length) {
                b[i] = (byte) ((bMax[i] + bMin[i]) >> 1);
            } else {
                b[i] = bMax[i];
            }
        }
        return b;
    }
}
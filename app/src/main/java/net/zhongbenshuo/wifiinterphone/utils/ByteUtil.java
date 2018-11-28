package net.zhongbenshuo.wifiinterphone.utils;

/**
 * Byte工具类
 * Created at 2018/11/28 16:48
 *
 * @author LiYuliang
 * @version 1.0
 */

public class ByteUtil {

    /**
     * int 数值转byte数组
     *
     * @param i int数值
     * @return 4位长度的byte数组
     */
    public static byte[] intToByteArray(int i) {
        byte[] result = new byte[4];
        result[0] = (byte) ((i >> 24) & 0xFF);
        //必须把我们要的值弄到最低位去，有人说不移位这样做也可以， result[0] = (byte)(i  & 0xFF000000);
        //这样虽然把第一个字节取出来了，但是若直接转换为byte类型，会超出byte的界限，出现error。再提下数//之间转换的原则（不管两种类型的字节大小是否一样，原则是不改变值，内存内容可能会变，比如int转为//float肯定会变）所以此时的int转为byte会越界，只有int的前三个字节都为0的时候转byte才不会越界。虽//然 result[0] = (byte)(i  & 0xFF000000); 这样不行，但是我们可以这样 result[0] = (byte)((i  & //0xFF000000) >>24);
        result[1] = (byte) ((i >> 16) & 0xFF);
        result[2] = (byte) ((i >> 8) & 0xFF);
        result[3] = (byte) (i & 0xFF);
        return result;
    }

    /**
     * byte数组转int数值（第一种方法）
     *
     * @param b      byte数组
     * @param offset 偏移量
     * @return int数值
     */
    public static int byteArrayToInt(byte[] b, int offset) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (b[i + offset] & 0x000000FF) << shift;//往高位游
        }
        return value;
    }

    /**
     * byte数组转int数值（第二种方法）
     *
     * @param b      byte数组
     * @param offset 偏移量
     * @return int数值
     */
    public static int byteArrayToInt2(byte[] b, int offset) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            value |= b[i];
            value = value << 8;
        }
        return value;
    }

}

package net.zhongbenshuo.wifiinterphone;

import net.zhongbenshuo.wifiinterphone.bean.DataPacket;
import net.zhongbenshuo.wifiinterphone.utils.ByteUtil;

public class Test {

    public static void main(String[] agrs) {

        int a = 38;
        byte[] b = ByteUtil.intToByteArray(a);
        System.out.print("数组长度为：" + b.length);
        int c = ByteUtil.byteArrayToInt(b, 0);
        System.out.print("输出的值为：" + c);

        int headSize = 15;
        int totalByte = 38;
        byte[] thisDevInfo = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};
        byte[] encoded = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 0, 1, 2, 3, 4, 5, 6, 7};
        DataPacket dataPacket = new DataPacket(headSize, totalByte, thisDevInfo, encoded);
        System.out.print("最终长度为：" + dataPacket.getAllData().length);

    }

}

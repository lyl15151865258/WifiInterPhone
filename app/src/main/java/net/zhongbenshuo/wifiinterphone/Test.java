package net.zhongbenshuo.wifiinterphone;

import net.zhongbenshuo.wifiinterphone.bean.DataPacket;
import net.zhongbenshuo.wifiinterphone.bean.Music;
import net.zhongbenshuo.wifiinterphone.utils.ByteUtil;

import java.util.ArrayList;
import java.util.List;

public class Test {

    public static void main(String[] agrs) {

        int a = 38;
        byte[] b = ByteUtil.intToByteArray(a);
        System.out.println("数组长度为：" + b.length);
        int c = ByteUtil.byteArrayToInt(b, 0);
        System.out.println("输出的值为：" + c);

        int headSize = 15;
        int totalByte = 38;
        byte[] thisDevInfo = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};
        byte[] encoded = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 0, 1, 2, 3, 4, 5, 6, 7};
        DataPacket dataPacket = new DataPacket(headSize, totalByte, thisDevInfo, encoded);
        System.out.println("最终长度为：" + dataPacket.getAllData().length);

        List<Music> musicList = new ArrayList<>();
        musicList.add(new Music(1, "aaa", 3, 0));
        musicList.add(new Music(2, "bbb", 3, 0));
        musicList.add(new Music(1, "ccc", 3, 0));
        musicList.add(new Music(4, "ddd", 3, 0));
        musicList.add(new Music(5, "eee", 3, 0));
        musicList.remove(new Music(1, "eee", 3, 0));
        for (Music music : musicList) {
            System.out.println(music.getListNo());
        }

    }

}

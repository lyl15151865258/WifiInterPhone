package net.zhongbenshuo.wifiinterphone.constant;

import android.media.AudioFormat;

/**
 * 语音参数常量
 * Created at 2018/11/24 13:43
 *
 * @author LiYuliang
 * @version 1.0
 */

public class VoiceConstant {

    /**
     * 声音采集频率
     */
    public static final int FREQUENCY = 8000;
    /**
     * 声音编码
     */
    public static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    /**
     * 局域网广播端口
     */
    public static final int PORT_BROADCAST = 30000;
    public static final int NOTICE_ID = 100;


    public static final String FILE_PATH = "sound/tts_%s.mp3";

    public static final String DOT_POINT = ".";
    //小数点
    public static final String DOT = "dot";
    //十
    public static final String TEN = "ten";
    //百
    public static final String HUNDRED = "hundred";
    //千
    public static final String THOUSAND = "thousand";
    //万
    public static final String TEN_THOUSAND = "ten_thousand";
    //亿
    public static final String TEN_MILLION = "ten_million";
    //收款成功
    public static final String SUCCESS = "success";
    //元
    public static final String YUAN = "yuan";
}
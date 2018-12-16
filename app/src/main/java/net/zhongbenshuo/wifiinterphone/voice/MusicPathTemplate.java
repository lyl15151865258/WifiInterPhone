package net.zhongbenshuo.wifiinterphone.voice;

import android.text.TextUtils;

import net.zhongbenshuo.wifiinterphone.constant.VoiceConstant;
import net.zhongbenshuo.wifiinterphone.utils.MoneyUtils;

import java.util.ArrayList;
import java.util.List;

public class MusicPathTemplate {

    /**
     * 音频组合
     *
     * @param musicPath
     * @return
     */
    public static List<String> genVoiceList(String musicPath) {
        List<String> result = new ArrayList<>();

        if (TextUtils.isEmpty(musicPath)) {
            return result;
        }else{
            result.add(musicPath);
        }
        return result;
    }


}

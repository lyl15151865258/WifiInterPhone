package net.zhongbenshuo.wifiinterphone;

import android.app.Application;
import android.os.Build;
import android.os.StrictMode;

import com.reechat.voiceengine.NativeVoiceEngine;

import net.zhongbenshuo.wifiinterphone.constant.Constants;
import net.zhongbenshuo.wifiinterphone.contentprovider.SPHelper;
import net.zhongbenshuo.wifiinterphone.utils.CrashHandler;
import net.zhongbenshuo.wifiinterphone.utils.MyLifecycleHandler;

/**
 * Application类
 * Created by Li Yuliang on 2017/03/01.
 *
 * @author LiYuliang
 * @version 2018/02/24
 */

public class WifiInterPhoneApplication extends Application {

    private static WifiInterPhoneApplication instance;


    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        SPHelper.init(this);
        //注册Activity生命周期回调
        registerActivityLifecycleCallbacks(new MyLifecycleHandler());
        // 捕捉异常
        CrashHandler.getInstance().init(this);
        // 初始化音视频通话SDK
        initNativeVoiceEngineSDK();
        // android 7.0系统解决拍照的问题
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
            builder.detectFileUriExposure();
        }
    }

    /**
     * 单例模式中获取唯一的MyApplication实例
     *
     * @return application实例
     */
    public static WifiInterPhoneApplication getInstance() {
        if (instance == null) {
            instance = new WifiInterPhoneApplication();
        }
        return instance;
    }

    /**
     * 初始化音视频通信SDK
     */
    private void initNativeVoiceEngineSDK() {
        NativeVoiceEngine.getInstance().SetSdkParam("RoomServerAddr", Constants.SERVER_ADDRESS);
        //初始化 SDK
        NativeVoiceEngine.getInstance().initSDK(Constants.APP_ID, Constants.APP_KEY);
    }

}

package net.zhongbenshuo.wifiinterphone;

import android.app.Application;
import android.os.Build;
import android.os.StrictMode;

import com.reechat.voiceengine.NativeVoiceEngine;

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

    private static final String appId = "3768c59536565afb";
    private static final String appKey = "df191ec457951c35b8796697c204382d0e12d4e8cb56f54df6a54394be74c5fe";
    private static final String serverAddress = "47.99.72.90:8080";

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
        NativeVoiceEngine.getInstance().SetSdkParam("RoomServerAddr", serverAddress);
        //初始化 SDK
        NativeVoiceEngine.getInstance().initSDK(appId, appKey);
    }

}

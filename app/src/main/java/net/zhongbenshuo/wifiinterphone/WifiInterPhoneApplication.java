package net.zhongbenshuo.wifiinterphone;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.util.DisplayMetrics;

import net.zhongbenshuo.wifiinterphone.contentprovider.SPHelper;
import net.zhongbenshuo.wifiinterphone.utils.CrashHandler;
import net.zhongbenshuo.wifiinterphone.utils.MyLifecycleHandler;

import java.io.File;

/**
 * Application类
 * Created by Li Yuliang on 2017/03/01.
 *
 * @author LiYuliang
 * @version 2018/02/24
 */

public class WifiInterPhoneApplication extends Application {

    private static WifiInterPhoneApplication instance;
    private Context mContext;

    public static int screenWidth = 0;

    public static int screenHeight = 0;

    public static String mSDCardPath;
    public static final String APP_FOLDER_NAME = "Njmeter";

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        instance = this;
        SPHelper.init(this);
        //注册Activity生命周期回调
        registerActivityLifecycleCallbacks(new MyLifecycleHandler());
        // 捕捉异常
        CrashHandler.getInstance().init(this);
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
     * 获取屏幕尺寸
     */
    private void getScreenSize() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenHeight = dm.heightPixels;
        screenWidth = dm.widthPixels;
    }

    private boolean initDirs() {
        mSDCardPath = Environment.getExternalStorageDirectory().toString();
        if (mSDCardPath == null) {
            return false;
        }
        File f = new File(mSDCardPath, APP_FOLDER_NAME);
        if (!f.exists()) {
            try {
                return f.mkdirs();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

}

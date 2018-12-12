package net.zhongbenshuo.wifiinterphone.job;

import android.os.Handler;

/**
 * 子线程任务
 * Created at 2018/12/12 13:02
 *
 * @author LiYuliang
 * @version 1.0
 */

public abstract class JobHandler implements Runnable {

    protected Handler handler;

    public JobHandler(Handler handler) {
        this.handler = handler;
    }

    public void free() {

    }
}

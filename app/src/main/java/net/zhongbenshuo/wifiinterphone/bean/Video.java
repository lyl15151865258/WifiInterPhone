package net.zhongbenshuo.wifiinterphone.bean;

import android.view.SurfaceView;

/**
 * 视频聊天时渲染多人视频画面
 * Created at 2018/12/24 10:40
 *
 * @author Li Yuliang
 * @version 1.0
 */

public class Video {

    private String videoNumber;

    private SurfaceView surfaceView;

    public Video(String videoNumber, SurfaceView surfaceView) {
        this.videoNumber = videoNumber;
        this.surfaceView = surfaceView;
    }

    public String getVideoNumber() {
        return videoNumber;
    }

    public void setVideoNumber(String videoNumber) {
        this.videoNumber = videoNumber;
    }

    public SurfaceView getSurfaceView() {
        return surfaceView;
    }

    public void setSurfaceView(SurfaceView surfaceView) {
        this.surfaceView = surfaceView;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Video)) {
            return false;
        } else {
            try {
                Video that = (Video) o;
                // IP地址匹配就认为是同一个设备
                return videoNumber.equals(that.videoNumber);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }
}

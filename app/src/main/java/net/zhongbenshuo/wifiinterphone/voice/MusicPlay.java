package net.zhongbenshuo.wifiinterphone.voice;

import android.content.Context;
import android.media.MediaPlayer;
import android.text.TextUtils;

import net.zhongbenshuo.wifiinterphone.utils.LogUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 音乐播放工具
 * Created at 2018/11/29 9:53
 *
 * @author LiYuliang
 * @version 1.0
 */

public class MusicPlay {

    private static final String TAG = "MusicPlay";
    private ExecutorService mExecutorService;
    private Context mContext;

    private MusicPlay(Context context) {
        this.mContext = context;
        this.mExecutorService = Executors.newCachedThreadPool();
    }

    private volatile static MusicPlay mVoicePlay = null;

    /**
     * 单例
     *
     * @return
     */
    public static MusicPlay with(Context context) {
        if (mVoicePlay == null) {
            synchronized (MusicPlay.class) {
                if (mVoicePlay == null) {
                    mVoicePlay = new MusicPlay(context);
                }
            }
        }
        return mVoicePlay;
    }

    /**
     * 接收自定义
     *
     * @param voicePlay
     */
    public void play(List<String> voicePlay) {
        if (voicePlay == null || voicePlay.isEmpty()) {
            return;
        }
        mExecutorService.execute(() -> start(voicePlay));
    }

    /**
     * 开始播报
     *
     * @param voicePlay
     */
    private void start(List<String> voicePlay) {
        for (String name : voicePlay) {
            LogUtils.d(TAG, "播放的文件名：" + name);
        }
        if (voicePlay.size() != 0) {
            synchronized (MusicPlay.this) {
                MediaPlayer mMediaPlayer = new MediaPlayer();
                CountDownLatch mCountDownLatch = new CountDownLatch(1);
                try {
                    mMediaPlayer.setDataSource(voicePlay.get(0));
                    mMediaPlayer.prepareAsync();
                    mMediaPlayer.setOnPreparedListener(mediaPlayer -> mMediaPlayer.start());
                    mMediaPlayer.setOnCompletionListener(mediaPlayer -> {
                        mediaPlayer.reset();
                        voicePlay.remove(0);
                        if (voicePlay.size() > 0) {
                            try {
                                mediaPlayer.setDataSource(voicePlay.get(0));
                                mediaPlayer.prepare();
                            } catch (IOException e) {
                                e.printStackTrace();
                                mCountDownLatch.countDown();
                            }
                        } else {
                            mediaPlayer.release();
                            mCountDownLatch.countDown();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    mCountDownLatch.countDown();
                }

                try {
                    mCountDownLatch.await();
                    notifyAll();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

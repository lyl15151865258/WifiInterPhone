package net.zhongbenshuo.wifiinterphone.bean;

import android.media.AudioTrack;

public class MyAudioTrack {

    private AudioTrack audioTrack;

    private String ip;

    public MyAudioTrack(String ip) {
        this.ip = ip;
    }

    public MyAudioTrack(AudioTrack audioTrack, String ip) {
        this.audioTrack = audioTrack;
        this.ip = ip;
    }

    public AudioTrack getAudioTrack() {
        return audioTrack;
    }

    public void setAudioTrack(AudioTrack audioTrack) {
        this.audioTrack = audioTrack;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MyAudioTrack)) {
            return false;
        } else {
            try {
                MyAudioTrack that = (MyAudioTrack) o;
                return ip.equals(that.ip);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

}

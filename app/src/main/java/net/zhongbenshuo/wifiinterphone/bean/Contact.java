package net.zhongbenshuo.wifiinterphone.bean;

import java.io.Serializable;

/**
 * 联系人
 * Created at 2018/11/28 13:40
 *
 * @author LiYuliang
 * @version 1.0
 */

public class Contact implements Serializable {

    private String ip;

    private String userName;

    private boolean shouldSend = true;

    public Contact(String ip) {
        this.ip = ip;
    }

    public Contact(String ip, String userName) {
        this.ip = ip;
        this.userName = userName;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public boolean isShouldSend() {
        return shouldSend;
    }

    public void setShouldSend(boolean shouldSend) {
        this.shouldSend = shouldSend;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Contact)) {
            return false;
        } else {
            try {
                Contact that = (Contact) o;
                // 设备Mac地址一致就认为是同一个设备
                return ip.equals(that.ip);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }
}

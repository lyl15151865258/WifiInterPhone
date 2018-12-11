package net.zhongbenshuo.wifiinterphone.bean;

/**
 * 联系人
 * Created at 2018/11/28 13:40
 *
 * @author LiYuliang
 * @version 1.0
 */

public class Contact {

    private String ip;

    private String deviceModel;

    private String userName;

    private String iconUrl;

    private boolean shouldSend = true;

    public Contact(String ip) {
        this.ip = ip;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
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

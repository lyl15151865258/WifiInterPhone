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
}

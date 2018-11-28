package net.zhongbenshuo.wifiinterphone.bean;

/**
 * 故障实体类
 * Created at 2018/11/28 13:40
 *
 * @author LiYuliang
 * @version 1.0
 */

public class Malfunction {

    private String malfunctionType;

    private String description;

    private String malfunctionTime;

    private String iconUrl;

    public String getMalfunctionType() {
        return malfunctionType;
    }

    public void setMalfunctionType(String malfunctionType) {
        this.malfunctionType = malfunctionType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMalfunctionTime() {
        return malfunctionTime;
    }

    public void setMalfunctionTime(String malfunctionTime) {
        this.malfunctionTime = malfunctionTime;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }
}

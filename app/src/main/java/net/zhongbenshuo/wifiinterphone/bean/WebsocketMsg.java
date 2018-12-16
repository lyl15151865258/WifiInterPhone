package net.zhongbenshuo.wifiinterphone.bean;

import java.util.List;

/**
 * WebSocket传输的信息
 * Created at 2018-12-16 22:50
 *
 * @author LiYuliang
 * @version 1.0
 */

public class WebsocketMsg {

    private String Description;
    private int ListNo;
    private boolean Status;
    private String Time;
    private List<String> Title;
    private String Type;
    private int PlayCount;
    private String ServerAddress;

    public void setDescription(String Description) {
        this.Description = Description;
    }

    public String getDescription() {
        return Description;
    }

    public void setListNo(int ListNo) {
        this.ListNo = ListNo;
    }

    public int getListNo() {
        return ListNo;
    }

    public void setStatus(boolean Status) {
        this.Status = Status;
    }

    public boolean getStatus() {
        return Status;
    }

    public void setTime(String Time) {
        this.Time = Time;
    }

    public String getTime() {
        return Time;
    }

    public void setTitle(List<String> Title) {
        this.Title = Title;
    }

    public List<String> getTitle() {
        return Title;
    }

    public void setType(String Type) {
        this.Type = Type;
    }

    public String getType() {
        return Type;
    }

    public int getPlayCount() {
        return PlayCount;
    }

    public void setPlayCount(int playCount) {
        this.PlayCount = playCount;
    }

    public String getServerAddress() {
        return ServerAddress;
    }

    public void setServerAddress(String serverAddress) {
        ServerAddress = serverAddress;
    }
}

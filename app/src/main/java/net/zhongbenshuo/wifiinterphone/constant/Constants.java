package net.zhongbenshuo.wifiinterphone.constant;

/**
 * 部分常量值
 * Created at 2018/11/28 13:42
 *
 * @author LiYuliang
 * @version 1.0
 */

public class Constants {

    public static final String EMPTY = "";
    public static final String FAIL = "fail";
    public static final String NEW_LINE = "\n";
    public static final String POINT = ".";
    public static final String HYPHEN = "-";
    public static final String SUCCESS = "success";

    /**
     * EventBus标记
     */
    public static final String CONNECT_SUCCESS_SOCKET = "connectSuccess_socket";
    public static final String CONNECT_SUCCESS_WEBSOCKET = "connectSuccess_webSocket";
    public static final String CONNECT_FAIL_SOCKET = "connectFail_socket";
    public static final String CONNECT_FAIL_WEBSOCKET = "connectFail_webSocket";
    public static final String CONNECT_OPEN_SOCKET = "connectOpen_socket";
    public static final String CONNECT_OPEN_WEBSOCKET = "connectOpen_webSocket";
    public static final String CONNECT_CLOSE_SOCKET = "connectClose_socket";
    public static final String CONNECT_CLOSE_WEBSOCKET = "connectClose_webSocket";
    public static final String SHOW_TOAST_SOCKET = "showToast_socket";
    public static final String SHOW_TOAST_WEBSOCKET = "showToast_webSocket";
    public static final String SHOW_DATA_SOCKET = "showData_socket";
    public static final String SHOW_DATA_WEBSOCKET = "showData_webSocket";

    /**
     * 退出程序点击两次返回键的间隔时间
     */
    public static final int EXIT_DOUBLE_CLICK_TIME = 2000;
    /**
     * 距离达到1000m进行单位转换，变为1km
     */
    public static final int KILOMETER = 1000;
    /**
     * 网页加载完成进度
     */
    public static final int PROGRESS_WEBVIEW = 100;

}

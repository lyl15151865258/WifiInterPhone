package net.zhongbenshuo.wifiinterphone.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;

import net.zhongbenshuo.wifiinterphone.R;
import net.zhongbenshuo.wifiinterphone.adapter.MalfunctionAdapter;
import net.zhongbenshuo.wifiinterphone.bean.WebSocketData;
import net.zhongbenshuo.wifiinterphone.broadcast.BaseBroadcastReceiver;
import net.zhongbenshuo.wifiinterphone.service.WebSocketService;
import net.zhongbenshuo.wifiinterphone.utils.LogUtils;
import net.zhongbenshuo.wifiinterphone.widget.xrecyclerview.ProgressStyle;
import net.zhongbenshuo.wifiinterphone.widget.xrecyclerview.XRecyclerView;
import net.zhongbenshuo.wifiinterphone.widget.xrecyclerview.XRecyclerViewDivider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * 故障列表Fragment
 * Created at 2018/11/20 13:45
 *
 * @author LiYuliang
 * @version 1.0
 */

public class MalfunctionFragment extends BaseFragment {

    private final static String TAG = "MalfunctionFragment";
    private Context mContext;
    private XRecyclerView rvMalfunction;
    private List<WebSocketData> malfunctionList;
    private MalfunctionAdapter malfunctionAdapter;
    private MyReceiver myReceiver;
    private boolean sIsScrolling = false;
    private WebSocketService webSocketService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_malfunction, container, false);
        mContext = getContext();
        rvMalfunction = view.findViewById(R.id.rvMalfunction);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rvMalfunction.setLayoutManager(linearLayoutManager);
        rvMalfunction.setRefreshProgressStyle(ProgressStyle.BallSpinFadeLoader);
        rvMalfunction.setArrowImageView(R.drawable.iconfont_downgrey);
        rvMalfunction.getDefaultRefreshHeaderView().setRefreshTimeVisible(false);
        rvMalfunction.setLoadingMoreEnabled(false);
        rvMalfunction.addItemDecoration(new XRecyclerViewDivider(mContext, LinearLayoutManager.HORIZONTAL, 1, ContextCompat.getColor(mContext, R.color.gray_slight)));

        rvMalfunction.setLoadingListener(new XRecyclerView.LoadingListener() {
            @Override
            public void onRefresh() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                rvMalfunction.refreshComplete();
            }

            @Override
            public void onLoadMore() {

            }
        });
        malfunctionList = new ArrayList<>();
        malfunctionAdapter = new MalfunctionAdapter(mContext, malfunctionList);
        malfunctionAdapter.setOnItemClickListener(onItemClickListener);
        rvMalfunction.setAdapter(malfunctionAdapter);
        rvMalfunction.addOnScrollListener(onScrollListener);

        Intent intent = new Intent(mContext, WebSocketService.class);
        intent.putExtra("ServerHost", "192.168.1.126");
        intent.putExtra("WebSocketPort", "50100");
        mContext.startService(intent);

        myReceiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("RECEIVE_MALFUNCTION");
        mContext.registerReceiver(myReceiver, intentFilter);

        return view;
    }

    @Override
    public void lazyLoad() {
        Intent intent = new Intent(mContext, WebSocketService.class);
        mContext.bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    /**
     * onServiceConnected和onServiceDisconnected运行在UI线程中
     */
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            WebSocketService.WebSocketServiceBinder binder = (WebSocketService.WebSocketServiceBinder) service;
            webSocketService = binder.getService();
            LogUtils.d(TAG, "绑定Service成功");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            webSocketService = null;
        }
    };

    private MalfunctionAdapter.OnItemClickListener onItemClickListener = (position) -> {

    };

    private RecyclerView.OnScrollListener onScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            // 先判断mContext是否为空，防止Activity已经onDestroy导致的java.lang.IllegalArgumentException: You cannot start a load for a destroyed activity
            if (mContext != null) {
                // 如果快速滑动，停止Glide的加载，停止滑动后恢复加载
                if (newState == RecyclerView.SCROLL_STATE_SETTLING) {
                    sIsScrolling = true;
                    Glide.with(mContext).pauseRequests();
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (sIsScrolling) {
                        Glide.with(mContext).resumeRequests();
                    }
                    sIsScrolling = false;
                }
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
        }
    };

    private class MyReceiver extends BaseBroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            super.onReceive(context, intent);
            if ("RECEIVE_MALFUNCTION".equals(intent.getAction())) {
                WebSocketData webSocketData = (WebSocketData) intent.getSerializableExtra("data");
                if (webSocketData.isStatus()) {
                    malfunctionList.add(0, webSocketData);
                    malfunctionAdapter.notifyDataSetChanged();
                } else {
                    Iterator<WebSocketData> webSocketMsgIterator = malfunctionList.iterator();
                    while (webSocketMsgIterator.hasNext()) {
                        WebSocketData socketMsg = webSocketMsgIterator.next();
                        if (socketMsg.getListNo() == webSocketData.getListNo()) {
                            webSocketMsgIterator.remove();
                        }
                    }
                    malfunctionAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        webSocketService.closeWebSocket();
        if (myReceiver != null) {
            mContext.unregisterReceiver(myReceiver);
        }
    }
}
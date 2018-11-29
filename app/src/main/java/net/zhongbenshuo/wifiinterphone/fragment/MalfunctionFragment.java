package net.zhongbenshuo.wifiinterphone.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;

import net.zhongbenshuo.wifiinterphone.R;
import net.zhongbenshuo.wifiinterphone.adapter.MalfunctionAdapter;
import net.zhongbenshuo.wifiinterphone.bean.Malfunction;
import net.zhongbenshuo.wifiinterphone.widget.xrecyclerview.ProgressStyle;
import net.zhongbenshuo.wifiinterphone.widget.xrecyclerview.XRecyclerView;
import net.zhongbenshuo.wifiinterphone.widget.xrecyclerview.XRecyclerViewDivider;

import java.util.ArrayList;
import java.util.List;

/**
 * 故障列表Fragment
 * Created at 2018/11/20 13:45
 *
 * @author LiYuliang
 * @version 1.0
 */

public class MalfunctionFragment extends BaseFragment {

    private Context mContext;
    private XRecyclerView xRVMalfunction;
    private List<Malfunction> malfunctionList;
    private MalfunctionAdapter malfunctionAdapter;
    private boolean sIsScrolling = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);
        mContext = getContext();
        xRVMalfunction = view.findViewById(R.id.xRVMalfunction);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        xRVMalfunction.setLayoutManager(linearLayoutManager);
        xRVMalfunction.setRefreshProgressStyle(ProgressStyle.BallSpinFadeLoader);
        xRVMalfunction.setArrowImageView(R.drawable.iconfont_downgrey);
        xRVMalfunction.getDefaultRefreshHeaderView().setRefreshTimeVisible(false);
        xRVMalfunction.getDefaultFootView().setLoadingHint(getString(R.string.loading));
        xRVMalfunction.getDefaultFootView().setNoMoreHint(getString(R.string.NoMoreData));
        xRVMalfunction.addItemDecoration(new XRecyclerViewDivider(mContext, LinearLayoutManager.HORIZONTAL, 1, ContextCompat.getColor(mContext, R.color.gray_slight)));

        xRVMalfunction.setLoadingListener(new XRecyclerView.LoadingListener() {
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
                xRVMalfunction.refreshComplete();
            }

            @Override
            public void onLoadMore() {
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
                xRVMalfunction.loadMoreComplete();
                xRVMalfunction.setNoMore(true);
            }
        });
        malfunctionList = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Malfunction malfunction = new Malfunction();
            malfunction.setMalfunctionType("电机故障" + i);
            malfunction.setDescription("电机转速异常" + i);
            malfunction.setMalfunctionTime("2018-11-28 11:02:30");
            malfunction.setIconUrl("");
            malfunctionList.add(malfunction);
        }
        malfunctionAdapter = new MalfunctionAdapter(mContext, malfunctionList);
        malfunctionAdapter.setOnItemClickListener(onItemClickListener);
        xRVMalfunction.setAdapter(malfunctionAdapter);
        xRVMalfunction.addOnScrollListener(onScrollListener);

        return view;
    }

    @Override
    public void lazyLoad() {

    }

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

}
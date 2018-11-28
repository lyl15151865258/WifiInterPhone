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
import net.zhongbenshuo.wifiinterphone.adapter.ContactsAdapter;
import net.zhongbenshuo.wifiinterphone.bean.Contact;
import net.zhongbenshuo.wifiinterphone.widget.xrecyclerview.ProgressStyle;
import net.zhongbenshuo.wifiinterphone.widget.xrecyclerview.XRecyclerView;
import net.zhongbenshuo.wifiinterphone.widget.xrecyclerview.XRecyclerViewDivider;

import java.util.ArrayList;
import java.util.List;

/**
 * 联系人Fragment
 * Created at 2018/11/20 13:44
 *
 * @author LiYuliang
 * @version 1.0
 */

public class ContactsFragment extends BaseFragment {

    private Context mContext;
    private XRecyclerView xRVContacts;
    private List<Contact> contactList;
    private ContactsAdapter contactsAdapter;
    private boolean sIsScrolling = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);
        mContext = getContext();
        xRVContacts = view.findViewById(R.id.xRVContacts);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        xRVContacts.setLayoutManager(linearLayoutManager);
        xRVContacts.setRefreshProgressStyle(ProgressStyle.BallSpinFadeLoader);
        xRVContacts.setLoadingMoreEnabled(false);
        xRVContacts.setArrowImageView(R.drawable.iconfont_downgrey);
        xRVContacts.getDefaultRefreshHeaderView().setRefreshTimeVisible(false);
        xRVContacts.addItemDecoration(new XRecyclerViewDivider(mContext, LinearLayoutManager.HORIZONTAL, 1, ContextCompat.getColor(mContext, R.color.gray_slight)));

        xRVContacts.setLoadingListener(new XRecyclerView.LoadingListener() {
            @Override
            public void onRefresh() {

            }

            @Override
            public void onLoadMore() {

            }
        });
        contactList = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Contact contact = new Contact();
            contact.setUserName("姓名" + i);
            contact.setIp("IP" + i);
            contact.setIconUrl("");
            contactList.add(contact);
        }
        contactsAdapter = new ContactsAdapter(mContext, contactList);
        contactsAdapter.setOnItemClickListener(onItemClickListener);
        xRVContacts.setAdapter(contactsAdapter);
        xRVContacts.addOnScrollListener(onScrollListener);

        return view;
    }

    @Override
    public void lazyLoad() {

    }

    private ContactsAdapter.OnItemClickListener onItemClickListener = (position) -> {

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
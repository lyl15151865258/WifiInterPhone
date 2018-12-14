package net.zhongbenshuo.wifiinterphone.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import net.zhongbenshuo.wifiinterphone.R;
import net.zhongbenshuo.wifiinterphone.adapter.ContactsAdapter;
import net.zhongbenshuo.wifiinterphone.bean.Contact;
import net.zhongbenshuo.wifiinterphone.contentprovider.SPHelper;
import net.zhongbenshuo.wifiinterphone.service.IIntercomCallback;
import net.zhongbenshuo.wifiinterphone.service.IIntercomService;
import net.zhongbenshuo.wifiinterphone.service.IntercomService;
import net.zhongbenshuo.wifiinterphone.utils.LogUtils;
import net.zhongbenshuo.wifiinterphone.utils.WifiUtil;
import net.zhongbenshuo.wifiinterphone.widget.RecyclerViewDivider;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * 联系人Fragment
 * Created at 2018/11/20 13:44
 *
 * @author LiYuliang
 * @version 1.0
 */

public class ContactsFragment extends BaseFragment {

    private static final String TAG = "ContactsFragment";
    private Context mContext;
    private TextView tvContactCount;
    private ContactsAdapter contactsAdapter;
    private boolean sIsScrolling = false;
    private IIntercomService intercomService;

    private List<Contact> contactList;
    private Handler handler = new DisplayHandler(this);
    private static final int FOUND_NEW_USER = 0, REMOVE_USER = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);
        mContext = getContext();
        tvContactCount = view.findViewById(R.id.tvContactCount);
        RecyclerView rvContacts = view.findViewById(R.id.rvContacts);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rvContacts.setLayoutManager(linearLayoutManager);
        rvContacts.addItemDecoration(new RecyclerViewDivider(mContext, LinearLayoutManager.HORIZONTAL, 1, ContextCompat.getColor(mContext, R.color.gray_slight)));

        contactList = new ArrayList<>();
        Contact contact = new Contact("/" + WifiUtil.getLocalIPAddress(),SPHelper.getString("UserName", ""));
        contactList.add(contact);

        contactsAdapter = new ContactsAdapter(mContext, contactList);
        contactsAdapter.setOnItemClickListener(onItemClickListener);
        rvContacts.setAdapter(contactsAdapter);
        rvContacts.addOnScrollListener(onScrollListener);

        view.findViewById(R.id.btnSelectAll).setOnClickListener(onClickListener);
        view.findViewById(R.id.btnUnSelectAll).setOnClickListener(onClickListener);

        return view;
    }

    @Override
    public void lazyLoad() {
        if (intercomService == null) {
            Intent intent = new Intent(mContext, IntercomService.class);
            mContext.bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        }
    }

    private View.OnClickListener onClickListener = (v) -> {
        switch (v.getId()) {
            case R.id.btnSelectAll:
                for (int i = 0; i < contactList.size(); i++) {
                    contactList.get(i).setShouldSend(true);
                }
                tvContactCount.setText(String.valueOf(contactList.size()) + "/" + String.valueOf(contactList.size()));
                contactsAdapter.notifyDataSetChanged();
                sendChangeIpBroadcast();
                break;
            case R.id.btnUnSelectAll:
                for (int i = 0; i < contactList.size(); i++) {
                    contactList.get(i).setShouldSend(false);
                }
                tvContactCount.setText("0/" + String.valueOf(contactList.size()));
                contactsAdapter.notifyDataSetChanged();
                sendChangeIpBroadcast();
                break;
            default:
                break;
        }
    };

    /**
     * 待发送的IP发生了变化，发送广播通知
     */
    private void sendChangeIpBroadcast() {
        Intent intent = new Intent();
        intent.setAction("CHANGE_SEND_IP");
        ArrayList<String> ipList = new ArrayList<>();
        for (int i = 0; i < contactList.size(); i++) {
            if (contactList.get(i).isShouldSend()) {
                ipList.add(contactList.get(i).getIp().replace("/", ""));
            }
        }
        intent.putStringArrayListExtra("IP", ipList);
        mContext.sendBroadcast(intent);
    }

    /**
     * onServiceConnected和onServiceDisconnected运行在UI线程中
     */
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            intercomService = IIntercomService.Stub.asInterface(service);
            try {
                intercomService.registerCallback(intercomCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            LogUtils.d(TAG, "绑定Service成功");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            intercomService = null;
        }
    };

    /**
     * 被调用的方法运行在Binder线程池中，不能更新UI
     */
    private IIntercomCallback intercomCallback = new IIntercomCallback.Stub() {
        @Override
        public void findNewUser(String ipAddress, String name) {
            sendMsg2MainThread(ipAddress, name, FOUND_NEW_USER);
        }

        @Override
        public void removeUser(String ipAddress, String name) {
            sendMsg2MainThread(ipAddress, name, REMOVE_USER);
        }
    };

    /**
     * 发送Handler消息
     *
     * @param address IP地址
     * @param name    姓名
     * @param msgWhat 消息类型
     */
    private void sendMsg2MainThread(String address, String name, int msgWhat) {
        Message msg = new Message();
        msg.what = msgWhat;
        msg.obj = address;
        Bundle bundle = new Bundle();
        bundle.putString("address", address);
        bundle.putString("name", name);
        msg.setData(bundle);
        handler.sendMessage(msg);
    }

    private ContactsAdapter.OnItemClickListener onItemClickListener = (position) -> {
        int sendCount = 0;
        for (int i = 0; i < contactList.size(); i++) {
            if (i == position) {
                contactList.get(i).setShouldSend(!contactList.get(i).isShouldSend());
            }
            if (contactList.get(i).isShouldSend()) {
                sendCount++;
            }
        }
        tvContactCount.setText(String.valueOf(sendCount) + "/" + String.valueOf(contactList.size()));
        contactsAdapter.notifyItemChanged(position);
        sendChangeIpBroadcast();
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

    /**
     * 跨进程回调更新界面
     */
    private static class DisplayHandler extends Handler {
        // 弱引用
        private WeakReference<ContactsFragment> activityWeakReference;

        DisplayHandler(ContactsFragment contactsFragment) {
            activityWeakReference = new WeakReference<>(contactsFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            ContactsFragment contactsFragment = activityWeakReference.get();
            if (contactsFragment != null) {
                if (msg.what == FOUND_NEW_USER) {
                    Bundle bundle = msg.getData();
                    String address = bundle.getString("address", "");
                    String name = bundle.getString("name", "");
                    contactsFragment.foundNewUser(address, name);
                } else if (msg.what == REMOVE_USER) {
                    contactsFragment.removeExistUser((String) msg.obj);
                }
            }
        }
    }

    /**
     * 发现新的用户地址
     *
     * @param ipAddress 用户IP地址
     * @param name      用户姓名
     */
    public void foundNewUser(String ipAddress, String name) {
        Contact contact = new Contact(ipAddress, name);
        if (!contactList.contains(contact)) {
            addNewUser(contact);
        }
    }

    /**
     * 删除用户
     *
     * @param ipAddress 用户IP地址
     */
    public void removeExistUser(String ipAddress) {
        Contact contact = new Contact(ipAddress);
        if (contactList.contains(contact)) {
            int position = contactList.indexOf(contact);
            contactList.remove(position);
            int sendCount = 0;
            for (int i = 0; i < contactList.size(); i++) {
                if (contactList.get(i).isShouldSend()) {
                    sendCount++;
                }
            }
            tvContactCount.setText(String.valueOf(sendCount) + "/" + String.valueOf(contactList.size()));
            contactsAdapter.notifyItemRemoved(position);
            contactsAdapter.notifyItemRangeChanged(0, contactList.size());
            sendChangeIpBroadcast();
        }
    }

    /**
     * 增加新的用户
     *
     * @param contact 新用户
     */
    public void addNewUser(Contact contact) {
        contactList.add(contact);
        int sendCount = 0;
        for (int i = 0; i < contactList.size(); i++) {
            if (contactList.get(i).isShouldSend()) {
                sendCount++;
            }
        }
        tvContactCount.setText(String.valueOf(sendCount) + "/" + String.valueOf(contactList.size()));
        contactsAdapter.notifyItemInserted(contactList.size() - 1);
        sendChangeIpBroadcast();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (intercomService != null && intercomService.asBinder().isBinderAlive()) {
            try {
                intercomService.unRegisterCallback(intercomCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            mContext.unbindService(serviceConnection);
        }
    }

}
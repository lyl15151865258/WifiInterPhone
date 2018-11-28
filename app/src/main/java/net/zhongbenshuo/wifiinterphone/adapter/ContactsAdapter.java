package net.zhongbenshuo.wifiinterphone.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import net.zhongbenshuo.wifiinterphone.R;
import net.zhongbenshuo.wifiinterphone.bean.Contact;
import net.zhongbenshuo.wifiinterphone.constant.NetWork;

import java.util.List;

/**
 * 联系人适配器
 * Created at 2018/11/28 13:38
 *
 * @author LiYuliang
 * @version 1.0
 */

public class ContactsAdapter extends RecyclerView.Adapter {

    private Context mContext;
    private List<Contact> contactList;
    private OnItemClickListener mListener;

    public ContactsAdapter(Context mContext, List<Contact> contactList) {
        this.mContext = mContext;
        this.contactList = contactList;
    }

    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list_contact, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        NewsViewHolder holder = (NewsViewHolder) viewHolder;
        Contact contact = contactList.get(position);
        holder.tvIp.setText(contact.getIp());
        holder.tvDeviceModel.setText(contact.getDeviceModel());
        holder.tvUserName.setText(contact.getUserName());

        String userIconUrl = contact.getIconUrl();
        if (userIconUrl != null && !userIconUrl.isEmpty()) {
            String headIconUrl = "http://" + NetWork.SERVER_HOST_MAIN + ":" + NetWork.SERVER_PORT_MAIN + "/" + userIconUrl.replace("\\", "/");
            // 加载头像
            RequestOptions options = new RequestOptions()
                    .error(R.drawable.photo_user)
                    .placeholder(R.drawable.photo_user)
                    .dontAnimate();
            Glide.with(mContext).load(headIconUrl).apply(options).into(holder.ivContactIcon);
        }

        holder.itemView.setOnClickListener((v) -> {
            if (mListener != null) {
                mListener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    private class NewsViewHolder extends RecyclerView.ViewHolder {
        private TextView tvIp, tvDeviceModel, tvUserName;
        private ImageView ivContactIcon;

        private NewsViewHolder(View itemView) {
            super(itemView);
            ivContactIcon = itemView.findViewById(R.id.ivContactIcon);
            tvIp = itemView.findViewById(R.id.tvIp);
            tvDeviceModel = itemView.findViewById(R.id.tvDeviceModel);
            tvUserName = itemView.findViewById(R.id.tvUserName);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

}

package net.zhongbenshuo.wifiinterphone.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import net.zhongbenshuo.wifiinterphone.R;
import net.zhongbenshuo.wifiinterphone.bean.WebSocketData;
import net.zhongbenshuo.wifiinterphone.constant.NetWork;

import java.util.List;

/**
 * 故障列表适配器
 * Created at 2018/11/28 13:39
 *
 * @author LiYuliang
 * @version 1.0
 */

public class MalfunctionAdapter extends RecyclerView.Adapter {

    private Context mContext;
    private List<WebSocketData> malfunctionList;
    private OnItemClickListener mListener;

    public MalfunctionAdapter(Context mContext, List<WebSocketData> malfunctionList) {
        this.mContext = mContext;
        this.malfunctionList = malfunctionList;
    }

    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list_malfunction, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        NewsViewHolder holder = (NewsViewHolder) viewHolder;
        WebSocketData malfunction = malfunctionList.get(position);
        holder.tvMalfunctionType.setText(malfunction.getText());
        holder.tvMalfunctionTime.setText(malfunction.getTime());

        holder.tvMalfunctionType.setTextColor(malfunction.getForeColor());
        holder.tvMalfunctionTime.setTextColor(malfunction.getForeColor());
        holder.llRoot.setBackgroundColor(malfunction.getBackColor());

        String userIconUrl = "";
        if (userIconUrl != null && !userIconUrl.isEmpty()) {
            String headIconUrl = "http://" + NetWork.SERVER_HOST_MAIN + ":" + NetWork.SERVER_PORT_MAIN + "/" + userIconUrl.replace("\\", "/");
            // 加载头像
            RequestOptions options = new RequestOptions()
                    .error(R.drawable.photo_user)
                    .placeholder(R.drawable.photo_user)
                    .dontAnimate();
            Glide.with(mContext).load(headIconUrl).apply(options).into(holder.ivMalfunctionIcon);
        }

        holder.itemView.setOnClickListener((v) -> {
            if (mListener != null) {
                mListener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return malfunctionList.size();
    }

    private class NewsViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout llRoot;
        private TextView tvMalfunctionType, tvMalfunctionTime;
        private ImageView ivMalfunctionIcon;

        private NewsViewHolder(View itemView) {
            super(itemView);
            llRoot = itemView.findViewById(R.id.llRoot);
            ivMalfunctionIcon = itemView.findViewById(R.id.ivMalfunctionIcon);
            tvMalfunctionType = itemView.findViewById(R.id.tvMalfunctionType);
            tvMalfunctionTime = itemView.findViewById(R.id.tvMalfunctionTime);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

}

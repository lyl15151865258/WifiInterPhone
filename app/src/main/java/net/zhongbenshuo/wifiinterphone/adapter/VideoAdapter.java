package net.zhongbenshuo.wifiinterphone.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import net.zhongbenshuo.wifiinterphone.R;
import net.zhongbenshuo.wifiinterphone.activity.chat.VideoActivity;
import net.zhongbenshuo.wifiinterphone.bean.Video;
import net.zhongbenshuo.wifiinterphone.utils.DeviceUtil;

import java.util.List;

/**
 * 视频适配器
 * Created at 2018/12/24 10:43
 *
 * @author Li Yuliang
 * @version 1.0
 */

public class VideoAdapter extends RecyclerView.Adapter {

    private VideoActivity videoActivity;
    private List<Video> videoList;
    private OnItemClickListener mListener;

    public VideoAdapter(VideoActivity videoActivity, List<Video> videoList) {
        this.videoActivity = videoActivity;
        this.videoList = videoList;
    }

    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list_video, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        NewsViewHolder holder = (NewsViewHolder) viewHolder;
        Video video = videoList.get(position);
        RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams((DeviceUtil.getScreenWidth(videoActivity) - 8) / 2, (DeviceUtil.getScreenWidth(videoActivity) - 8) / 2);
        int margin = DeviceUtil.dp2px(videoActivity, 2);
        param.setMargins(margin, margin, margin, margin);
        holder.llRoot.removeAllViews();
        if (video.getSurfaceView() != null) {
            holder.llRoot.addView(video.getSurfaceView(), param);
        }
        holder.itemView.setOnClickListener((v) -> {
            if (mListener != null) {
                mListener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    private class NewsViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout llRoot;

        private NewsViewHolder(View itemView) {
            super(itemView);
            llRoot = itemView.findViewById(R.id.llRoot);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

}

package net.zhongbenshuo.wifiinterphone.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.List;

/**
 * 主页面Viewpager适配器
 * Created at 2018/11/20 13:39
 *
 * @author LiYuliang
 * @version 1.0
 */

public class SelectModuleAdapter extends FragmentPagerAdapter {

    private List<Fragment> fragments;

    public SelectModuleAdapter(FragmentManager fm, List<Fragment> fragments) {
        super(fm);
        this.fragments = fragments;
    }

    @Override
    public Fragment getItem(int position) {
        return fragments.get(position);
    }

    @Override
    public int getCount() {
        return fragments.size();
    }
}

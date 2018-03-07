package com.example.alarmclock.fragment;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.example.alarmclock.adapter.RingSelectAdapter;
import com.example.alarmclock.bean.model.RingSelectItem;
import com.example.alarmclock.common.AlarmClockCommon;
import com.example.alarmclock.util.AudioPlayer;
import com.example.alarmclock.util.MyUtil;
import com.strangeman.alarmclock.R;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2018/2/4.
 */

public class SystemRingFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * 保存铃声信息的Adapter
     */
    RingSelectAdapter mSystemRingAdapter;

    /**
     * loader Id
     */
    private static final int LOADER_ID = 1;

    /**
     * 铃声选择位置
     */
    private int mPosition = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fm_ring_system_ring, container,
                false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 管理cursor
        LoaderManager loaderManager = getLoaderManager();
        // 注册Loader
        loaderManager.initLoader(LOADER_ID, null, this);
        // initAdapter();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Map<String, String> map = mSystemRingAdapter.getItem(position);
        // 取得铃声名
        String ringName = map.get(AlarmClockCommon.RING_NAME);
        // 取得播放地址
        String ringUrl = map.get(AlarmClockCommon.RING_URL);
        // 更新当前铃声选中的位置
        mSystemRingAdapter.updateSelection(ringName);
        // 更新适配器刷新铃声列表显示
        mSystemRingAdapter.notifyDataSetChanged();
        // 设置最后一次选中的铃声选择界面位置为系统铃声界面
        RingSelectItem.getInstance().setRingPager(0);

        // 播放音频文件
        switch (ringUrl) {
            case AlarmClockCommon.DEFAULT_RING_URL:
                // 当为默认铃声时
                AudioPlayer.getInstance(getActivity()).playRaw(
                        R.raw.ring_weac_alarm_clock_default, false, false);
                // 无铃声
                break;
            case AlarmClockCommon.NO_RING_URL:
                AudioPlayer.getInstance(getActivity()).stop();
                break;
            default:
                AudioPlayer.getInstance(getActivity()).play(ringUrl, false, false);
                break;
        }

        ViewPager pager = (ViewPager) getActivity().findViewById(R.id.fragment_ring_select_sort);
        PagerAdapter f = pager.getAdapter();
        LocalMusicFragment localMusicFragment = (LocalMusicFragment) f.instantiateItem(pager, 1);
        // 取消本地音乐选中标记
        if (localMusicFragment.mLocalMusicAdapter != null) {
            localMusicFragment.mLocalMusicAdapter.updateSelection("");
            localMusicFragment.mLocalMusicAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        // 查询内部存储音频文件
        return new CursorLoader(getActivity(),
                MediaStore.Audio.Media.INTERNAL_CONTENT_URI, new String[]{
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATA}, null, null,
                MediaStore.Audio.Media.DISPLAY_NAME);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case LOADER_ID:
                // 当为编辑闹钟状态时，铃声名为闹钟单例铃声名
                String ringName1;
                if (RingSelectFragment.sRingName != null) {
                    ringName1 = RingSelectFragment.sRingName;
                } else {
                    SharedPreferences share = getActivity().getSharedPreferences(
                            AlarmClockCommon.EXTRA_AC_SHARE, Activity.MODE_PRIVATE);
                    // 当为新建闹钟状态时，铃声名为最近一次选择保存的铃声名,没有的话为默认铃声
                    ringName1 = share.getString(AlarmClockCommon.RING_NAME,
                            getString(R.string.default_ring));
                }

                // 过滤重复音频文件的Set
                HashSet<String> set = new HashSet<>();

                //  保存铃声信息的List
                List<Map<String, String>> list = new ArrayList<>();
                // 添加默认铃声
                Map<String, String> defaultRing = new HashMap<>();
                defaultRing.put(AlarmClockCommon.RING_NAME, getString(R.string.default_ring));
                defaultRing.put(AlarmClockCommon.RING_URL, AlarmClockCommon.DEFAULT_RING_URL);
                list.add(defaultRing);
                set.add(getString(R.string.default_ring));

                // 保存的铃声名为默认铃声，设置该列表的显示位置
                if (getString(R.string.default_ring).equals(ringName1)) {
                    mPosition = 0;
                    RingSelectItem.getInstance().setRingPager(0);
                }

                // 添加无铃声
                Map<String, String> noRing = new HashMap<>();
                noRing.put(AlarmClockCommon.RING_NAME, getString(R.string.no_ring));
                noRing.put(AlarmClockCommon.RING_URL, AlarmClockCommon.NO_RING_URL);
                list.add(noRing);
                set.add(getString(R.string.no_ring));

                // 当列表中存在与保存的铃声名一致时，设置该列表的显示位置
                if (getString(R.string.no_ring).equals(ringName1)) {
                    mPosition = list.size() - 1;
                    RingSelectItem.getInstance().setRingPager(0);
                }

                if (cursor != null) {
                    for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor
                            .moveToNext()) {
                        // 音频文件名
                        String ringName = cursor.getString(cursor
                                .getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
                        if (ringName != null) {
                            // 当过滤集合里不存在此音频文件
                            if (!set.contains(ringName)) {
                                // 添加音频文件到列表过滤同名文件
                                set.add(ringName);
                                // 去掉音频文件的扩展名
                                ringName = MyUtil.removeEx(ringName);
                                // 取得音频文件的地址
                                String ringUrl = cursor.getString(cursor
                                        .getColumnIndex(MediaStore.Audio.Media.DATA));
                                Map<String, String> map = new HashMap<>();
                                map.put(AlarmClockCommon.RING_NAME, ringName);
                                map.put(AlarmClockCommon.RING_URL, ringUrl);
                                list.add(map);
                                // 当列表中存在与保存的铃声名一致时，设置该列表的显示位置
                                if (ringName.equals(ringName1)) {
                                    mPosition = list.size() - 1;
                                    RingSelectItem.getInstance().setRingPager(0);
                                }
                            }
                        }
                    }
                }

                mSystemRingAdapter = new RingSelectAdapter(getActivity(), list, ringName1);
                setListAdapter(mSystemRingAdapter);
                setSelection(mPosition);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {

    }

}
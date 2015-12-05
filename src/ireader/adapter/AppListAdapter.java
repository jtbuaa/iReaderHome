package ireader.adapter;


import ireader.home.R;

import java.util.List;

import com.android.settings.net.UidDetailProvider;
import com.way.plistview.PinnedHeaderListView;
import com.way.plistview.PinnedHeaderListView.PinnedHeaderAdapter;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.TextView;

public class AppListAdapter extends AppSelectListAdapter implements PinnedHeaderAdapter, OnScrollListener {

    public void setApps(List<ResolveInfo> apps) {
        mAllApps = apps;
    }
    public void setSections(List<String> sections) {
        mSections = sections;
    }
    public void setPositions(List<Integer> positions) {
        mPositions = positions;
    }
    public AppListAdapter(Context context,
            UidDetailProvider provider,
            List<ResolveInfo> apps,
            List<String> sections,
            List<Integer> positions) {
        super(context, provider, apps, sections, positions);
        mIsSearching = false;
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
            int visibleItemCount, int totalItemCount) {
        if (view instanceof PinnedHeaderListView) {
            ((PinnedHeaderListView) view).configureHeaderView(firstVisibleItem);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView arg0, int arg1) {
    }

    @Override
    public int getPinnedHeaderState(int position) {
        int realPosition = position;
        if (realPosition < 0 || position >= getCount()) {
            return PINNED_HEADER_GONE;
        }
        int section = getSectionForPosition(realPosition);
        int nextSectionPosition = getPositionForSection(section + 1);
        if (nextSectionPosition != -1
                && realPosition == nextSectionPosition - 1) {
            return PINNED_HEADER_PUSHED_UP;
        }
        return PINNED_HEADER_VISIBLE;
    }

    @Override
    public void configurePinnedHeader(View header, int position, int alpha) {
        int realPosition = position;
        int section = getSectionForPosition(realPosition);
        if (section < 0)
            return;
        String title = (String) getSections()[section];
        ((TextView) header.findViewById(R.id.group_title)).setText(title);
    }

    @Override
    public int getCount() {
        return mAllApps.size();
    }

    @Override
    public Object getItem(int position) {
        return mAllApps.get(position);
    }

}
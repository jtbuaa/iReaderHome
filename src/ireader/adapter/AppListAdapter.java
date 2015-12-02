package ireader.adapter;


import ireader.home.R;

import java.util.Arrays;
import java.util.List;

import base.util.Util;

import com.android.settings.net.UidDetail;
import com.android.settings.net.UidDetailProvider;
import com.android.settings.net.UidDetailTask;
import com.way.plistview.PinnedHeaderListView;
import com.way.plistview.PinnedHeaderListView.PinnedHeaderAdapter;

import de.greenrobot.event.EventBus;
import floating.lib.Dragger;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ArrayAdapter;
import android.widget.SectionIndexer;
import android.widget.TextView;

public class AppListAdapter extends ArrayAdapter<ResolveInfo> implements SectionIndexer, PinnedHeaderAdapter, OnScrollListener {
    protected List<ResolveInfo> mAllApps;
    public List<ResolveInfo> mResultApps;
    protected boolean mIsSearching = false;
    private Context mContext;
    private PackageManager mPm;
    private final UidDetailProvider mProvider;
    private List<String> mSections;
    private List<Integer> mPositions;

    public void setSections(List<String> sections) {
        mSections = sections;
    }
    public void setPositions(List<Integer> positions) {
        mPositions = positions;
    }
    public AppListAdapter(Context context, UidDetailProvider provider, List<ResolveInfo> apps, List<String> sections,
            List<Integer> positions) {
        super(context, 0, apps);
        mAllApps = apps;
        mContext = context;
        mPm = mContext.getPackageManager();
        mProvider = provider;
        mSections = sections;
        mPositions = positions;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = ((Activity) mContext).getLayoutInflater().inflate(R.layout.app_item, parent, false);
            convertView.findViewById(R.id.app_item).setOnClickListener(launchClickListener);
            convertView.findViewById(R.id.version_name).setOnClickListener(uninstallClickListener);
        }
        final TextView title = (TextView) convertView.findViewById(R.id.app_name);
        final TextView versionName = (TextView) convertView.findViewById(R.id.version_name);
        final TextView packageName = (TextView) convertView.findViewById(R.id.package_name);
        ResolveInfo info;
        if (mIsSearching) {
            if (mResultApps == null || mResultApps.isEmpty() || position >= mResultApps.size()) {
                return convertView;
            }
            info = mResultApps.get(position);
        } else {
            info = mAllApps.get(position);
        }
        title.setText(Util.getLabel(info));
        packageName.setText(info.activityInfo.packageName);
        try {
            String version = mPm.getPackageInfo(info.activityInfo.packageName, 0).versionName;
            if ((version == null) || (version.trim().equals("")))
                version = String.valueOf(mPm.getPackageInfo(info.activityInfo.packageName, 0).versionCode);
            versionName.setText(version);
        } catch (NameNotFoundException e) {
            versionName.setText(e.toString());
        }

        TextView group = (TextView) convertView.findViewById(R.id.group_title);
        int section = getSectionForPosition(position);
        if (getPositionForSection(section) == position) {
            group.setVisibility(View.VISIBLE);
            group.setText(mSections.get(section));
        } else {
            group.setVisibility(View.GONE);
        }
        UidDetailTask.bindView(mProvider, info, convertView);

        return convertView;
    }

    private OnClickListener launchClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            UidDetail detail = (UidDetail)view.findViewById(R.id.version_name).getTag();
            if (detail == null || ((Activity) mContext).getComponentName().getPackageName().equals(detail.packageName)) {
                return;
            }
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setComponent(new ComponentName(detail.packageName,
                    detail.className));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            EventBus.getDefault().post(intent);
            try {
                mContext.startActivity(intent);
                mContext.startService(new Intent(mContext, Dragger.class));
            } catch(ActivityNotFoundException e) {}
        }
    };

    private OnClickListener uninstallClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            UidDetail detail = (UidDetail)view.getTag();
            if (detail == null) {
                return;
            }
            Uri uri = Uri.fromParts("package", detail.packageName , null);
            Intent intent = new Intent(Intent.ACTION_DELETE, uri);
            try {
                mContext.startActivity(intent);
            } catch(ActivityNotFoundException e) {}
        }
    };

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
    public int getPositionForSection(int section) {
        if (section < 0 || section >= mPositions.size()) {
            return -1;
        }
        return mPositions.get(section);
    }

    @Override
    public int getSectionForPosition(int position) {
        if (position < 0 || position >= getCount()) {
            return -1;
        }
        int index = Arrays.binarySearch(mPositions.toArray(), position);
        return index >= 0 ? index : -index - 2;
    }

    @Override
    public Object[] getSections() {
        return mSections.toArray();
    }

}
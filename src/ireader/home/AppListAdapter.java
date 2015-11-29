package ireader.home;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.way.plistview.PinnedHeaderListView.PinnedHeaderAdapter;

import de.greenrobot.event.EventBus;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.SectionIndexer;
import android.widget.TextView;

public class AppListAdapter extends ArrayAdapter<ResolveInfo> implements SectionIndexer, PinnedHeaderAdapter {
    private ArrayList localApplist;
    private Context mContext;
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
        localApplist = (ArrayList) apps;
        mContext = context;
        mContext.getPackageManager();
        mProvider = provider;
        mSections = sections;
        mPositions = positions;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = ((Activity) mContext).getLayoutInflater().inflate(R.layout.app_item, parent, false);
            convertView.setOnClickListener(clickListener);
            convertView.findViewById(R.id.version_name).setOnClickListener(uninstallClickListener);
        }
        TextView group = (TextView) convertView.findViewById(R.id.group_title);
        int section = getSectionForPosition(position);
        if (getPositionForSection(section) == position) {
            group.setVisibility(View.VISIBLE);
            group.setText(mSections.get(section));
        } else {
            group.setVisibility(View.GONE);
        }
        UidDetailTask.bindView(mProvider, (ResolveInfo) localApplist.get(position), convertView);

        return convertView;
    }

    private OnClickListener clickListener = new OnClickListener() {
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
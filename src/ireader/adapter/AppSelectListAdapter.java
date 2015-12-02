package ireader.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.widget.Filter;
import android.widget.Filterable;
import base.util.Util;

import com.android.settings.net.UidDetailProvider;

public class AppSelectListAdapter extends AppListAdapter implements Filterable {

    public AppSelectListAdapter(Context context, UidDetailProvider provider,
            List<ResolveInfo> apps, List<String> sections, List<Integer> positions) {
        super(context, provider, apps, sections, positions);
    }

    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence constraint,
                    FilterResults results) {
                mResultApps = (ArrayList<ResolveInfo>) results.values;
                if (results.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }

            protected FilterResults performFiltering(CharSequence s) {
                String str = s.toString().toLowerCase();
                // mFilterStr = str;
                FilterResults results = new FilterResults();
                ArrayList<ResolveInfo> appList = new ArrayList<ResolveInfo>();
                if (mAllApps != null && mAllApps.size() > 0) {
                    for (ResolveInfo info : mAllApps) {
                        // match label or first character
                        if (Util.getLabel(info).indexOf(str) > -1
                                || Util.getPinyin(info).indexOf(str) > -1) {
                            appList.add(info);
                        }
                    }
                }
                results.values = appList;
                results.count = appList.size();
                return results;
            }
        };
        return filter;
    }

}

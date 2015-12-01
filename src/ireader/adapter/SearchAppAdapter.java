
package ireader.adapter;

import ireader.home.R;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

public class SearchAppAdapter extends BaseAdapter implements Filterable {

    private List<ResolveInfo> mAllApps;
    private List<ResolveInfo> mResultApps;
    private LayoutInflater mInflater;
    private Context mContext;

    public SearchAppAdapter(Context context, List<ResolveInfo> allApps) {
        mContext = context;
        mAllApps = allApps;
        mResultApps = new ArrayList<ResolveInfo>();
        mInflater = LayoutInflater.from(mContext);
    }

    @Override
    public int getCount() {
        return mResultApps.size();
    }

    @Override
    public ResolveInfo getItem(int position) {
        return mResultApps.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.search_city_item, null);
        }
        TextView provinceTv = (TextView) convertView
                .findViewById(R.id.search_province);
        provinceTv.setText(mResultApps.get(position).getProvince());
        TextView cityTv = (TextView) convertView
                .findViewById(R.id.column_title);
        cityTv.setText(mResultApps.get(position).getName());
        return convertView;
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
                if (mAllApps != null && mAllApps.size() != 0) {
                    for (ResolveInfo info : mAllApps) {
                        // match label or first character
                        if (((ReaderResolveInfo) info).getLabel().indexOf(str) > -1
                                || ((ReaderResolveInfo) info).getFirstCharacter().indexOf(str) > -1) {
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

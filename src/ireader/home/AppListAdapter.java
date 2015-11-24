package ireader.home;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class AppListAdapter extends ArrayAdapter<ResolveInfo> {
    ArrayList localApplist;
    Context mContext;
    PackageManager mPm;

    private class ViewHolder {
        RelativeLayout appItem;
        TextView appName, versionName, packageName;
        ImageView appIcon;
        ResolveInfo info;
    }

    public AppListAdapter(Context context, List<ResolveInfo> apps) {
        super(context, 0, apps);
        localApplist = (ArrayList) apps;
        mContext = context;
        mPm = mContext.getPackageManager();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = ((Activity) mContext).getLayoutInflater().inflate(R.layout.app_list, parent, false);
            holder = new ViewHolder();
            holder.appItem = (RelativeLayout) convertView.findViewById(R.id.app_item);
            holder.appIcon = (ImageView) convertView.findViewById(R.id.app_icon);
            holder.appName = (TextView) convertView.findViewById(R.id.app_name);
            holder.versionName = (TextView) convertView.findViewById(R.id.version_name);
            holder.packageName = (TextView) convertView.findViewById(R.id.package_name);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }
        holder.info = (ResolveInfo) localApplist.get(position);
        holder.appItem.setOnClickListener(clickListener);
        holder.appIcon.setImageDrawable(holder.info.loadIcon(mPm));
        holder.appIcon.setOnClickListener(clickListener);
        holder.appName.setText(holder.info.loadLabel(mPm).toString());
        holder.appName.setOnClickListener(clickListener);
        holder.packageName.setText(holder.info.activityInfo.packageName);
        try {
            String version = mPm.getPackageInfo(holder.info.activityInfo.packageName, 0).versionName;
            if ((version == null) || (version.trim().equals("")))
                version = String.valueOf(mPm.getPackageInfo(holder.info.activityInfo.packageName, 0).versionCode);
            holder.versionName.setText(version);
        } catch (NameNotFoundException e) {
            holder.versionName.setText(e.toString());
        }

        return convertView;
    }

    OnClickListener clickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            ViewHolder holder = (ViewHolder) ((View) view.getParent()).getTag();
            if (holder == null) {
                return;
            }
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.setComponent(new ComponentName(holder.info.activityInfo.applicationInfo.packageName,
                    holder.info.activityInfo.name));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                mContext.startActivity(i);
                mContext.startService(new Intent(mContext, Dragger.class));
            } catch(ActivityNotFoundException e) {}
        }
    };
}
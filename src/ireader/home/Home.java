package ireader.home;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class Home extends Activity {

    List<ResolveInfo> mAllApps;
    ListView mAppListView;
    AppListAdapter mAppListAdapter;
    PackageManager mPm;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        getAllApp();
        mAppListAdapter = new AppListAdapter(this, mAllApps);
        mAppListView = (ListView) findViewById(R.id.apps);
        mAppListView.setAdapter(mAppListAdapter);
    }

    private void getAllApp() {
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mPm = getPackageManager();
        mAllApps = mPm.queryIntentActivities(mainIntent, 0);
        removeInfo(getComponentName().getPackageName());
    }

    private ResolveInfo removeInfo(String packageName) {
        for (int i = 0; i < mAllApps.size(); i++) {
            ResolveInfo info = mAllApps.get(i);
            if (info.activityInfo.packageName.equals(packageName)) {
                mAllApps.remove(i);
                return info;
            }
        }
        return null;
    }

    BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String packageName = intent.getDataString().split(":")[1];
            if (action.equals(Intent.ACTION_PACKAGE_REMOVED)) {
                ResolveInfo info = removeInfo(packageName);
            } else if (action.equals(Intent.ACTION_PACKAGE_ADDED)) {
                Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                mainIntent.setPackage(packageName);
                List<ResolveInfo> targetApps = mPm.queryIntentActivities(mainIntent, 0);
                // the new package may not support launcher. so filter it first
                for (int i = 0; i < targetApps.size(); i++) {
                    ResolveInfo info = targetApps.get(i);
                    if (info.activityInfo.packageName.equals(packageName)) {
                        mAllApps.add(info);
                        break;
                    }
                }
            }
        }
    };

    private class ViewHolder {
        RelativeLayout appItem;
        TextView appName, versionName, packageName;
        ImageView appIcon;
    }

    private class AppListAdapter extends ArrayAdapter<ResolveInfo> {
        ArrayList localApplist;
        ResolveInfo info;

        public AppListAdapter(Context context, List<ResolveInfo> apps) {
            super(context, 0, apps);
            localApplist = (ArrayList) apps;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            info = (ResolveInfo) localApplist.get(position);
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.app_list, parent, false);
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
            holder.appItem.setOnClickListener(clickListener);
            holder.appIcon.setImageDrawable(info.loadIcon(mPm));
            holder.appIcon.setOnClickListener(clickListener);
            holder.appName.setText(info.loadLabel(mPm).toString());
            holder.appName.setOnClickListener(clickListener);
            holder.packageName.setText(info.activityInfo.packageName);
            try {
                String version = mPm.getPackageInfo(info.activityInfo.packageName, 0).versionName;
                if ((version == null) || (version.trim().equals("")))
                    version = String.valueOf(mPm.getPackageInfo(info.activityInfo.packageName, 0).versionCode);
                holder.versionName.setText(version);
            } catch (NameNotFoundException e) {
                holder.versionName.setText(e.toString());
            }

            return convertView;
        }

        OnClickListener clickListener = new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent i = new Intent(Intent.ACTION_MAIN);
                i.setComponent(new ComponentName(info.activityInfo.applicationInfo.packageName,
                        info.activityInfo.name));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Home.this.startActivity(i);
            }
        };
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getRepeatCount() == 0) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                return true;
            }
        }
        return false;
    }

}

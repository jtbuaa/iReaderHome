package ireader.home;

import java.util.Collections;
import java.util.List;

import de.greenrobot.event.EventBus;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

public class Home extends Activity {

    List<ResolveInfo> mAllApps;
    ListView mAppListView;
    AppListAdapter mAppListAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        getAllApp();
        mAppListAdapter = new AppListAdapter(this, mAllApps);
        mAppListView = (ListView) findViewById(R.id.apps);
        mAppListView.setVisibility(View.VISIBLE);
        mAppListView.setAdapter(mAppListAdapter);
        ProgressBar pbar = (ProgressBar) findViewById(R.id.loading);
        pbar.setVisibility(View.GONE);

        // for package add/remove
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        registerReceiver(packageReceiver, filter);

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        unregisterReceiver(packageReceiver);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        stopService(new Intent(this, Dragger.class));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getRepeatCount() == 0) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (mIntent != null) {
                    try {
                        startActivity(mIntent);
                        startService(new Intent(Home.this, Dragger.class));
                    } catch(ActivityNotFoundException e) {}
                }
                return true;
            }
        }
        return false;
    }

    Intent mIntent;
    public void onEventMainThread(Intent intent) {
        mIntent = intent;
    }

    PackageManager mPm;
    private void getAllApp() {
        mPm = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mAllApps = mPm.queryIntentActivities(mainIntent, 0);
        removeInfo(getComponentName().getPackageName());
        for (int i = 0; i < mAllApps.size(); i++) {
            prepareInfo(mAllApps.get(i));
        }
        Collections.sort(mAllApps, new StringComparator());// sort by name
    }

    private void removeInfo(String packageName) {
        for (int i = 0; i < mAllApps.size(); i++) {
            ResolveInfo info = mAllApps.get(i);
            if (info.activityInfo.packageName.equals(packageName)) {
                mAllApps.remove(i);
                break;
            }
        }
    }

    HanziToPinyin mTo = HanziToPinyin.getInstance();
    private void prepareInfo(ResolveInfo info) {
        // borrow the dataDir to store label, for loadLabel() is very time consuming
        info.activityInfo.applicationInfo.dataDir = (String) info.loadLabel(mPm);
        if (info.activityInfo.applicationInfo.dataDir.length() < 1) {
            info.activityInfo.applicationInfo.dataDir = " ";
            info.activityInfo.applicationInfo.nativeLibraryDir = " ";
        } else {
            info.activityInfo.applicationInfo.nativeLibraryDir = mTo.getToken(info.activityInfo.applicationInfo.dataDir.charAt(0)).target;
        }
    }

    BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String packageName = intent.getDataString().split(":")[1];
            if (action.equals(Intent.ACTION_PACKAGE_REMOVED)) {
                removeInfo(packageName);
                mAppListAdapter.notifyDataSetChanged();
            } else if (action.equals(Intent.ACTION_PACKAGE_ADDED)) {
                Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                mainIntent.setPackage(packageName);
                List<ResolveInfo> targetApps = mPm.queryIntentActivities(mainIntent, 0);
                // the new package may not support launcher. so filter it first
                for (int i = 0; i < targetApps.size(); i++) {
                    ResolveInfo info = targetApps.get(i);
                    if (info.activityInfo.packageName.equals(packageName)) {
                        prepareInfo(info);
                        mAllApps.add(info);
                        Collections.sort(mAllApps, new StringComparator());// sort by name
                        mAppListAdapter.notifyDataSetChanged();
                        break;
                    }
                }
            }
        }
    };

}

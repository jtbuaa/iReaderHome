package ireader.home;

import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.ListView;

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
        mAppListView.setAdapter(mAppListAdapter);

        // for package add/remove
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        registerReceiver(packageReceiver, filter);
    }

    @Override
    protected void onDestroy() {
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
                return true;
            }
        }
        return false;
    }

    private void getAllApp() {
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mAllApps = getPackageManager().queryIntentActivities(mainIntent, 0);
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
                List<ResolveInfo> targetApps = getPackageManager().queryIntentActivities(mainIntent, 0);
                // the new package may not support launcher. so filter it first
                for (int i = 0; i < targetApps.size(); i++) {
                    ResolveInfo info = targetApps.get(i);
                    if (info.activityInfo.packageName.equals(packageName)) {
                        mAllApps.add(info);
                        break;
                    }
                }
            }
            mAppListAdapter.notifyDataSetChanged();
        }
    };

}

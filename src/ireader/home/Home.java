package ireader.home;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.way.plistview.PinnedHeaderListView;

import de.greenrobot.event.EventBus;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;

public class Home extends Activity {

    private List<ResolveInfo> mTmpAllApps, mAllApps;
    private static final int MIN_SIZE = 20;
    private PinnedHeaderListView mAppListView;
    private AppListAdapter mAppListAdapter;
    private UidDetailProvider mUidDetailProvider;
    // collection of first character of apps
    private List<String> mSections = new ArrayList<String>();
    // position of each character
    private List<Integer> mPositions = new ArrayList<Integer>();;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        getAllApp();
        mUidDetailProvider = new UidDetailProvider(this);
        mAppListAdapter = new AppListAdapter(this, mUidDetailProvider, mAllApps, mSections, mPositions);
        mAppListView = (PinnedHeaderListView) findViewById(R.id.apps);
        mAppListView.setVisibility(View.VISIBLE);
        mAppListView.setAdapter(mAppListAdapter);
        mAppListView.setOnScrollListener(mAppListAdapter);
        mAppListView.setPinnedHeaderView(LayoutInflater.from(
                this).inflate(
                R.layout.biz_plugin_weather_list_group_item, mAppListView,
                false));

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

    private void preparePosition() {
        mPositions.clear();
        mPositions.add(0);
        String current = mAllApps.get(0).activityInfo.applicationInfo.nativeLibraryDir;
        for (int i = 0; i < mAllApps.size(); i++) {
            if (current.matches(FORMAT) || (!current.matches(FORMAT) && mAllApps.get(i).activityInfo.applicationInfo.nativeLibraryDir.matches(FORMAT))) {
                if (!current.equals(mAllApps.get(i).activityInfo.applicationInfo.nativeLibraryDir)) {
                    mPositions.add(i);
                    current = mAllApps.get(i).activityInfo.applicationInfo.nativeLibraryDir;
                }
            }
        }
    }

    PackageManager mPm;
    private void getAllApp() {
        mPm = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mTmpAllApps = mPm.queryIntentActivities(mainIntent, 0);
        if (mTmpAllApps.size() <= MIN_SIZE) {
            mAllApps = mTmpAllApps;
            mTmpAllApps = null;
            for (int i = 0; i < mAllApps.size(); i++) {
                prepareInfo(mAllApps.get(i));
            }
            Collections.sort(mAllApps, new StringComparator());// sort by name
            Collections.sort(mSections);
            preparePosition();
        } else {
            mAllApps = new ArrayList<ResolveInfo>();
            for (int i = 0; i < MIN_SIZE; i++) {
                mAllApps.add(mTmpAllApps.remove(i));
                prepareInfo(mAllApps.get(i));
            }
        }

        if (mTmpAllApps != null && mTmpAllApps.size() > 0) {
            // use AsyncTask to handle more data
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    for (int i = 0; i < mTmpAllApps.size(); i++) {
                        prepareInfo(mTmpAllApps.get(i));
                    }
                    mHandler.sendEmptyMessage(0);
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private class AppHandler extends Handler {
        public void handleMessage(Message msg) {
            while (mTmpAllApps.size() > 0) {
                mAllApps.add(mTmpAllApps.remove(0));
            }
            Collections.sort(mAllApps, new StringComparator());// sort by name
            Collections.sort(mSections);
            preparePosition();
            mAppListAdapter.setSections(mSections);
            mAppListAdapter.setPositions(mPositions);
            mAppListAdapter.notifyDataSetChanged();
        }
    }
    AppHandler mHandler = new AppHandler();

    private void removeInfo(String packageName) {
        for (int i = 0; i < mAllApps.size(); i++) {
            ResolveInfo info = mAllApps.get(i);
            if (info.activityInfo.packageName.equals(packageName)) {
                mAllApps.remove(i);
                break;
            }
        }
    }

    private static final String FORMAT = "^[A-Z]+$";
    HanziToPinyin mTo = HanziToPinyin.getInstance();
    private void prepareInfo(ResolveInfo info) {
        String firstName = " ";
        // borrow the dataDir to store label, for loadLabel() is very time consuming
        // use nativeLibraryDir to store first character
        info.activityInfo.applicationInfo.dataDir = (String) info.loadLabel(mPm);
        if (info.activityInfo.applicationInfo.dataDir.length() < 1) {
            info.activityInfo.applicationInfo.dataDir = " ";
        } else {
            firstName = mTo.getToken(info.activityInfo.applicationInfo.dataDir.charAt(0)).target;
        }
        firstName = firstName.substring(0, 1).toUpperCase();
        info.activityInfo.applicationInfo.nativeLibraryDir = firstName;
        if (firstName.matches(FORMAT)) {
            if (!mSections.contains(firstName)) {
                mSections.add(firstName);
            }
        } else {
            if (!mSections.contains("#")) {
                mSections.add("#");
            }
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

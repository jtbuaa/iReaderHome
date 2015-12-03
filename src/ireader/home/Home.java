package ireader.home;


import ireader.adapter.AppListAdapter;
import ireader.adapter.AppSelectListAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import base.util.HanziToPinyin;
import base.util.StringComparator;
import base.util.Util;

import com.android.settings.net.UidDetailProvider;
import com.way.plistview.BladeView;
import com.way.plistview.PinnedHeaderListView;
import com.way.plistview.BladeView.OnItemClickListener;

import de.greenrobot.event.EventBus;
import floating.lib.Dragger;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

public class Home extends Activity implements TextWatcher {

    private List<ResolveInfo> mTmpAllApps, mAllApps;
    private static final int MIN_SIZE = 20;
    private PinnedHeaderListView mAppListView;
    private ListView mSearchListView;
    private View mAppContainer, mSearchContainer;
    private BladeView mLetter;
    private AppListAdapter mAppListAdapter;
    private AppSelectListAdapter mAppSelectListAdapter;
    private UidDetailProvider mUidDetailProvider;
    // collection of first character of apps
    private List<String> mSections = new ArrayList<String>();
    // position of each first-character
    private List<Integer> mPositions = new ArrayList<Integer>();
    // collection of positions
    private Map<String, Integer> mIndexer = new HashMap<String, Integer>();

    private EditText mSearchEditText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        mAppListView = (PinnedHeaderListView) findViewById(R.id.apps);
        mAppListView.setEmptyView(findViewById(R.id.app_list_empty));
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                getAllApp();
                mUidDetailProvider = new UidDetailProvider(Home.this);
                mAppListAdapter = new AppListAdapter(Home.this, mUidDetailProvider, mAllApps, mSections, mPositions);
                mHandler.sendEmptyMessage(0);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
            }
        }.execute(null, null, null);
        mAppListView.setPinnedHeaderView(LayoutInflater.from(
                this).inflate(
                R.layout.biz_plugin_weather_list_group_item, mAppListView,
                false));

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        mLetter = (BladeView) findViewById(R.id.app_bladeview);
        mLetter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(String s) {
                if (!s.matches(FORMAT)) {
                    s = "#";
                }
                if (mIndexer.get(s) != null) {
                    mAppListView.setSelection(mIndexer.get(s));
                }
            }
        });
        mLetter.setCharHeight(dm.density);

        mSearchEditText = (EditText) findViewById(R.id.search_edit);
        mSearchEditText.addTextChangedListener(this);

        mAppContainer = findViewById(R.id.app_content_container);
        mSearchContainer = findViewById(R.id.search_content_container);
        mSearchListView = (ListView) findViewById(R.id.search_list);

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

    private static final String FORMAT = "^[A-Z]+$";
    HanziToPinyin mTo = HanziToPinyin.getInstance();
    private void prepareInfo(ResolveInfo info) {
        String label = (String) info.loadLabel(mPm);
        if (TextUtils.isEmpty(label)) {
            label = info.activityInfo.name;
        }
        Util.setLabel(info, label);

        StringBuilder pinyin = new StringBuilder("");
        for (int i = 0; i < label.length(); i++) {
            pinyin.append(mTo.getToken(label.charAt(i)).target);
        }
        Util.setPinyin(info, pinyin.toString().toUpperCase());

        try {
            String version = mPm.getPackageInfo(info.activityInfo.packageName, 0).versionName;
            if ((version == null) || (version.trim().equals("")))
                version = String.valueOf(mPm.getPackageInfo(info.activityInfo.packageName, 0).versionCode);
            Util.setVersion(info, version);
        } catch (NameNotFoundException e) {
            Util.setVersion(info, e.toString());
        }
    }

    private void preparePosition() {
        mSections.clear();
        mPositions.clear();
        mIndexer.clear();
        for (int i = 0; i < mAllApps.size(); i++) {
            String firstName = Util.getPinyin(mAllApps.get(i)).substring(0, 1);
            if (firstName.matches(FORMAT)) {
                if (!mSections.contains(firstName)) {
                    mSections.add(firstName);
                    mPositions.add(i);
                    mIndexer.put(firstName, i);
                }
            } else {
                if (!mSections.contains("#")) {
                    mSections.add("#");
                    mPositions.add(i);
                    mIndexer.put("#", i);
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
            preparePosition();
        } else {
            mAllApps = new ArrayList<ResolveInfo>();
            for (int i = 0; i < MIN_SIZE; i++) {
                mAllApps.add(mTmpAllApps.remove(0));
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
                    mHandler.sendEmptyMessage(1);
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                }
            }.execute(null, null, null);
        }
    }

    private class AppHandler extends Handler {
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case 0:
                    mAppListView.setAdapter(mAppListAdapter);
                    mAppListView.setOnScrollListener(mAppListAdapter);
                    break;
                case 1:
                    while (mTmpAllApps.size() > 0) {
                        mAllApps.add(mTmpAllApps.remove(0));
                    }
                    Collections.sort(mAllApps, new StringComparator());// sort by name
                    prepareAll();
                    break;
            }
        }
    }
    AppHandler mHandler = new AppHandler();

    private void prepareAll() {
        preparePosition();
        mAppListAdapter.setSections(mSections);
        mAppListAdapter.setPositions(mPositions);
        mAppListAdapter.notifyDataSetChanged();
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

    BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String packageName = intent.getDataString().split(":")[1];
            if (action.equals(Intent.ACTION_PACKAGE_REMOVED)) {
                removeInfo(packageName);
                prepareAll();
            } else if (action.equals(Intent.ACTION_PACKAGE_ADDED)) {
                Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                mainIntent.setPackage(packageName);
                List<ResolveInfo> targetApps = mPm.queryIntentActivities(mainIntent, 0);
                // the new package may not support launcher. so filter it at first
                for (int i = 0; i < targetApps.size(); i++) {
                    ResolveInfo info = targetApps.get(i);
                    if (info.activityInfo.packageName.equals(packageName)) {
                        prepareInfo(info);
                        mAllApps.add(info);
                        Collections.sort(mAllApps, new StringComparator());// sort by name
                        prepareAll();
                        break;
                    }
                }
            }
        }
    };

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (TextUtils.isEmpty(s)) {
            mAppContainer.setVisibility(View.VISIBLE);
            mSearchContainer.setVisibility(View.GONE);
            mAppListView.setAdapter(mAppListAdapter);
        } else {
            mAppContainer.setVisibility(View.GONE);
            mSearchContainer.setVisibility(View.VISIBLE);
            mAppSelectListAdapter = new AppSelectListAdapter(Home.this, mUidDetailProvider, mAllApps, mSections, mPositions);
            mSearchListView.setTextFilterEnabled(true);
            mSearchListView.setAdapter(mAppSelectListAdapter);
            mAppSelectListAdapter.getFilter().filter(s);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

}

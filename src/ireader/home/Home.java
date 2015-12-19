package ireader.home;


import ireader.adapter.AppListAdapter;
import ireader.adapter.AppSelectListAdapter;
import ireader.provider.UidDetailDbProvider;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import base.util.StringComparator;
import base.util.TaskHelper;
import base.util.Util;

import com.android.settings.net.UidDetail;
import com.android.settings.net.UidDetailProvider;
import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.github.promeg.pinyinhelper.Pinyin;
import com.way.plistview.BladeView;
import com.way.plistview.PinnedHeaderListView;
import com.way.plistview.BladeView.OnItemClickListener;

import de.greenrobot.event.EventBus;
import fi.iki.asb.android.logo.TextViewUndoRedo;
import floating.lib.Dragger;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;

public class Home extends Activity implements TextWatcher {

    private List<UidDetail> mAllApps, mSystemApps, mUserApps, mCurrentApps;
    private PinnedHeaderListView mAppListView;
    private ListView mSearchListView;
    private View mAppContainer, mSearchContainer;
    private View mShadowView;
    private AnimatorSet mStartSearchAnimatorSet, mStopSearchAnimatorSet;
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
    private TextViewUndoRedo mUndoRedo;
    private InputMethodManager mInputManager;

    private static final int APP_ALL = 0;
    private static final int APP_SYSTEM = 1;
    private static final int APP_USER = 2;
    //private static final int APP_GRIDVIEW = 3;
    private int mAppGroup = APP_ALL;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        mAppListView = (PinnedHeaderListView) findViewById(R.id.apps);
        mAppListView.setEmptyView(findViewById(R.id.app_list_empty));
        AsyncTask<Void, Void, Void> firstTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                getAllApp();
                mUidDetailProvider = new UidDetailProvider(Home.this);
                mAppListAdapter = new AppListAdapter(Home.this, mUidDetailProvider, mAllApps, mSections, mPositions);
                mHandler.sendEmptyMessage(GET_ALL_OK);
                return null;
            }
        };
        TaskHelper.execute(firstTask);
        mAppListView.setPinnedHeaderView(
                LayoutInflater.from(this).inflate(R.layout.biz_plugin_weather_list_group_item, mAppListView,
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
        mSearchEditText.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                startSearchBarAnimation();
                return false;
            }
        });
        mUndoRedo = new TextViewUndoRedo(mSearchEditText);
        mInputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        mAppContainer = findViewById(R.id.app_content_container);
        mSearchContainer = findViewById(R.id.search_content_container);
        mSearchListView = (ListView) findViewById(R.id.search_list);
        mSearchListView.setEmptyView(findViewById(R.id.search_empty));

        mShadowView = findViewById(R.id.shadow_view);
        mShadowView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSearchBarAnimation();
            }
        });

        // step 1. create a MenuCreator
        SwipeMenuCreator creator = new SwipeMenuCreator() {
            @Override
            public void create(SwipeMenu menu) {
                // create "open" item
                SwipeMenuItem openItem = new SwipeMenuItem(
                        getApplicationContext());
                // set item background
                openItem.setBackground(new ColorDrawable(Color.rgb(0xC9, 0xC9,
                        0xCE)));
                // set item width
                openItem.setWidth(dp2px(90));
                // set item title
                //openItem.setTitle("Open");
                openItem.setIcon(R.drawable.ic_setting);
                // set item title fontsize
                openItem.setTitleSize(18);
                // set item title font color
                openItem.setTitleColor(Color.WHITE);
                // add to menu
                menu.addMenuItem(openItem);

                // create "delete" item
                SwipeMenuItem deleteItem = new SwipeMenuItem(
                        getApplicationContext());
                // set item background
                deleteItem.setBackground(new ColorDrawable(Color.rgb(0xF9,
                        0x3F, 0x25)));
                // set item width
                deleteItem.setWidth(dp2px(90));
                // set a icon
                deleteItem.setIcon(R.drawable.ic_delete);
                // add to menu
                menu.addMenuItem(deleteItem);
            }
        };
        // set creator
        mAppListView.setMenuCreator(creator);

        // step 2. listener item click event
        mAppListView.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
                UidDetail detail = (UidDetail) mAppListAdapter.getItem(position);
                Uri uri = Uri.fromParts("package", detail.packageName , null);
                Intent intent = null;
                switch (index) {
                    case 0:
                        // setting
                        intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri);
                        break;
                    case 1:
                        // delete
                        intent = new Intent(Intent.ACTION_DELETE, uri);
                        break;
                }
                try {
                    startActivity(intent);
                } catch(ActivityNotFoundException e) {}
                return false;
            }
        });

        // set SwipeListener
        mAppListView.setOnSwipeListener(new SwipeMenuListView.OnSwipeListener() {
            @Override
            public void onSwipeStart(int position) {
            }

            @Override
            public void onSwipeEnd(int position) {
            }
        });

        // set MenuStateChangeListener
        mAppListView.setOnMenuStateChangeListener(new SwipeMenuListView.OnMenuStateChangeListener() {
            @Override
            public void onMenuOpen(int position) {
                mLetter.setVisibility(View.GONE);
            }

            @Override
            public void onMenuClose(int position) {
                mLetter.setVisibility(View.VISIBLE);
            }
        });

        // for package add/remove
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        registerReceiver(packageReceiver, filter);

        EventBus.getDefault().register(this);
    }

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
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
        mLetter.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getRepeatCount() == 0) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (mShadowView.getVisibility() == View.VISIBLE) {
                    stopSearchBarAnimation();
                } else if (mAppContainer.getVisibility() == View.VISIBLE) {
                    if (mIntent != null) {
                        try {
                            startActivity(mIntent);
                            startService(new Intent(Home.this, Dragger.class));
                        } catch(ActivityNotFoundException e) {}
                    }
                } else {
                    if (mUndoRedo.getCanUndo()) {
                        mUndoRedo.undo();
                    } else if (mUndoRedo.getCanRedo()) {
                        mUndoRedo.redo();
                    }
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
    private UidDetail prepareInfo(ResolveInfo info, boolean block) {
        UidDetail detail = new UidDetail();
        detail.info = info;

        String label = (String) info.loadLabel(mPm);
        if (TextUtils.isEmpty(label)) {
            label = info.activityInfo.name;
        }
        detail.label = label;

        detail.className = info.activityInfo.name;
        detail.hashCode = info.activityInfo.packageName.hashCode();
        detail.isSystem = (info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM;
        detail.packageName = info.activityInfo.packageName;
        detail.sourceDir = info.activityInfo.applicationInfo.sourceDir;
        if (block) {
            Util.extractDetail(detail, mPm);
            Util.update(detail, getContentResolver());
        } else {
            detail.pinyin = Pinyin.toPinyin(label.charAt(0)).toUpperCase();
        }
        return detail;
    }

    private void preparePosition() {
        mSections.clear();
        mPositions.clear();
        mIndexer.clear();
        switch (mAppGroup) {
            case APP_ALL:
                mCurrentApps = mAllApps;
                break;
            case APP_SYSTEM:
                mCurrentApps = mSystemApps;
                break;
            case APP_USER:
                mCurrentApps = mUserApps;
                break;
        }
        Collections.sort(mCurrentApps, new StringComparator());// sort by name
        for (int i = 0; i < mCurrentApps.size(); i++) {
            String firstName = mCurrentApps.get(i).pinyin.substring(0, 1);
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
        if (mAppListAdapter != null) {
            mAppListAdapter.setApps(mCurrentApps);
        }
    }

    PackageManager mPm;
    private void getAllApp() {
        mAllApps = new ArrayList<UidDetail>();
        mSystemApps = new ArrayList<UidDetail>();
        mUserApps = new ArrayList<UidDetail>();
        mPm = getPackageManager();
        Cursor cursor = getContentResolver().query(UidDetailDbProvider.CONTENT_URI_APP_DETAIL, null, null, null, UidDetailDbProvider.PINYIN);
        if (cursor != null && cursor.moveToFirst()) {
            while (cursor.moveToNext()) {
                UidDetail detail = new UidDetail();
                Util.query(detail, cursor);
                if (detail.label != null) {
                    mAllApps.add(detail);
                    if (detail.isSystem) {
                        mSystemApps.add(detail);
                    } else {
                        mUserApps.add(detail);
                    }
                }
            }
            cursor.close();
            if (mAllApps.size() == 0) {
                // sth wrong when read db. still read from package manager
                queryMain(false);
            } else {
                syncDB();
            }
        } else {
            queryMain(false);
        }
        preparePosition();
    }

    private void syncDB() {
        for (int i = 0; i < mAllApps.size(); i++) {
            if (mAllApps.get(i).icon == null) {
                Util.queryIcon(mAllApps.get(i), getContentResolver());
            }
        }
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = mPm.queryIntentActivities(mainIntent, 0);
        for (int i = 0; i < apps.size(); i++) {
            ResolveInfo info = apps.get(i);
            String packageName = info.activityInfo.packageName;
            String versionName;
            try {
                versionName = mPm.getPackageInfo(packageName, 0).versionName;
                if ((versionName == null) || (versionName.trim().equals("")))
                    versionName = String.valueOf(mPm.getPackageInfo(packageName, 0).versionCode);
            } catch (NameNotFoundException e) {
                versionName = e.toString();
            }
            boolean found = false;

            for (int j = 0; j < mAllApps.size(); j++) {
                if (mAllApps.get(j).packageName.equals(info.activityInfo.packageName)) {
                    found = true;
                    if (mAllApps.get(j).versionName.equals(versionName)) {
                        mAllApps.get(j).found = true;
                        break;
                    } else {
                        mAllApps.remove(j);
                        mAllApps.add(j, prepareInfo(info, true));
                        mAllApps.get(j).found = true;
                        break;
                    }
                }
            }
            if (!found) {
                UidDetail detail = prepareInfo(info, true);
                detail.found = true;
                mAllApps.add(detail);
                if (detail.isSystem) {
                    mSystemApps.add(detail);
                } else {
                    mUserApps.add(detail);
                }
            }
        }
        int i = 0;
        while (i < mAllApps.size()) {
            UidDetail detail = mAllApps.get(i);
            if (!detail.found) {
                mAllApps.remove(i);
                if (detail.isSystem) {
                    mSystemApps.remove(detail);
                } else {
                    mUserApps.remove(detail);
                }
                getContentResolver().delete(UidDetailDbProvider.CONTENT_URI_APP_DETAIL, UidDetailDbProvider.HASH_CODE + "=" + detail.hashCode, null);
            } else {
                i += 1;
            }
        }
    }

    private void queryMain(boolean block) {
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = mPm.queryIntentActivities(mainIntent, 0);
        for (int i = 0; i < apps.size(); i++) {
            ResolveInfo info = apps.get(i);
            UidDetail detail = prepareInfo(info, block);
            mAllApps.add(detail);
            if (detail.isSystem) {
                mSystemApps.add(detail);
            } else {
                mUserApps.add(detail);
            }
        }
        if (!block) {
            AsyncTask<Void, Void, Void> queryIconTask = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... arg0) {
                    for (int i = 0; i < mAllApps.size(); i++) {
                        UidDetail detail = mAllApps.get(i);
                        Util.extractDetail(detail, mPm);
                        Util.update(detail, getContentResolver());
                        // release reference for info, so that it can release memory by gc
                        detail.info = null;
                    }
                    mHandler.sendEmptyMessage(QUERY_ICON_OK);
                    return null;
                }
            };
            TaskHelper.execute(queryIconTask);
        }
    }

    private static final int GET_ALL_OK = 0;
    private static final int SYNC_DB_OK = 1;
    private static final int QUERY_ICON_OK = 2;
    private class AppHandler extends Handler {
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case GET_ALL_OK:
                    mAppListView.setAdapter(mAppListAdapter);
                    mAppListView.setOnScrollListener(mAppListAdapter);
                    break;
                case SYNC_DB_OK:
                    prepareAll();
                    break;
                case QUERY_ICON_OK:
                    mAppListAdapter.notifyDataSetChanged();
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
            UidDetail detail = mAllApps.get(i);
            if (detail.packageName.equals(packageName)) {
                mAllApps.remove(i);
                mSystemApps.remove(detail);
                mUserApps.remove(detail);
                getContentResolver().delete(UidDetailDbProvider.CONTENT_URI_APP_DETAIL, UidDetailDbProvider.HASH_CODE + "=" + detail.hashCode, null);
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
                        UidDetail detail = prepareInfo(info, true);
                        mAllApps.add(detail);
                        if (detail.isSystem) {
                            mSystemApps.add(detail);
                        } else {
                            mUserApps.add(detail);
                        }
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
        mShadowView.setVisibility(View.GONE);
        if (TextUtils.isEmpty(s)) {
            mAppContainer.setVisibility(View.VISIBLE);
            mSearchContainer.setVisibility(View.GONE);
        } else {
            mAppContainer.setVisibility(View.GONE);
            mSearchContainer.setVisibility(View.VISIBLE);
            mAppSelectListAdapter = new AppSelectListAdapter(Home.this, mUidDetailProvider, mCurrentApps, mSections, mPositions);
            mSearchListView.setTextFilterEnabled(true);
            mSearchListView.setAdapter(mAppSelectListAdapter);
            mAppSelectListAdapter.getFilter().filter(s);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    private void startSearchBarAnimation() {
        if (android.os.Build.VERSION.SDK_INT < 11 || mShadowView.getVisibility() == View.VISIBLE) {
            return;
        }
        mShadowView.setAlpha(0);
        mShadowView.setVisibility(View.VISIBLE);
        if (mStartSearchAnimatorSet != null && !mStartSearchAnimatorSet.isRunning()) {
            mStartSearchAnimatorSet.start();
            return;
        }
        PropertyValuesHolder holderAlpha = PropertyValuesHolder.ofFloat("alpha", 0, 1);
        Animator animatorAlpha = ObjectAnimator.ofPropertyValuesHolder(mShadowView, holderAlpha);
        mStartSearchAnimatorSet = new AnimatorSet();
        mStartSearchAnimatorSet.setStartDelay(10);
        mStartSearchAnimatorSet.setDuration(200);
        mStartSearchAnimatorSet.playTogether(animatorAlpha);
        mStartSearchAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator arg0) {
            }
        });
        mStartSearchAnimatorSet.start();
    }

    private void stopSearchBarAnimation() {
        if (android.os.Build.VERSION.SDK_INT < 11 || mShadowView.getVisibility() == View.GONE) {
            return;
        }
        if (mStopSearchAnimatorSet != null && !mStopSearchAnimatorSet.isRunning()) {
            mStopSearchAnimatorSet.start();
            return;
        }
        PropertyValuesHolder holderAlpha = PropertyValuesHolder.ofFloat("alpha", 1, 0);
        Animator animatorAlpha = ObjectAnimator.ofPropertyValuesHolder(mShadowView, holderAlpha);
        mStopSearchAnimatorSet = new AnimatorSet();
        mStopSearchAnimatorSet.setDuration(200);
        mStopSearchAnimatorSet.playTogether(animatorAlpha);
        mStopSearchAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator arg0) {
                searchAnimationEnd();
            }

            @Override
            public void onAnimationCancel(Animator arg0) {
                searchAnimationEnd();
            }
        });
        mStopSearchAnimatorSet.start();
    }

    private void searchAnimationEnd() {
        mShadowView.setVisibility(View.GONE);
        mSearchEditText.clearFocus();
        mInputManager.hideSoftInputFromWindow(mSearchEditText.getWindowToken(), 0);
    }

    MenuItem mAllMenu, mSystemMenu, mUserMenu;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        setIconEnable(menu, true);

        mAllMenu = menu.add(0, APP_ALL, 0, "all");
        mAllMenu.setIcon(R.drawable.ic_yes);
        mSystemMenu = menu.add(0, APP_SYSTEM, 0, "system");
        mUserMenu = menu.add(0, APP_USER, 0, "user");
        //menu.add(0, APP_GRIDVIEW, 0, "GridView");
        return super.onCreateOptionsMenu(menu);
    }

    private void setIconEnable(Menu menu, boolean enable)
    {
        try {
            Class<?> clazz = Class.forName("com.android.internal.view.menu.MenuBuilder");
            Method m = clazz.getDeclaredMethod("setOptionalIconsVisible", boolean.class);
            m.setAccessible(true);
            m.invoke(menu, enable);
        } catch (Exception e) {}
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.equals(mAllMenu) || item.equals(mSystemMenu) || item.equals(mUserMenu)) {
            if (item.equals(mAllMenu)) {
                mAllMenu.setIcon(R.drawable.ic_yes);
                mSystemMenu.setIcon(null);
                mUserMenu.setIcon(null);
            } else if (item.equals(mSystemMenu)) {
                mAllMenu.setIcon(null);
                mSystemMenu.setIcon(R.drawable.ic_yes);
                mUserMenu.setIcon(null);
            } else if (item.equals(mUserMenu)) {
                mAllMenu.setIcon(null);
                mSystemMenu.setIcon(null);
                mUserMenu.setIcon(R.drawable.ic_yes);
            }
            if (mAppGroup != item.getItemId()) {
                mAppGroup = item.getItemId();
                prepareAll();
            }
        }
        return super.onOptionsItemSelected(item);
    }
}

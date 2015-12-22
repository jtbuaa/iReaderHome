package com.android.settings.net;

import base.util.TaskHelper;
import ireader.home.R;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Background task that loads {@link UidDetail}, binding to {@link DataUsageAdapter} row item
 * when finished.
 */
public class UidDetailTask extends AsyncTask<Void, Void, UidDetail> {
    private final UidDetailProvider mProvider;
    private final View mTarget;
    private final UidDetail mInfo;

    private UidDetailTask(UidDetailProvider provider, UidDetail info,
            View target) {
        mProvider = provider;
        mInfo = info;
        mTarget = target;
    }

    public static void bindView(UidDetailProvider provider, UidDetail detail,
            View target) {
        final UidDetailTask existing = (UidDetailTask) target.getTag();
        if (existing != null) {
            existing.cancel(false);
        }

        if (detail.icon != null) {
            bindView(detail, target);
        } else {
            UidDetailTask detailTask = new UidDetailTask(provider, detail, target);
            TaskHelper.execute((AsyncTask)detailTask);
            target.setTag(detailTask);
        }
    }

    private static void bindView(UidDetail detail, View target) {
        final ImageView icon = (ImageView) target.findViewById(R.id.app_icon);
        final TextView versionName = (TextView) target.findViewById(R.id.version_name);
        if (detail != null) {
            icon.setImageDrawable(detail.icon);
            versionName.setText(detail.versionName);
            versionName.setTag(detail);
        } else {
            icon.setImageDrawable(null);
            versionName.setText("");
            versionName.setTag(null);
        }
    }

    @Override
    protected void onPreExecute() {
        bindView(null, mTarget);
    }

    @Override
    protected UidDetail doInBackground(Void... params) {
        return mProvider.getUidDetail(mInfo, true);
    }

    @Override
    protected void onPostExecute(UidDetail result) {
        bindView(result, mTarget);
    }
}

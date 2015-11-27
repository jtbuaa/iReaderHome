package ireader.home;

import android.content.pm.ResolveInfo;
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
    private final ResolveInfo mInfo;

    private UidDetailTask(UidDetailProvider provider, ResolveInfo info,
            View target) {
        mProvider = provider;
        mInfo = info;
        mTarget = target;
    }

    public static void bindView(UidDetailProvider provider, ResolveInfo info,
            View target) {
        final UidDetailTask existing = (UidDetailTask) target.getTag();
        if (existing != null) {
            existing.cancel(false);
        }

        final UidDetail cachedDetail = provider.getUidDetail(info,
                false);
        if (cachedDetail != null) {
            bindView(cachedDetail, target);
        } else {
            target.setTag(new UidDetailTask(provider, info, target)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR));
        }
    }

    private static void bindView(UidDetail detail, View target) {
        final ImageView icon = (ImageView) target
                .findViewById(R.id.app_icon);
        final TextView title = (TextView) target
                .findViewById(R.id.app_name);
        final TextView versionName = (TextView) target
                .findViewById(R.id.version_name);
        final TextView packageName = (TextView) target
                .findViewById(R.id.package_name);
        if (detail != null) {
            icon.setImageDrawable(detail.icon);
            title.setText(detail.label);
            packageName.setText(detail.packageName);
            versionName.setText(detail.versionName);
            versionName.setTag(detail);
        } else {
            icon.setImageDrawable(null);
            title.setText(null);
            packageName.setText(null);
            versionName.setText(null);
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

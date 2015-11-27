package ireader.home;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;

public class AppListAdapter extends ArrayAdapter<ResolveInfo> {
    ArrayList localApplist;
    Context mContext;
    PackageManager mPm;
    private final UidDetailProvider mProvider;

    public AppListAdapter(Context context, UidDetailProvider provider, List<ResolveInfo> apps) {
        super(context, 0, apps);
        localApplist = (ArrayList) apps;
        mContext = context;
        mPm = mContext.getPackageManager();
        mProvider = provider;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = ((Activity) mContext).getLayoutInflater().inflate(R.layout.app_list, parent, false);
            convertView.setOnClickListener(clickListener);
            convertView.findViewById(R.id.version_name).setOnClickListener(uninstallClickListener);
        }
        UidDetailTask.bindView(mProvider, (ResolveInfo) localApplist.get(position), convertView);

        return convertView;
    }

    private OnClickListener clickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            UidDetail detail = (UidDetail)view.findViewById(R.id.version_name).getTag();
            if (detail == null || ((Activity) mContext).getComponentName().getPackageName().equals(detail.packageName)) {
                return;
            }
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setComponent(new ComponentName(detail.packageName,
                    detail.className));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            EventBus.getDefault().post(intent);
            try {
                mContext.startActivity(intent);
                mContext.startService(new Intent(mContext, Dragger.class));
            } catch(ActivityNotFoundException e) {}
        }
    };

    private OnClickListener uninstallClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            UidDetail detail = (UidDetail)view.getTag();
            if (detail == null) {
                return;
            }
            Uri uri = Uri.fromParts("package", detail.packageName , null);
            Intent intent = new Intent(Intent.ACTION_DELETE, uri);
            try {
                mContext.startActivity(intent);
            } catch(ActivityNotFoundException e) {}
        }
    };
}
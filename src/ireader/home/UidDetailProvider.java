/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ireader.home;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.SparseArray;

public class UidDetailProvider {
    private final Context mContext;
    private final SparseArray<UidDetail> mUidDetailCache;

    public UidDetailProvider(Context context) {
        mContext = context.getApplicationContext();
        mUidDetailCache = new SparseArray<UidDetail>();
    }

    public void clearCache() {
        synchronized (mUidDetailCache) {
            mUidDetailCache.clear();
        }
    }

    public UidDetail getUidDetail(ResolveInfo info, boolean blocking) {
        UidDetail detail;

        synchronized (mUidDetailCache) {
            detail = mUidDetailCache.get(info.hashCode());
        }

        if (detail != null) {
            return detail;
        } else if (!blocking) {
            return null;
        }

        detail = buildUidDetail(info);

        synchronized (mUidDetailCache) {
            mUidDetailCache.put(info.hashCode(), detail);
        }

        return detail;
    }

    /**
     * Build {@link UidDetail} object, blocking until all {@link Drawable}
     * lookup is finished.
     */
    private UidDetail buildUidDetail(ResolveInfo info) {
        final PackageManager pm = mContext.getPackageManager();

        final UidDetail detail = new UidDetail();
        // we have store label in dataDir when prepareInfo. if not, should use info.loadLabel(pm);
        detail.label = info.activityInfo.applicationInfo.dataDir;
        detail.icon = info.loadIcon(pm);
        detail.packageName = info.activityInfo.packageName;
        detail.className = info.activityInfo.name;
        detail.hashCode = info.hashCode();
        try {
            String version = pm.getPackageInfo(info.activityInfo.packageName, 0).versionName;
            if ((version == null) || (version.trim().equals("")))
                version = String.valueOf(pm.getPackageInfo(info.activityInfo.packageName, 0).versionCode);
            detail.versionName = version;
        } catch (NameNotFoundException e) {
            detail.versionName= e.toString();
        }

        if (TextUtils.isEmpty(detail.label)) {
            detail.label = info.activityInfo.name;
        }

        return detail;
    }
}

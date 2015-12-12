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

package com.android.settings.net;

import base.util.Util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

public class UidDetailProvider {
    private final Context mContext;
    //private final SparseArray<UidDetail> mUidDetailCache;
    PackageManager mPm;

    public UidDetailProvider(Context context) {
        mContext = context.getApplicationContext();
        //mUidDetailCache = new SparseArray<UidDetail>();
        mPm = mContext.getPackageManager();
    }

    public void clearCache() {
        //synchronized (mUidDetailCache) {
        //    mUidDetailCache.clear();
        //}
    }

    public UidDetail getUidDetail(UidDetail info, boolean blocking) {
        UidDetail detail;

        //synchronized (mUidDetailCache) {
        //    detail = mUidDetailCache.get(info.hashCode);
        //}

        if (info != null && info.icon != null) {
            return info;
        } else if (!blocking) {
            return null;
        }

        detail = buildUidDetail(info);

        //synchronized (mUidDetailCache) {
        //    mUidDetailCache.put(info.hashCode, detail);
        //}

        return detail;
    }

    /**
     * Build {@link UidDetail} object, blocking until all {@link Drawable}
     * lookup is finished.
     */
    private UidDetail buildUidDetail(UidDetail detail) {
        ContentResolver contentResolver = mContext.getContentResolver();
        if (detail.info == null) {
            Util.queryIcon(detail, contentResolver);
        } else {
            Util.extractDetail(detail, mPm);
            Util.update(detail, contentResolver);
            // release reference for info, so that it can release memory by gc
            detail.info = null;
        }

        return detail;
    }

}

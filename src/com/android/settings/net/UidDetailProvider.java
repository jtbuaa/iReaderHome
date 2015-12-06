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

import java.nio.ByteBuffer;

import ireader.provider.InfoProvider;
import base.util.Util;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
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
        detail.icon = info.loadIcon(pm);
        detail.title = Util.getLabel(info);
        detail.packageName = info.activityInfo.packageName;
        detail.className = info.activityInfo.name;
        detail.versionName = Util.getVersion(info);
        detail.sourceDir = info.activityInfo.applicationInfo.sourceDir;
        detail.isSystem = (info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM;
        detail.hashCode = info.hashCode();

        ContentValues updateValue = new ContentValues();
        updateValue.put(InfoProvider.ICON, getBlob(detail.icon));
        updateValue.put(InfoProvider.TITLE, detail.title);
        updateValue.put(InfoProvider.PACKAGE_NAME, detail.packageName);
        updateValue.put(InfoProvider.CLASS_NAME, detail.className);
        updateValue.put(InfoProvider.VERSION_NAME, detail.versionName);
        updateValue.put(InfoProvider.SOURCE_DIR, detail.sourceDir);
        updateValue.put(InfoProvider.IS_SYSTEM, detail.isSystem);
        updateValue.put(InfoProvider.HASH_CODE, detail.hashCode);
        mContext.getContentResolver().update(InfoProvider.CONTENT_URI_APP_DETAIL, updateValue, null, null);

        return detail;
    }

    private byte[] getBlob(Drawable icon) {
        Bitmap bitmap = Bitmap.createBitmap(
        icon.getIntrinsicWidth(),
        icon.getIntrinsicHeight(),
        icon.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
        icon.draw(canvas);

        ByteBuffer buffer = null;
        buffer = ByteBuffer.allocate(bitmap.getByteCount());
        bitmap.copyPixelsToBuffer(buffer);
        byte[] bArray = buffer.array();
        buffer.rewind();
        return bArray;
    }

}

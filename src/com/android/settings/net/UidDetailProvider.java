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
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
        String selection = String.format("%s = %d", InfoProvider.HASH_CODE, info.hashCode());
        Cursor cursor = mContext.getContentResolver().query(InfoProvider.CONTENT_URI_APP_DETAIL, null, selection, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            byte[] blob = cursor.getBlob(cursor.getColumnIndex(InfoProvider.ICON));
            int width = cursor.getInt(cursor.getColumnIndex(InfoProvider.ICON_WIDTH));
            int height = cursor.getInt(cursor.getColumnIndex(InfoProvider.ICON_HEIGHT));
            detail.icon = getDrawableFromBlob(blob, width, height);
            detail.title = cursor.getString(cursor.getColumnIndex(InfoProvider.TITLE));
            detail.packageName = cursor.getString(cursor.getColumnIndex(InfoProvider.PACKAGE_NAME));
            detail.className = cursor.getString(cursor.getColumnIndex(InfoProvider.CLASS_NAME));
            detail.versionName = cursor.getString(cursor.getColumnIndex(InfoProvider.VERSION_NAME));
            detail.sourceDir = cursor.getString(cursor.getColumnIndex(InfoProvider.SOURCE_DIR));
            detail.isSystem = cursor.getInt(cursor.getColumnIndex(InfoProvider.IS_SYSTEM)) == 1 ? true : false;
            detail.hashCode = cursor.getInt(cursor.getColumnIndex(InfoProvider.HASH_CODE));
        } else {
            detail.icon = info.loadIcon(pm);
            detail.title = Util.getLabel(info);
            detail.packageName = info.activityInfo.packageName;
            detail.className = info.activityInfo.name;
            detail.versionName = Util.getVersion(info);
            detail.sourceDir = info.activityInfo.applicationInfo.sourceDir;
            detail.isSystem = (info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM;
            detail.hashCode = info.hashCode();

            ContentValues updateValue = new ContentValues();
            updateValue.put(InfoProvider.ICON_WIDTH, detail.icon.getIntrinsicWidth());
            updateValue.put(InfoProvider.ICON_HEIGHT, detail.icon.getIntrinsicHeight());
            updateValue.put(InfoProvider.HASH_CODE, detail.hashCode);
            updateValue.put(InfoProvider.ICON, getBlobFromIcon(detail.icon));
            updateValue.put(InfoProvider.TITLE, detail.title);
            updateValue.put(InfoProvider.PACKAGE_NAME, detail.packageName);
            updateValue.put(InfoProvider.CLASS_NAME, detail.className);
            updateValue.put(InfoProvider.VERSION_NAME, detail.versionName);
            updateValue.put(InfoProvider.SOURCE_DIR, detail.sourceDir);
            updateValue.put(InfoProvider.IS_SYSTEM, detail.isSystem);
            updateValue.put(InfoProvider.HASH_CODE, detail.hashCode);
            mContext.getContentResolver().update(InfoProvider.CONTENT_URI_APP_DETAIL, updateValue, null, null);
        }
        if (cursor != null) {
            cursor.close();
        }

        return detail;
    }

    private byte[] getBlobFromIcon(Drawable icon) {
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

    private Drawable getDrawableFromBlob(byte[] blob, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888);
        ByteBuffer buffer = ByteBuffer.wrap(blob);
        bitmap.copyPixelsFromBuffer(buffer);
        return new BitmapDrawable(bitmap);
    }
}

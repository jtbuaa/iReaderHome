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

import ireader.provider.UidDetailDbProvider;
import base.util.Util;
import android.content.ContentResolver;
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
            detail = mUidDetailCache.get(info.activityInfo.packageName.hashCode());
        }

        if (detail != null) {
            return detail;
        } else if (!blocking) {
            return null;
        }

        detail = buildUidDetail(info);

        synchronized (mUidDetailCache) {
            mUidDetailCache.put(info.activityInfo.packageName.hashCode(), detail);
        }

        return detail;
    }

    /**
     * Build {@link UidDetail} object, blocking until all {@link Drawable}
     * lookup is finished.
     */
    private UidDetail buildUidDetail(ResolveInfo info) {
        final UidDetail detail = new UidDetail();
        String hashCode = String.format("%d", info.activityInfo.packageName.hashCode());
        ContentResolver contentResolver = mContext.getContentResolver();
        Cursor cursor = contentResolver.query(UidDetailDbProvider.CONTENT_URI_APP_DETAIL, null, UidDetailDbProvider.HASH_CODE + "=?", new String[] {hashCode}, UidDetailDbProvider.PINYIN);
        if (cursor != null && cursor.moveToFirst()) {
            try {
                byte[] blob = cursor.getBlob(cursor.getColumnIndex(UidDetailDbProvider.ICON));
                int width = cursor.getInt(cursor.getColumnIndex(UidDetailDbProvider.ICON_WIDTH));
                int height = cursor.getInt(cursor.getColumnIndex(UidDetailDbProvider.ICON_HEIGHT));
                detail.icon = getDrawableFromBlob(blob, width, height);
                detail.title = cursor.getString(cursor.getColumnIndex(UidDetailDbProvider.TITLE));
                detail.pinyin = cursor.getString(cursor.getColumnIndex(UidDetailDbProvider.PINYIN));
                detail.packageName = cursor.getString(cursor.getColumnIndex(UidDetailDbProvider.PACKAGE_NAME));
                detail.className = cursor.getString(cursor.getColumnIndex(UidDetailDbProvider.CLASS_NAME));
                detail.versionName = cursor.getString(cursor.getColumnIndex(UidDetailDbProvider.VERSION_NAME));
                detail.sourceDir = cursor.getString(cursor.getColumnIndex(UidDetailDbProvider.SOURCE_DIR));
                detail.isSystem = cursor.getInt(cursor.getColumnIndex(UidDetailDbProvider.IS_SYSTEM)) == 1 ? true : false;
                detail.hashCode = cursor.getInt(cursor.getColumnIndex(UidDetailDbProvider.HASH_CODE));
            } catch(IllegalStateException e) {
                // exception when get iread.home only on T1?
                setDetail(detail, info);
            }
        } else {
            setDetail(detail, info);

            ContentValues updateValue = new ContentValues();
            updateValue.put(UidDetailDbProvider.ICON_WIDTH, detail.icon.getIntrinsicWidth());
            updateValue.put(UidDetailDbProvider.ICON_HEIGHT, detail.icon.getIntrinsicHeight());
            updateValue.put(UidDetailDbProvider.ICON, getBlobFromIcon(detail.icon));
            updateValue.put(UidDetailDbProvider.TITLE, detail.title);
            updateValue.put(UidDetailDbProvider.PINYIN, detail.pinyin);
            updateValue.put(UidDetailDbProvider.PACKAGE_NAME, detail.packageName);
            updateValue.put(UidDetailDbProvider.CLASS_NAME, detail.className);
            updateValue.put(UidDetailDbProvider.VERSION_NAME, detail.versionName);
            updateValue.put(UidDetailDbProvider.SOURCE_DIR, detail.sourceDir);
            updateValue.put(UidDetailDbProvider.IS_SYSTEM, detail.isSystem);
            updateValue.put(UidDetailDbProvider.HASH_CODE, detail.hashCode);
            contentResolver.update(UidDetailDbProvider.CONTENT_URI_APP_DETAIL, updateValue, null, null);
        }
        if (cursor != null) {
            cursor.close();
        }

        return detail;
    }

    private void setDetail(UidDetail detail, ResolveInfo info) {
        final PackageManager pm = mContext.getPackageManager();
        detail.icon = info.loadIcon(pm);
        detail.title = Util.getLabel(info);
        detail.pinyin = Util.getPinyin(info);
        detail.packageName = info.activityInfo.packageName;
        detail.className = info.activityInfo.name;
        detail.versionName = Util.getVersion(info);
        detail.sourceDir = info.activityInfo.applicationInfo.sourceDir;
        detail.isSystem = (info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM;
        detail.hashCode = info.activityInfo.packageName.hashCode();
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

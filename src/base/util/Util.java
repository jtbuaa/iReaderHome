package base.util;

import java.nio.ByteBuffer;

import ireader.provider.UidDetailDbProvider;

import com.android.settings.net.UidDetail;
import com.github.promeg.pinyinhelper.Pinyin;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public abstract class Util {
    public static void extractDetail(UidDetail detail, PackageManager pm) {
        if (detail.info == null) {
            return;
        }
        detail.icon = detail.info.loadIcon(pm);
        StringBuilder pinyin = new StringBuilder("");
        for (int i = 0; i < detail.label.length(); i++) {
            pinyin.append(Pinyin.toPinyin(detail.label.charAt(i)));
        }
        detail.pinyin = pinyin.toString();
        try {
            detail.versionName = pm.getPackageInfo(detail.packageName, 0).versionName;
            if ((detail.versionName == null) || (detail.versionName.trim().equals("")))
                detail.versionName = String.valueOf(pm.getPackageInfo(detail.packageName, 0).versionCode);
        } catch (NameNotFoundException e) {
            detail.versionName = e.toString();
        }
    }

    public static void query(UidDetail detail, Cursor cursor) {
        try {
            detail.label = cursor.getString(cursor.getColumnIndex(UidDetailDbProvider.TITLE));
            detail.pinyin = cursor.getString(cursor.getColumnIndex(UidDetailDbProvider.PINYIN));
            detail.packageName = cursor.getString(cursor.getColumnIndex(UidDetailDbProvider.PACKAGE_NAME));
            detail.className = cursor.getString(cursor.getColumnIndex(UidDetailDbProvider.CLASS_NAME));
            detail.versionName = cursor.getString(cursor.getColumnIndex(UidDetailDbProvider.VERSION_NAME));
            detail.sourceDir = cursor.getString(cursor.getColumnIndex(UidDetailDbProvider.SOURCE_DIR));
            detail.isSystem = cursor.getInt(cursor.getColumnIndex(UidDetailDbProvider.IS_SYSTEM)) == 1 ? true : false;
            detail.hashCode = cursor.getInt(cursor.getColumnIndex(UidDetailDbProvider.HASH_CODE));
        } catch(IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public static void queryIcon(UidDetail detail, ContentResolver contentResolver) {
        String hashCode = String.format("%d", detail.hashCode);
        Cursor cursor = contentResolver.query(UidDetailDbProvider.CONTENT_URI_APP_DETAIL, null, UidDetailDbProvider.HASH_CODE + "=?", new String[] {hashCode}, UidDetailDbProvider.PINYIN);
        if (cursor != null && cursor.moveToFirst()) {
            try {
                byte[] blob = cursor.getBlob(cursor.getColumnIndex(UidDetailDbProvider.ICON));
                int width = cursor.getInt(cursor.getColumnIndex(UidDetailDbProvider.ICON_WIDTH));
                int height = cursor.getInt(cursor.getColumnIndex(UidDetailDbProvider.ICON_HEIGHT));
                detail.icon = getDrawableFromBlob(blob, width, height);
            } catch(IllegalStateException e) {
                e.printStackTrace();
            }
            cursor.close();
        }
    }

    private static Drawable getDrawableFromBlob(byte[] blob, int width, int height) {
        if (blob == null) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888);
        ByteBuffer buffer = ByteBuffer.wrap(blob);
        bitmap.copyPixelsFromBuffer(buffer);
        return new BitmapDrawable(bitmap);
    }

    public static void update(UidDetail detail, ContentResolver contentResolver) {
        // save to db
        // comment for when query, nativeExecuteForCursorWindow so slow on T1. so not insert to db, then not query from db
        /*ContentValues updateValue = new ContentValues();
        updateValue.put(UidDetailDbProvider.ICON_WIDTH, detail.icon.getIntrinsicWidth());
        updateValue.put(UidDetailDbProvider.ICON_HEIGHT, detail.icon.getIntrinsicHeight());
        updateValue.put(UidDetailDbProvider.ICON, getBlobFromIcon(detail.icon));
        updateValue.put(UidDetailDbProvider.TITLE, detail.label);
        updateValue.put(UidDetailDbProvider.PINYIN, detail.pinyin);
        updateValue.put(UidDetailDbProvider.PACKAGE_NAME, detail.packageName);
        updateValue.put(UidDetailDbProvider.CLASS_NAME, detail.className);
        updateValue.put(UidDetailDbProvider.VERSION_NAME, detail.versionName);
        updateValue.put(UidDetailDbProvider.SOURCE_DIR, detail.sourceDir);
        updateValue.put(UidDetailDbProvider.IS_SYSTEM, detail.isSystem);
        updateValue.put(UidDetailDbProvider.HASH_CODE, detail.hashCode);
        contentResolver.update(UidDetailDbProvider.CONTENT_URI_APP_DETAIL, updateValue, null, null);*/
    }

    private static byte[] getBlobFromIcon(Drawable icon) {
        if (android.os.Build.VERSION.SDK_INT < 11) {
            return null;
        }

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

package ireader.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.drawable.Drawable;
import android.net.Uri;

public class InfoProvider extends ContentProvider {

    public static final String TAG = "InfoProvider";
    private final static String AUTHORITY = "ireader.home.infoProvider";
    private final static String TABLE_INFO_DETAIL = "detail";

    public static final Uri CONTENT_URI_APP_DETAIL = Uri.parse("content://" + AUTHORITY + "/"
            + TABLE_INFO_DETAIL);

    private static final int INFO_DETAIL = 0;

    public static final String ICON = "icon";
    public static final String TITLE = "title";
    public static final String PACKAGE_NAME = "package_name";
    public static final String CLASS_NAME = "class_name";
    public static final String VERSION_NAME = "version_name";
    public static final String SOURCE_DIR = "source_dir";
    public static final String IS_SYSTEM = "is_system";
    public static final String HASH_CODE = "hash_code";

    private static final UriMatcher sURLMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURLMatcher.addURI(AUTHORITY, TABLE_INFO_DETAIL, INFO_DETAIL);
    }

    private String[] mVirtualColumns = new String[]{
            ICON,
            TITLE,
            PACKAGE_NAME,
            CLASS_NAME,
            VERSION_NAME,
            SOURCE_DIR,
            HASH_CODE
    };

    private Context mContext;
    protected SQLiteDatabase mDb;
    DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mContext = getContext();
        mOpenHelper = getDatabaseHelper(mContext);
        mDb = mOpenHelper.getWritableDatabase();
        return true;
    }

    public InfoProvider() {
        super();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        switch (sURLMatcher.match(uri)) {
            case INFO_DETAIL:
                updateValue(values);
                break;
        }
        mContext.getContentResolver().notifyChange(uri, null);
        return uri;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        switch (sURLMatcher.match(uri)) {
            case INFO_DETAIL:
                updateValue(values);
                break;
        }
        return 0;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        switch (sURLMatcher.match(uri)) {
            case INFO_DETAIL:
                for (ContentValues contentValues : values) {
                    updateValue(contentValues);
                }
                break;
        }
        mContext.getContentResolver().notifyChange(uri, null);
        return values.length;
    }

    private void updateValue(ContentValues values) {
        int hashCode = (Integer) values.get(HASH_CODE);
        String sql = String.format("delete from %s where %s = '%s';", TABLE_INFO_DETAIL, HASH_CODE, hashCode);
        mDb.execSQL(sql);
        mDb.insert(TABLE_INFO_DETAIL, null, values);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        Cursor cursor = null;
        switch (sURLMatcher.match(uri)) {
            case INFO_DETAIL:
                cursor = getDetail(uri, selection, selectionArgs);
                break;
        }
        return cursor;
    }

    private Cursor getDetail(Uri uri, String selection, String[] selectionArgs) {
        if (selectionArgs != null) {
            MatrixCursor cursor = new MatrixCursor(mVirtualColumns);
            Object[] obj = new Object[]{
                    selectionArgs[0],
            };
            cursor.addRow(obj);
            return cursor;
        }
        return null;
    }

    private DatabaseHelper getDatabaseHelper(Context context) {
        synchronized (this) {
            if (mOpenHelper == null) {
                mOpenHelper = new DatabaseHelper(context);
            }
            return mOpenHelper;
        }
    }

    final class DatabaseHelper extends SQLiteOpenHelper {
        static final String DATABASE_NAME = "infos.db";
        static final int DATABASE_VERSION = 1;
        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_INFO_DETAIL + "(" +
                    TITLE + " TEXT NOT NULL," +
                    ICON + " BLOB," +
                    PACKAGE_NAME + " TEXT NOT NULL," +
                    CLASS_NAME + " TEXT NOT NULL," +
                    VERSION_NAME + " TEXT NOT NULL," +
                    SOURCE_DIR + " TEXT NOT NULL," +
                    IS_SYSTEM + " INTEGER NOT NULL DEFAULT 0," +
                    HASH_CODE + " INTEGER NOT NULL" +
                    ");");
        }
        @Override
        public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
        }
    }
}
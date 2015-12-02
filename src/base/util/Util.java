package base.util;

import android.content.pm.ResolveInfo;

public class Util {
    // borrow the dataDir to store label, for loadLabel() is very time consuming
    // use nativeLibraryDir to store first character

    public static void setLabel(ResolveInfo info, String label) {
        info.activityInfo.applicationInfo.dataDir = label;
    }

    public static String getLabel(ResolveInfo info) {
        return info.activityInfo.applicationInfo.dataDir;
    }

    public static void setPinyin(ResolveInfo info, String pinyin) {
        info.activityInfo.applicationInfo.nativeLibraryDir = pinyin;
    }

    public static String getPinyin(ResolveInfo info) {
        return info.activityInfo.applicationInfo.nativeLibraryDir;
    }

}

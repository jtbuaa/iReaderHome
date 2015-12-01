package ireader.adapter;

import android.content.pm.ResolveInfo;

public class ReaderResolveInfo extends ResolveInfo {

    public void setLabel(String label) {
        activityInfo.applicationInfo.dataDir = label;
    }

    public String getLabel() {
        return activityInfo.applicationInfo.dataDir;
    }

    public void setFirstCharacter(String firstCharacter) {
        activityInfo.applicationInfo.nativeLibraryDir = firstCharacter;
    }

    public String getFirstCharacter() {
        return activityInfo.applicationInfo.nativeLibraryDir;
    }
}

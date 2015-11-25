
package ireader.home;

import java.text.Collator;
import java.util.Comparator;

import android.content.pm.ResolveInfo;

public class StringComparator implements Comparator<ResolveInfo> {
    HanziToPinyin mTo;
    public StringComparator() {
        mTo = HanziToPinyin.getInstance();
    }

    public final int compare(ResolveInfo a, ResolveInfo b) {
        CharSequence labelA = a.activityInfo.applicationInfo.dataDir;
        CharSequence labelB = b.activityInfo.applicationInfo.dataDir;
        if (labelA.length() < 1)
            labelA = " ";
        if (labelB.length() < 1)
            labelB = " ";
        return sCollator.compare(mTo.getToken(labelA.charAt(0)).target, mTo.getToken(labelB.charAt(0)).target);
    }

    private final Collator sCollator = Collator.getInstance();
}

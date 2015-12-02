
package base.util;

import java.text.Collator;
import java.util.Comparator;

import android.content.pm.ResolveInfo;

public class StringComparator implements Comparator<ResolveInfo> {
    public StringComparator() {
    }

    public final int compare(ResolveInfo a, ResolveInfo b) {
        return sCollator.compare(Util.getPinyin(a), Util.getPinyin(b));
    }

    private final Collator sCollator = Collator.getInstance();
}

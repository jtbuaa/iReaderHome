
package base.util;

import ireader.adapter.ReaderResolveInfo;

import java.text.Collator;
import java.util.Comparator;

import android.content.pm.ResolveInfo;

public class StringComparator implements Comparator<ResolveInfo> {
    public StringComparator() {
    }

    public final int compare(ResolveInfo a, ResolveInfo b) {
        return sCollator.compare(((ReaderResolveInfo) a).getFirstCharacter(), ((ReaderResolveInfo) b).getFirstCharacter());
    }

    private final Collator sCollator = Collator.getInstance();
}

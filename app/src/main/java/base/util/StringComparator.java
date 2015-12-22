
package base.util;

import java.text.Collator;
import java.util.Comparator;

import com.android.settings.net.UidDetail;

public class StringComparator implements Comparator<UidDetail> {
    public StringComparator() {
    }

    public final int compare(UidDetail a, UidDetail b) {
        return sCollator.compare(a.pinyin, b.pinyin);
    }

    private final Collator sCollator = Collator.getInstance();
}

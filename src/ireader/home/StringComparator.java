
package ireader.home;

import java.text.Collator;
import java.util.Comparator;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

public class StringComparator implements Comparator<ResolveInfo> {
    private PackageManager mPm;
    public StringComparator(PackageManager pm) {
        mPm = pm;
    }

    public final int compare(ResolveInfo a, ResolveInfo b) {
        CharSequence labelA = a.loadLabel(mPm);
        CharSequence labelB = b.loadLabel(mPm);
        if (labelA.length() < 1)
            labelA = " ";
        if (labelB.length() < 1)
            labelB = " ";
        StringBuilder stringA = new StringBuilder();
        StringBuilder stringB = new StringBuilder();
        for (int i = 0; i < 1; i++) {
            stringA.append(HanziToPinyin.getInstance().getToken(labelA.charAt(i)).target);
        }
        for (int i = 0; i < 1; i++) {
            stringB.append(HanziToPinyin.getInstance().getToken(labelB.charAt(i)).target);
        }
        return sCollator.compare(stringA.toString(), stringB.toString());
    }

    private final Collator sCollator = Collator.getInstance();
}

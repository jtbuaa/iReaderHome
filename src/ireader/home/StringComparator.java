
package ireader.home;

import java.text.Collator;
import java.util.Comparator;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

public class StringComparator implements Comparator<ResolveInfo> {
    private PackageManager mPm;
    public StringComparator(PackageManager pm) {
        mPm = pm;
    }

    public final int compare(ResolveInfo a, ResolveInfo b) {
        CharSequence labelA = a.loadLabel(mPm);
        CharSequence labelB = b.loadLabel(mPm);
        StringBuilder stringA = new StringBuilder();
        StringBuilder stringB = new StringBuilder();
        for (int i = 0; i < labelA.length(); i++) {
            stringA.append(HanziToPinyin.getInstance().getToken(labelA.charAt(i)).target);
        }
        for (int i = 0; i < labelB.length(); i++) {
            stringB.append(HanziToPinyin.getInstance().getToken(labelB.charAt(i)).target);
        }
        return sCollator.compare(stringA.toString(), stringB.toString());
    }

    private final Collator sCollator = Collator.getInstance();
}

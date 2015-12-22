package base.util;

import android.os.AsyncTask;

public abstract class TaskHelper {

    public static void execute(AsyncTask<Void, Void, Void> task) {
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            // faster, but crash on android2.2, so need check sdk version before run
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            task.execute(null, null, null);
        }
    }
}

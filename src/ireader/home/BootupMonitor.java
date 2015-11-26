
package ireader.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * When booting is completed, it starts {@link ClipboardMonitor} service to monitor the states of
 * clipboard.
 */
public class BootupMonitor extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intents) {
        context.startService(new Intent(context, Dragger.class));
    }
}

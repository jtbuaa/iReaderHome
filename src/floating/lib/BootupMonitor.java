
package floating.lib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Show dragger when booting is completed, otherwise can't show on iReader device
 */
public class BootupMonitor extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intents) {
        context.startService(new Intent(context, Dragger.class));
    }
}

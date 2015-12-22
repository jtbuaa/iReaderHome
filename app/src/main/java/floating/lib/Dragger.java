
package floating.lib;

import ireader.home.Home;
import ireader.home.R;
import android.content.Intent;
import android.util.FloatMath;

public class Dragger extends FloatService {
    @Override
    public void onCreate() {
        super.onCreate();

        container.setOnTouchListener(dragListener);
        container.setBackgroundResource(R.drawable.ic_launcher);
    }

    @Override
    void onFingerDown() {
    }

    @Override
    void onFingerUp(float dx, float dy) {
        if (FloatMath.sqrt(dx * dx + dy * dy) < 10) {
            Intent intent = new Intent(this, Home.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }
}

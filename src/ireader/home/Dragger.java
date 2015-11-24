
package ireader.home;

import android.content.Context;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

public class Dragger extends FloatService {
    HorizontalScrollView adPanel;

    // http://stackoverflow.com/questions/7919865/detecting-a-long-press-with-android
    Runnable mFingerUp = new Runnable() {
        public void run() {
            //tv.setVisibility(View.VISIBLE);
        }
    };

    public void onFingerUp() {
        super.onFingerUp();

        handler.postDelayed(mFingerUp, 5000);
    }

    public void onFingerDown() {
        super.onFingerDown();

        //tv.setVisibility(View.GONE);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        container.setOnTouchListener(dragListener);

        Drawable bg = container.getBackground();
        //container.setBackgroundResource(R.drawable.bg);
        if (bg != null) {// tile the background
            if (bg instanceof BitmapDrawable) {
                BitmapDrawable bmp = (BitmapDrawable) bg;
                bmp.mutate(); // make sure that we aren't sharing state anymore
                bmp.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
            }
        }
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.dragger, container);
        //adPanel = (HorizontalScrollView) container.findViewById(R.id.adPanel);

        final Context context = this;
    }
}

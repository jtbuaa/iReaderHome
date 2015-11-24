
package ireader.home;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

public abstract class FloatService extends Service {

    WindowManager windowManager;
    public DisplayMetrics dm;
    public int MIN_WIDTH = 50;
    public boolean fixedWidth = false;
    public FrameLayout container;
    public Activity activity;
    WindowManager.LayoutParams params;
    SharedPreferences sp;
    Editor sEdit;
    public View.OnTouchListener dragListener;

    public Handler handler = new Handler();

    abstract void onFingerDown();
    abstract void onFingerUp(float dx, float dy);

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (dm != null)
            windowManager.getDefaultDisplay().getMetrics(dm);
        windowManager.updateViewLayout(container, params);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        dm = new DisplayMetrics();

        windowManager.getDefaultDisplay().getMetrics(dm);
        container = new FrameLayout(this);

        sp = PreferenceManager.getDefaultSharedPreferences(this);
        sEdit = sp.edit();
        int width;
        if (fixedWidth)
            width = MIN_WIDTH;
        else {
            width = sp.getInt("layout_width", MIN_WIDTH);
            if (width < MIN_WIDTH)
                width = MIN_WIDTH;
        }
        if (width * dm.density > dm.widthPixels)
            width = (int) (dm.widthPixels / dm.density);

        int height = sp.getInt("layout_height", MIN_WIDTH);
        params = new WindowManager.LayoutParams(
                ((int) (width * dm.density)), // not 320 here is to leave blank edge of both side
                ((int) (height * dm.density)), // if width and height are 0, hierarchyviewer will crash when get layout. but 0 is no use to our display
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = sp.getInt("layout_x", (int) ((dm.widthPixels - width * dm.density) / 2));
        params.y = sp.getInt("layout_y", (int) (dm.heightPixels - 50 * dm.density));

        windowManager.addView(container, params);

        dragListener = new View.OnTouchListener() {
            private WindowManager.LayoutParams paramsF = params;
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            int[] location = {
                    0, 0
            };

            static final int DRAG = 1;
            static final int ZOOM = 2;
            private int mode = DRAG;
            private float oldDist = 1f;

            /** Determine the space between the first two fingers */
            private float spacing(MotionEvent event) {
                try {
                    float x = event.getX(0) - event.getX(1);
                    float y = event.getY(0) - event.getY(1);
                    return FloatMath.sqrt(x * x + y * y);
                } catch (Exception e) {
                    return 0;
                } // sometime it get index out of bound
            }

            // @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        // handler.postDelayed(mLongPressed, 1000);// use for long click
                        onFingerDown();
                        initialX = paramsF.x;
                        initialY = paramsF.y;
                        view.getLocationOnScreen(location);
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        mode = DRAG;
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        oldDist = spacing(event);
                        if (oldDist > 10f)
                            mode = ZOOM;
                        break;
                    case MotionEvent.ACTION_UP:
                        view.getLocationOnScreen(location);
                        sEdit.putInt("layout_x", location[0]);// save position for accident exit
                        sEdit.putInt("layout_y", location[1]);// - statusBarHeight); // no statusbar if play video in fullscreen mode.
                        sEdit.putInt("layout_width", (int) (paramsF.width / dm.density));
                        sEdit.apply();
                        onFingerUp(event.getRawX() - initialTouchX, event.getRawY() - initialTouchY);
                    case MotionEvent.ACTION_POINTER_UP:
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int deltaX = (int) (event.getRawX() - initialTouchX);
                        int deltaY = (int) (event.getRawY() - initialTouchY);
                        if (mode == DRAG) {
                            paramsF.x = initialX + deltaX;
                            paramsF.y = initialY + deltaY;
                            try {
                                windowManager.updateViewLayout(container, paramsF);
                            } catch (Exception e) {
                                e.printStackTrace();
                            } // will fc when long click if not catch
                        } else if (mode == ZOOM) {
                            float newDist = spacing(event);
                            if (newDist > 10f) {
                                float scale, newWidth;
                                if (newDist > oldDist) {
                                    scale = 1.005f;
                                    newWidth = paramsF.width * scale;
                                    if (newWidth - paramsF.width < 1)
                                        newWidth = paramsF.width + 1;
                                } else {
                                    scale = 0.995f;
                                    newWidth = paramsF.width * scale;
                                    if (paramsF.width - newWidth < 1)
                                        newWidth = paramsF.width - 1;
                                }
                                paramsF.width = (int) newWidth;
                                if (paramsF.width < MIN_WIDTH * dm.density)
                                    paramsF.width = (int) (MIN_WIDTH * dm.density);
                                else if (paramsF.width > dm.widthPixels)
                                    paramsF.width = dm.widthPixels;
                                try {
                                    windowManager.updateViewLayout(container, paramsF);
                                } catch (Exception e) {
                                } // will fc when long click if not catch
                            }
                        }
                        break;
                }
             // return true if can't receive event. return false if want child view receive event
                return true;
            }
        };
    }

    @Override
    public void onDestroy() {
        if (container != null)
            windowManager.removeView(container);

        super.onDestroy();
    }
}

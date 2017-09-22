package ru.ilushling.screenfilter;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RemoteViews;

public class OverlayService extends IntentService {
    // Common
    String TAG = "Service";
    // UI
    LinearLayout linearDimmer, linearDimmerColor;
    WindowManager wm;
    Notification.Builder notification;
    // Variables
    int dimmerColorValue, dimmerValue;
    // STATIC
    public static final String OPEN_ACTION = "ru.ilushling.screenfilter.OPEN_ACTION", CLOSE_ACTION = "ru.ilushling.screenfilter.CLOSE_ACTION";

    public OverlayService() {
        super("myname");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        dimmerColorValue = intent.getIntExtra("dimmerColorValue", 0);
        dimmerValue = intent.getIntExtra("dimmerValue", 0);
    }


    @Override
    public void onCreate() {
        super.onCreate();

        startNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            dimmerColorValue = intent.getIntExtra("dimmerColorValue", 0);
            dimmerValue = intent.getIntExtra("dimmerValue", 0);

            layout();
        }
        return Service.START_REDELIVER_INTENT;
    }

    void layout() {
        try {
            if (dimmerColorValue == 0 && dimmerValue == 0) {
                notification = null;
            } else {
                // Proccesing Values of DIMMER
                // Color
                StringBuilder sb = new StringBuilder();
                sb.append(Integer.toHexString(dimmerColorValue));
                if (sb.length() < 2) {
                    sb.insert(0, '0'); // pad with leading zero if needed
                }
                String dimmerColorValue = sb.toString();
                // Dim
                sb = new StringBuilder();
                sb.append(Integer.toHexString(dimmerValue));
                if (sb.length() < 2) {
                    sb.insert(0, '0'); // pad with leading zero if needed
                }
                String dimmerValue = sb.toString();

                // Prepare UI
                // UI Params
                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN |
                                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
                        ,
                        PixelFormat.TRANSLUCENT);

                params.gravity = Gravity.TOP;
                params.x = 0;
                params.y = 0;

                // Add or Update UI
                if (wm != null) {
                    linearDimmerColor.setBackgroundColor(Color.parseColor("#" + dimmerColorValue + "FF6600"));
                    linearDimmer.setBackgroundColor(Color.parseColor("#" + dimmerValue + "000000"));

                    params.height = screenHeight();

                    wm.updateViewLayout(linearDimmerColor, params);
                    wm.updateViewLayout(linearDimmer, params);
                } else {
                    wm = (WindowManager) getSystemService(WINDOW_SERVICE);
                    linearDimmerColor = new LinearLayout(this);
                    linearDimmer = new LinearLayout(this);
                    linearDimmerColor.setBackgroundColor(Color.parseColor("#" + dimmerColorValue + "FF6600"));
                    linearDimmer.setBackgroundColor(Color.parseColor("#" + dimmerValue + "000000"));

                    params.height = screenHeight();

                    wm.addView(linearDimmerColor, params);
                    wm.addView(linearDimmer, params);
                }
            }
        } catch (Exception exce) {
            Log.e(TAG, "" + exce);
        }
    }

    // Get screen height with navigation bar height (Because MATCH_PARENT = SCREEN_SIZE - NAV_BAR)
    int screenHeight() {
        int screenHeight = 0;
        // Screen size
        Point size = new Point();
        wm.getDefaultDisplay().getSize(size);
        // Nav bar
        int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            // screen + nav bar
            screenHeight = size.y + getResources().getDimensionPixelSize(resourceId);
        }

        return screenHeight;
    }

    @Override
    public void onDestroy() {
        try {
            if (linearDimmerColor != null && linearDimmer != null && wm != null) {
                wm.removeView(linearDimmerColor);
                wm.removeView(linearDimmer);
            }
            super.onDestroy();
        } catch (Exception exce) {
            Log.e("OnDestroy: ", "" + exce);
        }
    }


    private void startNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            // Prepare
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            // Create remote view and set bigContentView.
            RemoteViews customView = new RemoteViews(this.getPackageName(), R.layout.notification);

            // Intents to BReceiver
            // Open intent
            Intent notificationIntentOpen = new Intent(this, BReceiver.class);
            notificationIntentOpen.setAction(OPEN_ACTION);
            PendingIntent pendingIntentOpen = PendingIntent.getBroadcast(this, 0, notificationIntentOpen, PendingIntent.FLAG_UPDATE_CURRENT);
            // Close intent
            Intent notificationIntentClose = new Intent(this, BReceiver.class);
            notificationIntentClose.setAction(CLOSE_ACTION);
            PendingIntent pendingIntentClose = PendingIntent.getBroadcast(this, 0, notificationIntentClose, PendingIntent.FLAG_UPDATE_CURRENT);

            // Build notification
            notification = new Notification.Builder(this)
                    .setContentTitle("Ночной фильтр")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(pendingIntentClose)
                    .setContent(customView)
                    //.addAction(R., "Настроить", pendingIntentOpen)
                    //.addAction(R.mipmap.ic_launcher, "Выход", pendingIntentClose)
                    .setAutoCancel(true);
            // Listeners
            customView.setOnClickPendingIntent(R.id.open_settings, pendingIntentOpen);
            //customView.setOnClickPendingIntent(R.id.close, pendingIntentClose);

            mNotificationManager.notify(9954, notification.build());
            startForeground(9954, notification.build());
        }
    }

}

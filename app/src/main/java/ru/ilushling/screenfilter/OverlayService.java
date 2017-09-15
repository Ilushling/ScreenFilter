package ru.ilushling.screenfilter;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.WindowManager;
import android.widget.LinearLayout;

public class OverlayService extends IntentService {
    // Common
    Context context;
    String TAG = "Service";
    // UI
    LinearLayout dimmer, dimmerColor;
    WindowManager wm;
    NotificationCompat.Builder notification;
    // Variables
    int dimmerColorValue, dimmerValue;
    boolean created = false;
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
        dimmerColorValue = intent.getIntExtra("dimmerColorValue", 10);
        dimmerValue = intent.getIntExtra("dimmerValue", 10);
    }


    @Override
    public void onCreate() {
        super.onCreate();

        startNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        dimmerColorValue = intent.getIntExtra("dimmerColorValue", 10);
        dimmerValue = intent.getIntExtra("dimmerValue", 10);

        layout();
        return Service.START_STICKY;
    }

    void layout() {
        try {
            if (dimmerColorValue == 0 && dimmerValue == 0) {
                created = false;
                notification = null;
            } else if (!created) {
                startNotification();
            }

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

            //Log.e("egor: ", "" + progress1);
            wm = (WindowManager) getSystemService(WINDOW_SERVICE);

            if (dimmer != null) {
                dimmerColor.setBackgroundColor(Color.parseColor("#" + dimmerColorValue + "FF6600"));
                dimmer.setBackgroundColor(Color.parseColor("#" + dimmerValue + "000000"));
                //wm.removeView(Dimmer);
            } else {
                dimmerColor = new LinearLayout(this);
                dimmer = new LinearLayout(this);
                dimmerColor.setBackgroundColor(Color.parseColor("#" + dimmerColorValue + "FF6600"));
                dimmer.setBackgroundColor(Color.parseColor("#" + dimmerValue + "000000"));

                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        ,
                        PixelFormat.TRANSLUCENT);

                wm.addView(dimmerColor, params);
                wm.addView(dimmer, params);
                created = true;
            }

        } catch (Exception exce) {
            Log.e(TAG, "" + exce);
        }
    }

    @Override
    public void onDestroy() {
        try {
            if (dimmer != null || wm != null) {
                wm.removeView(dimmerColor);
                wm.removeView(dimmer);
            }
            super.onDestroy();
        } catch (Exception exce) {
            Log.e("OnDestroy: ", "" + exce);
        }
    }


    private void startNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
// Намерение для запуска второй активности
            //Intent intent = new Intent(this, BReceiver.class);
            //PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            // Intents to BReceiver
            // Open intent
            Intent notificationIntentOpen = new Intent(this, BReceiver.class);
            notificationIntentOpen.setAction("ru.ilushling.screenfilter." + OPEN_ACTION);
            PendingIntent pendingIntentOpen = PendingIntent.getBroadcast(this, 0, notificationIntentOpen, PendingIntent.FLAG_UPDATE_CURRENT);
            // Close intent
            Intent notificationIntentClose = new Intent(this, BReceiver.class);
            notificationIntentClose.setAction("ru.ilushling.screenfilter." + CLOSE_ACTION);
            PendingIntent pendingIntentClose = PendingIntent.getBroadcast(this, 0, notificationIntentClose, PendingIntent.FLAG_UPDATE_CURRENT);

            // Строим уведомление
            notification = new NotificationCompat.Builder(this)
                    .setContentTitle("Ночной фильтр")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .addAction(R.mipmap.ic_launcher, "Настроить", pendingIntentOpen)
                    .addAction(R.mipmap.ic_launcher, "Выход", pendingIntentClose)
                    .setAutoCancel(true);


            //notificationView.setB

            //notificationManager.notify(9954, notification);
            startForeground(9954, notification.build());
        }
    }

}

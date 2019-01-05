package ru.ilushling.screenfilter;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import java.util.Calendar;

import static ru.ilushling.screenfilter.MainActivity.APP_PREFERENCES_DIMMER;
import static ru.ilushling.screenfilter.MainActivity.APP_PREFERENCES_DIMMER_COLOR;
import static ru.ilushling.screenfilter.MainActivity.APP_PREFERENCES_DIMMER_ON;
import static ru.ilushling.screenfilter.MainActivity.APP_PREFERENCES_NAME;
import static ru.ilushling.screenfilter.MainActivity.APP_PREFERENCES_TEMPERATURE;
import static ru.ilushling.screenfilter.MainActivity.APP_PREFERENCES_THEME;
import static ru.ilushling.screenfilter.MainActivity.APP_PREFERENCES_TIMER_HOUR_OFF;
import static ru.ilushling.screenfilter.MainActivity.APP_PREFERENCES_TIMER_HOUR_ON;
import static ru.ilushling.screenfilter.MainActivity.APP_PREFERENCES_TIMER_MINUTE_OFF;
import static ru.ilushling.screenfilter.MainActivity.APP_PREFERENCES_TIMER_MINUTE_ON;
import static ru.ilushling.screenfilter.MainActivity.APP_PREFERENCES_TIMER_ON;

public class OverlayService extends Service {
    // Common
    String TAG = "OverlayService";
    // UI
    LinearLayout linearDimmer, linearDimmerColor;
    WindowManager wm;
    Notification.Builder notification;
    // Variables
    String theme = "";
    int dimmerColorValue, dimmerValue, temperature;
    boolean timerOn;
    protected String timerHourOn, timerMinuteOn, timerHourOff, timerMinuteOff;
    // Save Settings
    SharedPreferences mSettings;
    // STATIC
    public static final String OPEN_ACTION = "ru.ilushling.screenfilter.OPEN_ACTION", CLOSE_ACTION = "ru.ilushling.screenfilter.CLOSE_ACTION",
            ALARM_TIMER_ON = "ru.ilushling.screenfilter.alarmTimerOn", ALARM_TIMER_OFF = "ru.ilushling.screenfilter.alarmTimerOff";
    public static final String NOTIFICATION_CHANNEL_ID = "9954", NOTIFICATION_CHANNEL_NAME = "screenfilter";
    private static final int ID_SERVICE = 9954;

    Intent intentTimerOn, intentTimerOff;
    PendingIntent pIntentTimerOn, pIntentTimerOff;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public void onCreate() {
        super.onCreate();

        mSettings = getSharedPreferences(APP_PREFERENCES_NAME, Context.MODE_PRIVATE);
        loadSettings();

        startNotification();

        intentTimerOn = createIntent(ALARM_TIMER_ON);
        pIntentTimerOn = PendingIntent.getBroadcast(this, 0, intentTimerOn, PendingIntent.FLAG_UPDATE_CURRENT);

        intentTimerOff = createIntent(ALARM_TIMER_OFF);
        pIntentTimerOff = PendingIntent.getBroadcast(this, 0, intentTimerOff, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    Intent createIntent(String action) {
        Intent intent = new Intent(this, BReceiver.class);
        intent.setAction(action);
        return intent;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();

        switch (action) {
            case "overlayOn":
                theme = intent.getStringExtra("theme");
                dimmerColorValue = intent.getIntExtra("dimmerColorValue", 0);
                dimmerValue = intent.getIntExtra("dimmerValue", 0);
                temperature = intent.getIntExtra("temperature", 4);

                overlayOn();
                break;
            case "overlayOff":
                overlayOff();
                break;
            case "timerOn":
                loadSettings();

                if (timerOn && timerHourOn != null && timerMinuteOn != null && timerHourOff != null && timerMinuteOff != null) {
                    timerOn(timerHourOn, timerMinuteOn, timerHourOff, timerMinuteOff);
                }
                break;
            case "timerOff":
                timerOff();
                break;
            case "alarmTimerOn":
                loadSettings();
                overlayOn();
                break;
            case "alarmTimerOff":
                overlayOff();
                break;
            case "theme":
                theme = intent.getStringExtra("theme");
                stopForeground(true);
                startNotification();
                break;
        }


        //Log.e(TAG, "action: " + action);
        return START_REDELIVER_INTENT;
    }

    void overlayOn() {
        try {
            if (dimmerColorValue == 0 && dimmerValue == 0) {
                notification = null;
            } else {
                // Prepare UI
                // UI Params
                WindowManager.LayoutParams params;
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    params = new WindowManager.LayoutParams(
                            WindowManager.LayoutParams.MATCH_PARENT,
                            WindowManager.LayoutParams.MATCH_PARENT,
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                            WindowManager.LayoutParams.FLAG_FULLSCREEN |
                                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
                            ,
                            PixelFormat.TRANSPARENT);
                } else {
                    params = new WindowManager.LayoutParams(
                            WindowManager.LayoutParams.MATCH_PARENT,
                            WindowManager.LayoutParams.MATCH_PARENT,
                            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                            WindowManager.LayoutParams.FLAG_FULLSCREEN |
                                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
                            ,
                            PixelFormat.TRANSPARENT);
                }

                params.gravity = Gravity.START | Gravity.TOP;
                params.x = 0;
                params.y = 0;

                // Add or Update UI
                if (wm != null && linearDimmerColor != null && linearDimmer != null) {
                    setLinears(params);

                    wm.updateViewLayout(linearDimmerColor, params);
                    wm.updateViewLayout(linearDimmer, params);
                } else {
                    // Add
                    //startNotification();
                    wm = (WindowManager) getSystemService(WINDOW_SERVICE);
                    linearDimmerColor = new LinearLayout(this);
                    linearDimmer = new LinearLayout(this);

                    setLinears(params);

                    wm.addView(linearDimmerColor, params);
                    wm.addView(linearDimmer, params);
                }

                saveSettings(APP_PREFERENCES_DIMMER_ON, true);
            }
        } catch (Exception exc) {
            Log.e(TAG, "Overlay On: " + exc);
            overlayOff();
        }
    }

    void setLinears(WindowManager.LayoutParams params) {
        switch (temperature) {
            case 1:
                linearDimmerColor.setBackgroundColor(Color.argb(dimmerColorValue, 255, 50, 20));
                linearDimmer.setBackgroundColor(Color.argb(dimmerValue, 0, 0, 0));
                break;
            case 2:
                linearDimmerColor.setBackgroundColor(Color.argb(dimmerColorValue, 255, 90, 40));
                linearDimmer.setBackgroundColor(Color.argb(dimmerValue, 0, 0, 0));
                break;
            case 3:
                linearDimmerColor.setBackgroundColor(Color.argb(dimmerColorValue, 255, 110, 50));
                linearDimmer.setBackgroundColor(Color.argb(dimmerValue, 0, 0, 0));
                break;
            case 4:
                linearDimmerColor.setBackgroundColor(Color.argb(dimmerColorValue, 255, 135, 60));
                linearDimmer.setBackgroundColor(Color.argb(dimmerValue, 0, 0, 0));
                break;
            case 5:
                linearDimmerColor.setBackgroundColor(Color.argb(dimmerColorValue, 255, 160, 75));
                linearDimmer.setBackgroundColor(Color.argb(dimmerValue, 0, 0, 0));
                break;
            default:
                linearDimmerColor.setBackgroundColor(Color.argb(dimmerColorValue, 255, 135, 60));
                linearDimmer.setBackgroundColor(Color.argb(dimmerValue, 0, 0, 0));
                break;
        }
        params.height = screenHeight();
    }

    void overlayOff() {
        if (linearDimmerColor != null && linearDimmer != null && wm != null) {
            try {
                saveSettings(APP_PREFERENCES_DIMMER_ON, false);

                wm.removeView(linearDimmerColor);
                wm.removeView(linearDimmer);


            } catch (Exception exc) {
                Log.e(TAG, "Overlay Off: " + exc);
            }

            wm = null;
            linearDimmerColor = null;
            linearDimmer = null;

            //stopForeground(true);
        }
        stopSelf();
        //Log.e(TAG, "overlay Off");
    }

    // Get screen height with navigation bar height (Because MATCH_PARENT = SCREEN_SIZE - NAV_BAR)
    int screenHeight() {
        // Screen size
        Point size = new Point();
        wm.getDefaultDisplay().getSize(size);
        int max = Math.max(
                size.x,
                size.y
        );
        // Nav bar
        int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            // screen + nav bar
            return max + getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "Service: onDestroy");
        overlayOff();
    }


    private void startNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            // Prepare
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            // Create remote view and set bigContentView.
            RemoteViews customView;
            switch (theme) {
                case "dark":
                    customView = new RemoteViews(this.getPackageName(), R.layout.notification_dark);
                    break;
                case "light":
                    customView = new RemoteViews(this.getPackageName(), R.layout.notification_light);
                    break;
                default:
                    customView = new RemoteViews(this.getPackageName(), R.layout.notification_dark);
                    break;
            }

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

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                if (mNotificationManager != null) {
                    int importance = NotificationManager.IMPORTANCE_HIGH;
                    NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, importance);
                    mNotificationManager.createNotificationChannel(notificationChannel);
                }
                notification = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                        .setContentTitle("Ночной фильтр")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentIntent(pendingIntentClose)
                        .setContent(customView)
                        .setAutoCancel(true);

            } else {
                notification = new Notification.Builder(this)
                        .setContentTitle("Ночной фильтр")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentIntent(pendingIntentClose)
                        .setContent(customView)
                        .setAutoCancel(true);
            }
            // Listeners
            customView.setOnClickPendingIntent(R.id.settings, pendingIntentOpen);

            mNotificationManager.notify(ID_SERVICE, notification.build());
            startForeground(ID_SERVICE, notification.build());
        }
    }

    void timerOn(String timerHourOn, String timerMinuteOn, String timerHourOff, String timerMinuteOff) {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        // Convert timer
        Calendar now = Calendar.getInstance(), timeOn = Calendar.getInstance(), timeOff = Calendar.getInstance();
        timeOn.setTimeInMillis(System.currentTimeMillis());
        timeOff.setTimeInMillis(System.currentTimeMillis());
        // Apply timer
        timeOn.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timerHourOn));
        timeOn.set(Calendar.MINUTE, Integer.parseInt(timerMinuteOn));
        timeOff.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timerHourOff));
        timeOff.set(Calendar.MINUTE, Integer.parseInt(timerMinuteOff));


        // if timeoff before timeon than timeoff set to next day
        if (timeOff.before(now)) {
            // Timer to next day
            if (timeOn.after(timeOff)) {
                timeOff.add(Calendar.DAY_OF_MONTH, 1);
            }
            if (((timeOn.before(now) || timeOn.equals(now)) && timeOn.before(timeOff)) && timeOff.after(now)) {
                // TimeOn after than TimeOff ex. (TimeOn = 22:00 and TimeOff = 06:00)
                turnOnDimmer();
            } else {
                turnOffDimmer();
            }
        } else {
            if (timeOn.before(now) && timeOff.after(now)) {
                // TimeOn before than TimeOff ex. (TimeOn = 01:00 and TimeOff = 06:00 now = 03:00)
                turnOnDimmer();
            } else {
                if (timeOn.before(timeOff)) {
                    // TimeOn after than now ex. (TimeOn = 02:00 and TimeOff = 06:00 now = 01:00)
                    turnOffDimmer();
                } else {
                    // TimeOn a day before than TimeOff ex. (TimeOn = 22:00 and TimeOff = 06:00 now = 03:00)
                    turnOnDimmer();
                }
            }
        }
        // Start timer
        am.setRepeating(AlarmManager.RTC, timeOn.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pIntentTimerOn);
        am.setRepeating(AlarmManager.RTC, timeOff.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pIntentTimerOff);

        Log.e(TAG, "timer On");
    }

    void turnOnDimmer() {
        overlayOn();
        // UpdateUI
        Intent i = new Intent(this, BReceiver.class);
        i.setAction(ALARM_TIMER_ON);
        sendBroadcast(i);
    }

    void turnOffDimmer() {
        // UpdateUI
        Intent i = new Intent(this, BReceiver.class);
        i.setAction(ALARM_TIMER_OFF);
        sendBroadcast(i);
        // Turn off overlay
        overlayOff();
    }

    void timerOff() {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.cancel(pIntentTimerOn);
        am.cancel(pIntentTimerOff);
        pIntentTimerOn.cancel();
        pIntentTimerOff.cancel();

        boolean isWorking = (PendingIntent.getBroadcast(this, 0, createIntent(ALARM_TIMER_ON), PendingIntent.FLAG_NO_CREATE) != null);//just changed the flag
        Log.e(TAG, "alarm is " + (isWorking ? "" : "not ") + "working...");

        Log.e(TAG, "timer Off");

        if (linearDimmerColor == null && linearDimmer == null && wm == null) {
            stopSelf();
        }
    }

    private void loadSettings() {
        // UI
        if (mSettings.contains(APP_PREFERENCES_THEME)) {
            theme = mSettings.getString(APP_PREFERENCES_THEME, "dark");
        }
        // Overlay
        // DimmerColor
        if (mSettings.contains(APP_PREFERENCES_DIMMER_COLOR)) {
            dimmerColorValue = mSettings.getInt(APP_PREFERENCES_DIMMER_COLOR, 0);
        }
        // Dimmer
        if (mSettings.contains(APP_PREFERENCES_DIMMER)) {
            dimmerValue = mSettings.getInt(APP_PREFERENCES_DIMMER, 0);
        }
        // Temperature
        if (mSettings.contains(APP_PREFERENCES_TEMPERATURE)) {
            temperature = mSettings.getInt(APP_PREFERENCES_TEMPERATURE, 4);
        }

        // Switch
        if (mSettings.contains(APP_PREFERENCES_TIMER_ON)) {
            timerOn = mSettings.getBoolean(APP_PREFERENCES_TIMER_ON, false);
        }
        // Values
        if (mSettings.contains(APP_PREFERENCES_TIMER_HOUR_ON)) {
            timerHourOn = mSettings.getString(APP_PREFERENCES_TIMER_HOUR_ON, "22");
        }
        if (mSettings.contains(APP_PREFERENCES_TIMER_MINUTE_ON)) {
            timerMinuteOn = mSettings.getString(APP_PREFERENCES_TIMER_MINUTE_ON, "0");
        }
        if (mSettings.contains(APP_PREFERENCES_TIMER_HOUR_OFF)) {
            timerHourOff = mSettings.getString(APP_PREFERENCES_TIMER_HOUR_OFF, "7");
        }
        if (mSettings.contains(APP_PREFERENCES_TIMER_MINUTE_OFF)) {
            timerMinuteOff = mSettings.getString(APP_PREFERENCES_TIMER_MINUTE_OFF, "0");
        }
    }

    // Save values
    private void saveSettings(String key, boolean value) {
        // Prepare for save
        SharedPreferences.Editor editor = mSettings.edit();
        // Edit Variables
        editor.putBoolean(key, value);
        // Save
        editor.apply();
    }
}

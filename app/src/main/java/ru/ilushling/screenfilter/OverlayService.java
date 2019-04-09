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
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.RemoteViews;

import com.google.firebase.analytics.FirebaseAnalytics;

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
    // STATIC
    // Common
    static final String TAG = "OverlayService";
    WindowManager wm;
    Notification.Builder notification;
    // Variables
    String theme = "";
    int dimmerColorValue, dimmerValue, temperature;
    boolean timerOn;
    protected String timerHourOn, timerMinuteOn, timerHourOff, timerMinuteOff;
    // Save Settings
    SharedPreferences mSettings;
    // UI
    View viewDimmer;
    public static final String OPEN_ACTION = "ru.ilushling.screenfilter.OPEN_ACTION", CLOSE_ACTION = "ru.ilushling.screenfilter.CLOSE_ACTION",
            ALARM_TIMER_ON = "ru.ilushling.screenfilter.alarmTimerOn", ALARM_TIMER_OFF = "ru.ilushling.screenfilter.alarmTimerOff";
    public static final String NOTIFICATION_CHANNEL_ID = "ilushling.screenfilter", NOTIFICATION_CHANNEL_NAME = "screenfilter";
    private static final int ID_SERVICE = 99544;

    Intent intentTimerOn, intentTimerOff;
    PendingIntent pIntentTimerOn, pIntentTimerOff;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startNotification();
        }
        // Obtain the FirebaseAnalytics instance.
        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(this);

        Bundle bundle = new Bundle();
        firebaseAnalytics.logEvent("start_service", bundle);

        mSettings = getSharedPreferences(APP_PREFERENCES_NAME, Context.MODE_PRIVATE);
        loadSettings();

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
        String action = intent.getAction() != null ? intent.getAction() : "";
        //Log.e(TAG, "action: " + action);

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
                if (wm != null /*&& viewDimmerColor != null*/ && viewDimmer != null) {
                    setView(params);

                    //wm.updateViewLayout(viewDimmerColor, params);
                    wm.updateViewLayout(viewDimmer, params);
                } else {
                    // Add
                    startNotification();
                    wm = (WindowManager) getSystemService(WINDOW_SERVICE);
                    //viewDimmerColor = new View(this);
                    viewDimmer = new View(this);

                    setView(params);

                    //wm.addView(viewDimmerColor, params);
                    wm.addView(viewDimmer, params);
                }

                saveSettings(APP_PREFERENCES_DIMMER_ON, true);

                //Log.e(TAG, "Overlay On");
            }
        } catch (Exception exc) {
            Log.e(TAG, "Overlay On: " + exc);
            overlayOff();
        }
    }

    void setView(WindowManager.LayoutParams params) {
        float colorSliderDivide = dimmerColorValue / 2.18F; // 2.18 Because color slider have max 218 value and RGB system have max 255 value
        float dimmerSliderDivide = (1 + (2.55F * dimmerValue / 218 * 2));
        float balance = colorSliderDivide / dimmerSliderDivide;
        switch (temperature) {
            case 1:
                //viewDimmerColor.setBackgroundColor(Color.argb(dimmerColorValue, 255, 50, 20));
                //viewDimmer.setBackgroundColor(Color.argb(dimmerValue, 0, 0, 0));
                viewDimmer.setBackgroundColor(Color.argb(Math.round(Math.max(dimmerValue, dimmerColorValue)),
                        Math.round(2.55F * balance),
                        Math.round(0.5F * balance),
                        Math.round(0.5F * balance)
                        )
                );
                break;
            case 2:
                //viewDimmerColor.setBackgroundColor(Color.argb(dimmerColorValue, 255, 90, 40));
                //viewDimmer.setBackgroundColor(Color.argb(dimmerValue, 0, 0, 0));
                viewDimmer.setBackgroundColor(Color.argb(Math.round(Math.max(dimmerValue, dimmerColorValue)),
                        Math.round(2.55F * balance),
                        Math.round(0.9F * balance),
                        Math.round(0.4F * balance)
                        )
                );
                break;
            case 3:
                //viewDimmerColor.setBackgroundColor(Color.argb(dimmerColorValue, 255, 110, 50));
                //viewDimmer.setBackgroundColor(Color.argb(dimmerValue, 0, 0, 0));
                viewDimmer.setBackgroundColor(Color.argb(Math.round(Math.max(dimmerValue, dimmerColorValue)),
                        Math.round(2.55F * balance),
                        Math.round(1.1F * balance),
                        Math.round(0.5F * balance)
                        )
                );
                break;
            case 4:
                //viewDimmerColor.setBackgroundColor(Color.argb(dimmerColorValue, 255, 135, 60));
                //viewDimmer.setBackgroundColor(Color.argb(dimmerValue, 0, 0, 0));
                viewDimmer.setBackgroundColor(Color.argb(Math.round(Math.max(dimmerValue, dimmerColorValue)),
                        Math.round(2.55F * balance),
                        Math.round(1.35F * balance),
                        Math.round(0.6F * balance)
                        )
                );
                break;
            case 5:
                //viewDimmerColor.setBackgroundColor(Color.argb(dimmerColorValue, 255, 160, 75));
                //viewDimmer.setBackgroundColor(Color.argb(dimmerValue, 0, 0, 0));
                viewDimmer.setBackgroundColor(Color.argb(Math.round(Math.max(dimmerValue, dimmerColorValue)),
                        Math.round(2.55F * balance),
                        Math.round(1.6F * balance),
                        Math.round(0.75F * balance)
                        )
                );
                break;
            default:
                //viewDimmerColor.setBackgroundColor(Color.argb(dimmerColorValue, 255, 135, 60));
                //viewDimmer.setBackgroundColor(Color.argb(dimmerValue, 0, 0, 0));
                viewDimmer.setBackgroundColor(Color.argb(Math.round(Math.max(dimmerValue, dimmerColorValue)),
                        Math.round(2.55F * balance),
                        Math.round(1.35F * balance),
                        Math.round(0.6F * balance)
                        )
                );
                break;
        }
        params.height = screenHeight();
        params.width = screenWidth();

        // There are some magic formula of balancing color and black
        /*Log.e(TAG, "R " + Math.round(2.55F * balance));
        Log.e(TAG, "G " + Math.round(1.35F * balance));
        Log.e(TAG, "B " + Math.round(0.6F * balance));
        */
    }

    void overlayOff() {
        if (/*viewDimmerColor != null && */viewDimmer != null && wm != null) {
            try {
                saveSettings(APP_PREFERENCES_DIMMER_ON, false);

                //wm.removeView(viewDimmerColor);
                wm.removeView(viewDimmer);
            } catch (Exception exc) {
                Log.e(TAG, "Overlay Off: " + exc);
            }

            wm = null;
            //viewDimmerColor = null;
            viewDimmer = null;

            stopForeground(true);
        }
        //Log.e(TAG, "overlay Off");

        stopSelf();
    }

    // Get screen height with navigation bar height (Because MATCH_PARENT = SCREEN_SIZE - NAV_BAR)
    int screenWidth() {
        // Screen size
        Point size = new Point();
        wm.getDefaultDisplay().getSize(size);
        // Nav bar
        int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            // screen + nav bar
            return size.x + (getResources().getDimensionPixelSize(resourceId) * 2);
        }
        return 0;
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
            return size.y + (getResources().getDimensionPixelSize(resourceId) * 2);
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
                    /*
                case "light":
                    customView = new RemoteViews(this.getPackageName(), R.layout.notification_light);
                    break;
                    */
                case "transparent":
                    customView = new RemoteViews(this.getPackageName(), R.layout.notification_transparent);
                    break;
                default:
                    customView = new RemoteViews(this.getPackageName(), R.layout.notification_dark);
                    break;
            }

            // Intents to BReceiver
            // Open intent
            Intent notificationIntentOpen = new Intent(this, BReceiver.class).setAction(OPEN_ACTION);
            PendingIntent pendingIntentOpen = PendingIntent.getBroadcast(this, 0, notificationIntentOpen, PendingIntent.FLAG_UPDATE_CURRENT);
            // Close intent
            Intent notificationIntentClose = new Intent(this, BReceiver.class).setAction(CLOSE_ACTION);
            PendingIntent pendingIntentClose = PendingIntent.getBroadcast(this, 0, notificationIntentClose, PendingIntent.FLAG_UPDATE_CURRENT);

            // Build notification
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                if (mNotificationManager != null) {
                    int importance = NotificationManager.IMPORTANCE_MIN;
                    NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, importance);
                    mNotificationManager.createNotificationChannel(notificationChannel);
                }
                notification = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                        .setContentTitle(getString(R.string.app_name))
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentIntent(pendingIntentClose)
                        .setCustomContentView(customView)
                        .setAutoCancel(true);
            } else {
                notification = new Notification.Builder(this)
                        .setContentTitle(getString(R.string.app_name))
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentIntent(pendingIntentClose)
                        .setContent(customView)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setAutoCancel(true);
            }
            // Listeners
            customView.setOnClickPendingIntent(R.id.settings, pendingIntentOpen);

            if (mNotificationManager != null) {
                mNotificationManager.notify(ID_SERVICE, notification.build());
                startForeground(ID_SERVICE, notification.build());
            }
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
        if (am != null) {
            am.cancel(pIntentTimerOn);
            am.cancel(pIntentTimerOff);

            am.setRepeating(AlarmManager.RTC, timeOn.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pIntentTimerOn);
            am.setRepeating(AlarmManager.RTC, timeOff.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pIntentTimerOff);
        }
        //Log.e(TAG, "timer On");

        timerOn = true;
        saveSettings(APP_PREFERENCES_TIMER_ON, timerOn);
    }

    void turnOnDimmer() {
        // UpdateUI
        Intent i = createIntent(ALARM_TIMER_ON);
        sendBroadcast(i);
    }

    void turnOffDimmer() {
        // UpdateUI
        Intent i = createIntent(ALARM_TIMER_OFF);
        sendBroadcast(i);
    }

    void timerOff() {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);

        if (am != null) {
            am.cancel(pIntentTimerOn);
            am.cancel(pIntentTimerOff);
        }
        pIntentTimerOn.cancel();
        pIntentTimerOff.cancel();

        boolean isWorking = (PendingIntent.getBroadcast(this, 0, createIntent(ALARM_TIMER_ON), PendingIntent.FLAG_NO_CREATE) != null);//just changed the flag
        Log.e(TAG, "alarm is " + (isWorking ? "" : "not ") + "working...");

        timerOn = false;
        saveSettings(APP_PREFERENCES_TIMER_ON, timerOn);
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

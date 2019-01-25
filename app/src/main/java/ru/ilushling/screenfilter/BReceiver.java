package ru.ilushling.screenfilter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import static ru.ilushling.screenfilter.OverlayService.ALARM_TIMER_OFF;
import static ru.ilushling.screenfilter.OverlayService.ALARM_TIMER_ON;

public class BReceiver extends BroadcastReceiver {

    String TAG = "BroadcastReceiver";

    public static final String APP_OVERLAY_ON = "ru.ilushling.screenfilter.OVERLAY_ON";
    public static final String APP_OVERLAY_OFF = "ru.ilushling.screenfilter.OVERLAY_OFF";

    public BReceiver() {
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction() != null ? intent.getAction() : "";
        //Log.e(TAG, action);

        // Open Activity
        if (action.equals(OverlayService.OPEN_ACTION)) {
            Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            context.sendBroadcast(it);
            // Start activity
            Intent i = new Intent(context, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(i);
        }

        // Close
        if (action.equals(OverlayService.CLOSE_ACTION)) {
            // Service
            Intent i = new Intent(context, OverlayService.class);
            i.setAction("overlayOff");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i);
            } else {
                context.startService(i);
            }
            // Activity
            Intent it = new Intent();
            it.setAction(OverlayService.CLOSE_ACTION);
            context.sendBroadcast(it);
        }

        // Timer
        // ON
        if (action.equals(ALARM_TIMER_ON)) {
            // Service
            Intent i = new Intent(context, OverlayService.class);
            i.setAction("alarmTimerOn");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i);
            } else {
                context.startService(i);
            }
            // Activity
            i = new Intent();
            i.setAction(APP_OVERLAY_ON);
            context.sendBroadcast(i);

            Log.e(TAG, "alarmTimerOn");
        }
        // OFF
        if (action.equals(ALARM_TIMER_OFF)) {
            // Service
            Intent i = new Intent(context, OverlayService.class);
            i.setAction("alarmTimerOff");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i);
            } else {
                context.startService(i);
            }
            // Activity
            i = new Intent();
            i.setAction(APP_OVERLAY_OFF);
            context.sendBroadcast(i);

            Log.e(TAG, "alarmTimerOff");
        }

        // Check alarm timer after reboot
        if (intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {
            Intent serviceIntent = new Intent(context, OverlayService.class);
            serviceIntent.setAction("timerOn");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }

    }
}

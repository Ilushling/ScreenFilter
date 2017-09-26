package ru.ilushling.screenfilter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static ru.ilushling.screenfilter.OverlayService.ALARM_TIMER_OFF;
import static ru.ilushling.screenfilter.OverlayService.ALARM_TIMER_ON;

public class BReceiver extends BroadcastReceiver {

    String TAG = "BroadcastReceiver";

    public static final String APP_PREFERENCES_OVERLAY = "OVERLAY";

    public BReceiver() {
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

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
            context.stopService(new Intent(context, OverlayService.class));
            // Activity
            Intent it = new Intent();
            it.setAction(OverlayService.CLOSE_ACTION);
            context.sendBroadcast(it);
        }

        // Timer
        // ON
        if (action == ALARM_TIMER_ON) {
            // Service
            Intent i = new Intent(context, OverlayService.class);
            i.setAction("alarmTimerOn");
            context.startService(i);
            // Activity
            i = new Intent();
            i.setAction(APP_PREFERENCES_OVERLAY);
            i.putExtra("overlayOn", true);
            context.sendBroadcast(i);

            //Log.e(TAG, "On");
        }
        // OFF
        if (action == ALARM_TIMER_OFF) {
            // Service
            Intent i = new Intent(context, OverlayService.class);
            i.setAction(APP_PREFERENCES_OVERLAY);
            context.startService(i);
            // Activity
            i = new Intent();
            i.setAction(APP_PREFERENCES_OVERLAY);
            i.putExtra("overlayOn", false);
            context.sendBroadcast(i);

            //Log.e(TAG, "Off");
        }

    }
}

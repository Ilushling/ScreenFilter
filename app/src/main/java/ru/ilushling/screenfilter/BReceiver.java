package ru.ilushling.screenfilter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BReceiver extends BroadcastReceiver {

    String TAG = "BroadcastReceiver";

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

    }
}

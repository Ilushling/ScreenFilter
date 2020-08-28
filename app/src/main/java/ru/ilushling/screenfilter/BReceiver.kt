package ru.ilushling.screenfilter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BReceiver : BroadcastReceiver() {
    private var tag = "BroadcastReceiver"
    override fun onReceive(context: Context, intent: Intent) {
        val action = if (intent.action != null) intent.action else ""
        //Log.i(TAG, action);

        // Open Activity
        if (action == OverlayService.OPEN_ACTION) {
            val it = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            context.sendBroadcast(it)

            // Start activity
            val i = Intent(context, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(i)
        }

        // Close
        if (action == OverlayService.CLOSE_ACTION) {
            // Service
            val i = Intent(context, OverlayService::class.java)
            i.action = "overlayOff"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
            // Activity
            val it = Intent()
            it.action = OverlayService.CLOSE_ACTION
            context.sendBroadcast(it)
        }

        // Timer
        // ON
        if (action == OverlayService.ALARM_TIMER_ON) {
            // Service
            var i = Intent(context, OverlayService::class.java)
            i.action = "alarmTimerOn"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }

            // Activity
            i = Intent()
            i.action = APP_OVERLAY_ON
            context.sendBroadcast(i)

            Log.i(tag, "alarmTimerOn")
        }

        // OFF
        if (action == OverlayService.ALARM_TIMER_OFF) {
            // Service
            var i = Intent(context, OverlayService::class.java)
            i.action = "alarmTimerOff"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }

            // Activity
            i = Intent()
            i.action = APP_OVERLAY_OFF
            context.sendBroadcast(i)

            Log.i(tag, "alarmTimerOff")
        }

        // Check alarm timer after reboot
        if (action.equals(Intent.ACTION_BOOT_COMPLETED, ignoreCase = true)) {
            val serviceIntent = Intent(context, OverlayService::class.java)
            serviceIntent.action = "timerOn"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }

    companion object {
        const val APP_OVERLAY_ON = "ru.ilushling.screenfilter.OVERLAY_ON"
        const val APP_OVERLAY_OFF = "ru.ilushling.screenfilter.OVERLAY_OFF"
    }
}
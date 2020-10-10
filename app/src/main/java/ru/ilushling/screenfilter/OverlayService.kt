package ru.ilushling.screenfilter

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.RemoteViews
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.analytics.FirebaseAnalytics
import java.util.*
import kotlin.math.max
import kotlin.math.roundToInt

class OverlayService : Service() {
    private var wm: WindowManager? = null
    private var notification: NotificationCompat.Builder? = null

    // Variables
    private var theme: String? = ""
    private var dimmerColorValue = 0
    private var dimmerValue = 0
    private var temperature = 0
    private var timerOn = false
    private lateinit var timerHourOn: String
    private lateinit var timerMinuteOn: String
    private lateinit var timerHourOff: String
    private lateinit var timerMinuteOff: String

    // Save Settings
    private lateinit var mSettings: SharedPreferences

    // UI
    private var viewDimmer: View? = null
    private lateinit var intentTimerOn: Intent
    private lateinit var intentTimerOff: Intent
    private lateinit var pIntentTimerOn: PendingIntent
    private lateinit var pIntentTimerOff: PendingIntent

    // Firebase
    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startNotification()
        }

        // Obtain the FirebaseAnalytics instance.
        val firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        val bundle = Bundle()
        firebaseAnalytics.logEvent("createService", bundle)

        mSettings = getSharedPreferences(MainActivity.APP_PREFERENCES_NAME, MODE_PRIVATE)
        loadSettings()
        intentTimerOn = createIntent(ALARM_TIMER_ON)
        pIntentTimerOn = PendingIntent.getBroadcast(this, 0, intentTimerOn, PendingIntent.FLAG_UPDATE_CURRENT)
        intentTimerOff = createIntent(ALARM_TIMER_OFF)
        pIntentTimerOff = PendingIntent.getBroadcast(this, 0, intentTimerOff, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun createIntent(action: String?): Intent {
        val intent = Intent(this, BReceiver::class.java)
        intent.action = action
        return intent
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when (if (intent.action != null) intent.action else "") {
            "overlayOn" -> {
                theme = intent.getStringExtra("theme")
                dimmerColorValue = intent.getIntExtra("dimmerColorValue", 0)
                dimmerValue = intent.getIntExtra("dimmerValue", 0)
                temperature = intent.getIntExtra("temperature", 4)

                overlayOn()
            }
            "overlayOff" -> overlayOff()
            "timerOn" -> {
                loadSettings()

                if (timerOn) {
                    timerOn(timerHourOn, timerMinuteOn, timerHourOff, timerMinuteOff)
                } else {
                    overlayOff()
                }
            }
            "timerOff" -> timerOff()
            "alarmTimerOn" -> {
                loadSettings()

                overlayOn()
            }
            "alarmTimerOff" -> overlayOff()
            "theme" -> {
                theme = intent.getStringExtra("theme")

                stopForeground(true)

                startNotification()
            }
        }
        return START_REDELIVER_INTENT
    }

    // Update sizes when device rotate
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        overlayOn()

        Log.i(TAG, "rotate")
    }

    private fun overlayOn() {
        try {
            if (dimmerColorValue == 0 && dimmerValue == 0) {
                notification = null
            } else {
                // Prepare UI
                // UI Params
                val params: WindowManager.LayoutParams = WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN or
                                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
                        PixelFormat.TRANSPARENT)
                params.gravity = Gravity.START or Gravity.TOP
                params.x = 0
                params.y = 0

                // Add or Update UI
                if (wm != null /*&& viewDimmerColor != null*/ && viewDimmer != null) {
                    setView(params)

                    //wm.updateViewLayout(viewDimmerColor, params);
                    wm!!.updateViewLayout(viewDimmer, params)
                } else {
                    // Add
                    startNotification()
                    wm = getSystemService(WINDOW_SERVICE) as WindowManager?
                    //viewDimmerColor = new View(this);
                    viewDimmer = View(this)

                    setView(params)

                    //wm.addView(viewDimmerColor, params);
                    wm!!.addView(viewDimmer, params)
                }

                saveSettings(MainActivity.APP_PREFERENCES_DIMMER_ON, true)

                Log.i(TAG, "Overlay On")

                // Obtain the FirebaseAnalytics instance.
                val bundle = Bundle()
                mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
                mFirebaseAnalytics.logEvent("overlayOn", bundle)
            }
        } catch (exc: Exception) {
            Log.e(TAG, "Error Overlay On: $exc")
            overlayOff()
        }
    }

    private fun setView(params: WindowManager.LayoutParams) {
        val colorSliderDivide = dimmerColorValue / 2.18f // 2.18 Because color slider have max 218 value and RGB system have max 255 value
        val dimmerSliderDivide = 1 + 2.55f * dimmerValue / 218 * 2
        val balance = colorSliderDivide / dimmerSliderDivide

        when (temperature) {
            1 -> viewDimmer!!.setBackgroundColor(Color.argb(max(dimmerValue, dimmerColorValue).toFloat().roundToInt(),
                    (2.55f * balance).roundToInt(),
                    (0.5f * balance).roundToInt(),
                    (0.5f * balance).roundToInt()
            )
            )
            2 -> viewDimmer!!.setBackgroundColor(Color.argb(max(dimmerValue, dimmerColorValue).toFloat().roundToInt(),
                    (2.55f * balance).roundToInt(),
                    (0.9f * balance).roundToInt(),
                    (0.4f * balance).roundToInt()
            )
            )
            3 -> viewDimmer!!.setBackgroundColor(Color.argb(max(dimmerValue, dimmerColorValue).toFloat().roundToInt(),
                    (2.55f * balance).roundToInt(),
                    (1.1f * balance).roundToInt(),
                    (0.5f * balance).roundToInt()
            )
            )
            4 -> viewDimmer!!.setBackgroundColor(Color.argb(max(dimmerValue, dimmerColorValue).toFloat().roundToInt(),
                    (2.55f * balance).roundToInt(),
                    (1.35f * balance).roundToInt(),
                    (0.6f * balance).roundToInt()
            )
            )
            5 -> viewDimmer!!.setBackgroundColor(Color.argb(max(dimmerValue, dimmerColorValue).toFloat().roundToInt(),
                    (2.55f * balance).roundToInt(),
                    (1.6f * balance).roundToInt(),
                    (0.75f * balance).roundToInt()
            )
            )
            else -> viewDimmer!!.setBackgroundColor(Color.argb(max(dimmerValue, dimmerColorValue).toFloat().roundToInt(),
                    (2.55f * balance).roundToInt(),
                    (1.35f * balance).roundToInt(),
                    (0.6f * balance).roundToInt()
            )
            )
        }

        params.height = screenHeight()
        params.width = screenWidth()

        // There are some magic formula of balancing color and black
        /*Log.e(TAG, "R " + Math.round(2.55F * balance));
        Log.e(TAG, "G " + Math.round(1.35F * balance));
        Log.e(TAG, "B " + Math.round(0.6F * balance));
        */
    }

    private fun overlayOff() {
        try {
            saveSettings(MainActivity.APP_PREFERENCES_DIMMER_ON, false)

            //wm.removeView(viewDimmerColor);
            wm?.removeView(viewDimmer)
        } catch (exc: Exception) {
            Log.e(TAG, "Overlay Off: $exc")
        }

        wm = null
        //viewDimmerColor = null;
        viewDimmer = null
        stopForeground(true)

        Log.i(TAG, "overlay Off")

        // Obtain the FirebaseAnalytics instance.
        val bundle = Bundle()
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        mFirebaseAnalytics.logEvent("overlayOff", bundle)

        stopSelf()
    }

    // Get screen height with navigation bar height (Because MATCH_PARENT = SCREEN_SIZE - NAV_BAR)
    private fun screenWidth(): Int {
        // Screen size
        val size = Point()
        wm!!.defaultDisplay.getSize(size)

        // Nav bar
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            // screen + nav bar
            size.x + resources.getDimensionPixelSize(resourceId) * 2
        } else 0
    }

    // Get screen height with navigation bar height (Because MATCH_PARENT = SCREEN_SIZE - NAV_BAR)
    private fun screenHeight(): Int {
        // Screen size
        val size = Point()
        wm!!.defaultDisplay.getSize(size)

        // Nav bar
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            // screen + nav bar
            size.y + resources.getDimensionPixelSize(resourceId) * 2
        } else 0
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.i(TAG, "Service: onDestroy")

        overlayOff()
    }

    private fun startNotification() {
        // Prepare
        val mNotificationManager = NotificationManagerCompat.from(this)
        // Create remote view and set bigContentView.
        val customView = RemoteViews(this.packageName, R.layout.notification_dark)

        // Intents to BReceiver
        // Open intent
        val notificationIntentOpen = Intent(this, BReceiver::class.java).setAction(OPEN_ACTION)
        val pendingIntentOpen = PendingIntent.getBroadcast(this, 0, notificationIntentOpen, PendingIntent.FLAG_UPDATE_CURRENT)
        // Close intent
        val notificationIntentClose = Intent(this, BReceiver::class.java).setAction(CLOSE_ACTION)
        val pendingIntentClose = PendingIntent.getBroadcast(this, 0, notificationIntentClose, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationChannel = NotificationChannelCompat.Builder(NOTIFICATION_CHANNEL_ID, NotificationManager.IMPORTANCE_MIN)
                .setName(NOTIFICATION_CHANNEL_NAME)
                .build()
        mNotificationManager.createNotificationChannel(notificationChannel)
        // Build notification
        notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setColor(ContextCompat.getColor(this, R.color.colorAccent))
                .setContentIntent(pendingIntentClose)
                .setCustomContentView(customView)
                .setAutoCancel(true)
        // Listeners
        customView.setOnClickPendingIntent(R.id.settings, pendingIntentOpen)
        mNotificationManager.notify(ID_SERVICE, notification!!.build())
        startForeground(ID_SERVICE, notification!!.build())
    }

    private fun timerOn(timerHourOn: String, timerMinuteOn: String, timerHourOff: String, timerMinuteOff: String) {
        val am = getSystemService(ALARM_SERVICE) as AlarmManager

        // Convert timer
        val now = Calendar.getInstance()
        val timeOn = Calendar.getInstance()
        val timeOff = Calendar.getInstance()
        timeOn.timeInMillis = System.currentTimeMillis()
        timeOff.timeInMillis = System.currentTimeMillis()
        // Apply timer
        timeOn[Calendar.HOUR_OF_DAY] = timerHourOn.toInt()
        timeOn[Calendar.MINUTE] = timerMinuteOn.toInt()
        timeOff[Calendar.HOUR_OF_DAY] = timerHourOff.toInt()
        timeOff[Calendar.MINUTE] = timerMinuteOff.toInt()


        // if timeoff before timeon than timeoff set to next day
        if (timeOff.before(now)) {
            // Timer to next day
            if (timeOn.after(timeOff)) {
                timeOff.add(Calendar.DAY_OF_MONTH, 1)
            }
            if ((timeOn.before(now) || timeOn == now) && timeOn.before(timeOff) && timeOff.after(now)) {
                // TimeOn after than TimeOff ex. (TimeOn = 22:00 and TimeOff = 06:00)
                turnOnDimmer()
            } else {
                turnOffDimmer()
            }
        } else {
            if (timeOn.before(now) && timeOff.after(now)) {
                // TimeOn before than TimeOff ex. (TimeOn = 01:00 and TimeOff = 06:00 now = 03:00)
                turnOnDimmer()
            } else {
                if (timeOn.before(timeOff)) {
                    // TimeOn after than now ex. (TimeOn = 02:00 and TimeOff = 06:00 now = 01:00)
                    turnOffDimmer()
                } else {
                    // TimeOn a day before than TimeOff ex. (TimeOn = 22:00 and TimeOff = 06:00 now = 03:00)
                    turnOnDimmer()
                }
            }
        }

        // Start timer
        am.cancel(pIntentTimerOn)
        am.cancel(pIntentTimerOff)
        am.setRepeating(AlarmManager.RTC, timeOn.timeInMillis, AlarmManager.INTERVAL_DAY, pIntentTimerOn)
        am.setRepeating(AlarmManager.RTC, timeOff.timeInMillis, AlarmManager.INTERVAL_DAY, pIntentTimerOff)

        timerOn = true
        saveSettings(MainActivity.APP_PREFERENCES_TIMER_ON, timerOn)

        Log.i(TAG, "timer On")

        // Obtain the FirebaseAnalytics instance.
        val bundle = Bundle()
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        mFirebaseAnalytics.logEvent("timerOn", bundle)
    }

    private fun turnOnDimmer() {
        // UpdateUI
        val i = createIntent(ALARM_TIMER_ON)
        sendBroadcast(i)

        // Obtain the FirebaseAnalytics instance.
        val bundle = Bundle()
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        mFirebaseAnalytics.logEvent("turnOnDimmer", bundle)
    }

    private fun turnOffDimmer() {
        // UpdateUI
        val i = createIntent(ALARM_TIMER_OFF)
        sendBroadcast(i)

        // Obtain the FirebaseAnalytics instance.
        val bundle = Bundle()
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        mFirebaseAnalytics.logEvent("turnOffDimmer", bundle)
    }

    private fun timerOff() {
        val am = getSystemService(ALARM_SERVICE) as AlarmManager
        am.cancel(pIntentTimerOn)
        am.cancel(pIntentTimerOff)
        pIntentTimerOn.cancel()
        pIntentTimerOff.cancel()

        val isWorking = PendingIntent.getBroadcast(this, 0, createIntent(ALARM_TIMER_ON), PendingIntent.FLAG_NO_CREATE) != null // just changed the flag

        timerOn = false
        saveSettings(MainActivity.APP_PREFERENCES_TIMER_ON, timerOn)

        Log.i(TAG, "alarm is " + (if (isWorking) "" else "not ") + "working...")

        // Obtain the FirebaseAnalytics instance.
        val bundle = Bundle()
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        mFirebaseAnalytics.logEvent("timerOff", bundle)
    }

    private fun loadSettings() {
        // UI
        if (mSettings.contains(MainActivity.APP_PREFERENCES_THEME)) {
            theme = mSettings.getString(MainActivity.APP_PREFERENCES_THEME, "dark")
        }
        // Overlay
        // DimmerColor
        if (mSettings.contains(MainActivity.APP_PREFERENCES_DIMMER_COLOR)) {
            dimmerColorValue = mSettings.getInt(MainActivity.APP_PREFERENCES_DIMMER_COLOR, 0)
        }
        // Dimmer
        if (mSettings.contains(MainActivity.APP_PREFERENCES_DIMMER)) {
            dimmerValue = mSettings.getInt(MainActivity.APP_PREFERENCES_DIMMER, 0)
        }
        // Temperature
        if (mSettings.contains(MainActivity.APP_PREFERENCES_TEMPERATURE)) {
            temperature = mSettings.getInt(MainActivity.APP_PREFERENCES_TEMPERATURE, 4)
        }

        // Switch
        if (mSettings.contains(MainActivity.APP_PREFERENCES_TIMER_ON)) {
            timerOn = mSettings.getBoolean(MainActivity.APP_PREFERENCES_TIMER_ON, false)
        }
        // Values
        if (mSettings.contains(MainActivity.APP_PREFERENCES_TIMER_HOUR_ON)) {
            timerHourOn = mSettings.getString(MainActivity.APP_PREFERENCES_TIMER_HOUR_ON, "22").toString()
        }
        if (mSettings.contains(MainActivity.APP_PREFERENCES_TIMER_MINUTE_ON)) {
            timerMinuteOn = mSettings.getString(MainActivity.APP_PREFERENCES_TIMER_MINUTE_ON, "0").toString()
        }
        if (mSettings.contains(MainActivity.APP_PREFERENCES_TIMER_HOUR_OFF)) {
            timerHourOff = mSettings.getString(MainActivity.APP_PREFERENCES_TIMER_HOUR_OFF, "7").toString()
        }
        if (mSettings.contains(MainActivity.APP_PREFERENCES_TIMER_MINUTE_OFF)) {
            timerMinuteOff = mSettings.getString(MainActivity.APP_PREFERENCES_TIMER_MINUTE_OFF, "0").toString()
        }
    }

    // Save values
    private fun saveSettings(key: String, value: Boolean) {
        // Prepare for save
        val editor = mSettings.edit()
        // Edit Variables
        editor.putBoolean(key, value)
        // Save
        editor.apply()
    }

    companion object {
        // STATIC
        // Common
        const val TAG = "OverlayService"
        const val OPEN_ACTION = "ru.ilushling.screenfilter.OPEN_ACTION"
        const val CLOSE_ACTION = "ru.ilushling.screenfilter.CLOSE_ACTION"
        const val ALARM_TIMER_ON = "ru.ilushling.screenfilter.alarmTimerOn"
        const val ALARM_TIMER_OFF = "ru.ilushling.screenfilter.alarmTimerOff"
        const val NOTIFICATION_CHANNEL_ID = "ilushling.screenfilter"
        const val NOTIFICATION_CHANNEL_NAME = "screenfilter"
        private const val ID_SERVICE = 99544
    }
}
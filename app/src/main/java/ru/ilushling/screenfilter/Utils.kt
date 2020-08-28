package ru.ilushling.screenfilter

import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.Toast
import com.google.firebase.analytics.FirebaseAnalytics

class Utils(private val context: Context) {
    private val notificationManager: NotificationManager
    private val audioManager: AudioManager
    private val mFirebaseAnalytics: FirebaseAnalytics

    // [START Battery protection]
    fun protectAppManager() {
        val settings = context.getSharedPreferences(MainActivity.APP_PREFERENCES_NAME, Context.MODE_PRIVATE)
        val skipMessage = settings.getBoolean("skipProtectedAppCheck", false)

        // Obtain the FirebaseAnalytics instance.
        val mFirebaseAnalytics = FirebaseAnalytics.getInstance(context)
        if (!skipMessage) {
            val editor = settings.edit()
            var foundCorrectIntent = false
            for (intent in ListPowerManager.POWERMANAGER_INTENTS) {
                if (isCallable(context, intent)) {
                    // Firebase
                    val bundleShow = Bundle()
                    bundleShow.putString(FirebaseAnalytics.Param.ITEM_ID, "1")
                    bundleShow.putString(FirebaseAnalytics.Param.ITEM_NAME, "protectAppManager")
                    bundleShow.putString(FirebaseAnalytics.Param.ITEM_BRAND, Build.MANUFACTURER)
                    bundleShow.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "Show")
                    mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundleShow)
                    foundCorrectIntent = true
                    val dontShowView = View.inflate(context, R.layout.check_box, null)
                    val dontShowAgain = dontShowView.findViewById<CheckBox>(R.id.checkBox)
                    dontShowAgain.setText(R.string.do_not_show_again)
                    dontShowAgain.setOnCheckedChangeListener { _, isChecked ->
                        editor.putBoolean("skipProtectedAppCheck", isChecked)
                        editor.apply()
                    }
                    AlertDialog.Builder(context)
                            .setTitle(Build.MANUFACTURER + " Protected Apps")
                            .setMessage(String.format(context.getString(R.string.protected_apps_dialog), context.getString(R.string.app_name)))
                            .setView(dontShowView)
                            .setPositiveButton(R.string.go_to_settings) { _, _ ->
                                context.startActivity(intent)
                                val bundleSet = Bundle()
                                bundleSet.putString(FirebaseAnalytics.Param.ITEM_ID, "1")
                                bundleSet.putString(FirebaseAnalytics.Param.ITEM_NAME, "protectAppManager")
                                bundleSet.putString(FirebaseAnalytics.Param.ITEM_BRAND, Build.MANUFACTURER)
                                bundleSet.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "Set")
                                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundleSet)
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                    break
                }
            }
            if (!foundCorrectIntent) {
                editor.putBoolean("skipProtectedAppCheck", true)
                editor.apply()
            }
        }
    }

    // [END Battery protection]
    // [START Sound Mode]
    val soundMode: Int
        get() = try {
            audioManager.ringerMode
        } catch (exc: Exception) {
            Log.e(TAG, "" + exc)
            val bundle = Bundle()
            bundle.putString("error", "" + exc)
            mFirebaseAnalytics.logEvent("audioMangerErrorGetRingerMode", bundle)
            0
        }

    fun setSoundMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                val i = Intent()
                i.action = MainActivity.CHECK_AUDIO_PERMISSION
                context.sendBroadcast(i)
                return
            }
        }
        when (soundMode) {
            AudioManager.RINGER_MODE_SILENT -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                if (soundMode == AudioManager.RINGER_MODE_SILENT) {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                }
                if (soundMode == AudioManager.RINGER_MODE_SILENT) {
                    Toast.makeText(context, context.resources.getString(R.string.no_permission_audio), Toast.LENGTH_SHORT).show()
                }
            }
            AudioManager.RINGER_MODE_VIBRATE -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                if (soundMode == AudioManager.RINGER_MODE_VIBRATE) {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                }
                if (soundMode == AudioManager.RINGER_MODE_VIBRATE) {
                    Toast.makeText(context, context.resources.getString(R.string.no_permission_audio), Toast.LENGTH_SHORT).show()
                }
            }
            AudioManager.RINGER_MODE_NORMAL -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                if (soundMode == AudioManager.RINGER_MODE_NORMAL) {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                }
                if (soundMode == AudioManager.RINGER_MODE_NORMAL) {
                    Toast.makeText(context, context.resources.getString(R.string.no_permission_audio), Toast.LENGTH_SHORT).show()
                }
            }
            else -> Log.e(TAG, "sound mode $soundMode")
        }
        Log.e(TAG, "sound mode $soundMode")
    } // [END Sound Mode]

    companion object {
        private const val TAG = "Utils"
        private fun isCallable(context: Context, intent: Intent): Boolean {
            val list = context.packageManager.queryIntentActivities(intent,
                    PackageManager.MATCH_DEFAULT_ONLY)
            return list.size > 0
        }
    }

    init {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(context)
    }
}
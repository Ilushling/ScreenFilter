package ru.ilushling.screenfilter

import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import com.google.firebase.analytics.FirebaseAnalytics

class Utils(private val context: Context) {
    private val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val mFirebaseAnalytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(context)

    // [START Battery protection]
    fun protectAppManager() {
        val settings = context.getSharedPreferences(MainActivity.APP_PREFERENCES_NAME, Context.MODE_PRIVATE)
        val skipMessage = settings.getBoolean("skipProtectedAppCheck", false)

        // Obtain the FirebaseAnalytics instance.
        val mFirebaseAnalytics = FirebaseAnalytics.getInstance(context)
        if (!skipMessage) {
            val editor = settings.edit()
            var foundCorrectIntent = false
            for (intent in ListPowerManager.POWER_MANAGER_INTENTS) {
                if (isCallable(context, intent)) {
                    // Firebase
                    val bundleShow = Bundle()
                    bundleShow.putString(FirebaseAnalytics.Param.ITEM_ID, "1")
                    bundleShow.putString(FirebaseAnalytics.Param.ITEM_NAME, "protectAppManager")
                    bundleShow.putString(FirebaseAnalytics.Param.ITEM_BRAND, Build.MANUFACTURER)
                    bundleShow.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "Show")
                    mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundleShow)
                    foundCorrectIntent = true
                    val noShowView = View.inflate(context, R.layout.check_box, null)
                    val noShowAgain = noShowView.findViewById<CheckBox>(R.id.checkBox)
                    noShowAgain.setText(R.string.do_not_show_again)
                    noShowAgain.setOnCheckedChangeListener { _, isChecked ->
                        editor.putBoolean("skipProtectedAppCheck", isChecked)
                        editor.apply()
                    }
                    AlertDialog.Builder(context)
                            .setTitle(Build.MANUFACTURER + " Protected Apps")
                            .setMessage(String.format(context.getString(R.string.protected_apps_dialog), context.getString(R.string.app_name)))
                            .setView(noShowView)
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

    companion object {
        private const val tag = "Utils"
        private fun isCallable(context: Context, intent: Intent): Boolean {
            val list = context.packageManager.queryIntentActivities(intent,
                    PackageManager.MATCH_DEFAULT_ONLY)
            return list.size > 0
        }
    }
}
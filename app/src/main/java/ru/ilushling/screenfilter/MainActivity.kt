package ru.ilushling.screenfilter

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.*
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

class MainActivity : Activity(), View.OnClickListener {
    private var first = true
    private var charity = false
    private var openUISettings = false
    private lateinit var charityCB: CheckBox
    private lateinit var timerTimeOn: TextView
    private lateinit var timerTimeOff: TextView
    lateinit var dimmer: SeekBar
    lateinit var dimmerColor: SeekBar

    // UI
    lateinit var UIMain: ConstraintLayout
    lateinit var Wrapper: ConstraintLayout
    lateinit var Wrapper1: ConstraintLayout
    lateinit var UISettings: ConstraintLayout
    private var utils: Utils? = null
    lateinit var dimmerColorStatus: TextView
    lateinit var dimmerStatus: TextView
    private lateinit var mAdView: AdView

    // Policy
    private lateinit var policyButton: ImageButton
    private lateinit var policyText: TextView

    // Save Settings
    private var mSettings: SharedPreferences? = null
    private var mFirebaseAnalytics: FirebaseAnalytics? = null

    // Listener for dialog
    private var listenerOverlay: DialogInterface.OnClickListener = object : DialogInterface.OnClickListener {
        val BUTTON_NEGATIVE = -2
        val BUTTON_POSITIVE = -1
        override fun onClick(dialog: DialogInterface, which: Int) {
            when (which) {
                BUTTON_NEGATIVE ->                     // int which = -2
                    dialog.dismiss()
                BUTTON_POSITIVE -> {
                    // int which = -1
                    var intent: Intent? = null
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    }
                    startActivityForResult(intent, 1)
                    dialog.dismiss()
                }
            }
        }
    }
    private lateinit var settingsButton: ImageButton
    private lateinit var soundMode: ImageButton
    lateinit var dimmerSwitch: Switch

    // Variables
    private var theme: String? = null
    private var dimmerColorValue = 0
    private var dimmerValue = 0
    private var dimmerOn = false
    private var timerOn = false
    private var temperature = 0
    private var timerHourOn: String? = null
    private var timerMinuteOn: String? = null
    private var timerHourOff: String? = null
    private var timerMinuteOff: String? = null
    lateinit var timerSwitch: Switch

    // Listener for dialog
    var listenerSound: DialogInterface.OnClickListener = object : DialogInterface.OnClickListener {
        val BUTTON_NEGATIVE = -2
        val BUTTON_POSITIVE = -1
        override fun onClick(dialog: DialogInterface, which: Int) {
            when (which) {
                BUTTON_NEGATIVE ->                     // int which = -2
                    dialog.dismiss()
                BUTTON_POSITIVE -> {
                    // int which = -1
                    var intent: Intent? = null
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    }
                    startActivityForResult(intent, 2)
                    dialog.dismiss()
                }
            }
        }
    }

    // RadioButton lightTheme;
    private lateinit var temperature1RB: RadioButton
    private lateinit var temperature2RB: RadioButton
    private lateinit var temperature3RB: RadioButton
    private lateinit var temperature4RB: RadioButton
    private lateinit var temperature5RB: RadioButton
    private lateinit var darkTheme: RadioButton
    private lateinit var transparentTheme: RadioButton
    private var mFirebaseRemoteConfig: FirebaseRemoteConfig? = null

    // Close
    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            //Log.e(TAG, intent.getAction());
            when (if (intent.action != null) intent.action else "") {
                BReceiver.APP_OVERLAY_ON -> dimmerSwitch.isChecked = true
                BReceiver.APP_OVERLAY_OFF -> dimmerSwitch.isChecked = false
                OverlayService.CLOSE_ACTION -> dimmerSwitch.isChecked = false
                AudioManager.RINGER_MODE_CHANGED_ACTION -> getSoundMode()
                CHECK_AUDIO_PERMISSION -> showPermission("sound", context.getString(R.string.restriction_sound))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        mAdView = findViewById(R.id.adView)
        MobileAds.initialize(this, getString(R.string.ad_app_id))
        val bundle = Bundle()
        mFirebaseAnalytics!!.logEvent("open_app", bundle)

        // ADS
        // Remote settings
        try {
            mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
            val remoteConfigSettings = FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(90)
                    .build()
            mFirebaseRemoteConfig!!.setConfigSettingsAsync(remoteConfigSettings)
            mFirebaseRemoteConfig!!.setDefaults(R.xml.remote_config_defaults)
            fetchRemoteConfig()
            val enableAd = mFirebaseRemoteConfig!!.getBoolean("enableAd")
            if (enableAd) {
                showAd()
            }
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }

        //Log.e(TAG, "enableAd: " + enableAd);
        temperature1RB = findViewById(R.id.temperature1)
        temperature1RB.setOnClickListener(this)
        temperature2RB = findViewById(R.id.temperature2)
        temperature2RB.setOnClickListener(this)
        temperature3RB = findViewById(R.id.temperature3)
        temperature3RB.setOnClickListener(this)
        temperature4RB = findViewById(R.id.temperature4)
        temperature4RB.setOnClickListener(this)
        temperature5RB = findViewById(R.id.temperature5)
        temperature5RB.setOnClickListener(this)
        val gd = temperature1RB.background as GradientDrawable
        gd.setColor(Color.rgb(255, 50, 20))
        val gd2 = temperature2RB.background as GradientDrawable
        gd2.setColor(Color.rgb(255, 90, 40))
        val gd3 = temperature3RB.background as GradientDrawable
        gd3.setColor(Color.rgb(255, 110, 50))
        val gd4 = temperature4RB.background as GradientDrawable
        gd4.setColor(Color.rgb(255, 135, 60))
        val gd5 = temperature5RB.background as GradientDrawable
        gd5.setColor(Color.rgb(255, 160, 75))

        // UI
        Wrapper = findViewById(R.id.Wrapper)
        Wrapper.setOnClickListener(this)
        Wrapper1 = findViewById(R.id.Wrapper1)
        Wrapper1.setOnClickListener(this)
        UIMain = findViewById(R.id.UIMain)
        UIMain.setOnClickListener(View.OnClickListener { })
        UISettings = findViewById(R.id.UISettings)
        UISettings.visibility = View.GONE
        UISettings.setOnClickListener(View.OnClickListener { })
        settingsButton = findViewById(R.id.settingsButton)
        settingsButton.setOnClickListener(this)
        // Dimmer
        dimmerSwitch = findViewById(R.id.dimmerSwitch)
        dimmerColor = findViewById(R.id.dimmerColorSeekbar)
        dimmerColorStatus = findViewById(R.id.dimmerColorStatus)
        dimmer = findViewById(R.id.dimmerSeekbar)
        dimmerStatus = findViewById(R.id.dimmerStatus)
        // Timer
        timerTimeOn = findViewById(R.id.timerTimeOn)
        timerTimeOn.setOnClickListener(this)
        timerTimeOff = findViewById(R.id.timerTimeOff)
        timerTimeOff.setOnClickListener(this)
        timerSwitch = findViewById(R.id.timerSwitch)

        // Theme
        darkTheme = findViewById(R.id.darkTheme)
        darkTheme.setOnClickListener(this)
        /*
        lightTheme = findViewById(R.id.lightTheme);
        lightTheme.setOnClickListener(this);
        */transparentTheme = findViewById(R.id.transparentTheme)
        transparentTheme.setOnClickListener(this)

        // Policy
        policyButton = findViewById(R.id.policyButton)
        policyButton.setOnClickListener(this)
        policyText = findViewById(R.id.policyText)
        policyText.setOnClickListener(this)

        // Charity
        charityCB = findViewById(R.id.charity)
        charityCB.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
            charity = isChecked
            // Analytics
            var charityLoad = charity
            if (mSettings!!.contains(APP_PREFERENCES_CHARITY)) {
                try {
                    charityLoad = mSettings!!.getBoolean(APP_PREFERENCES_CHARITY, false)
                } catch (e: Exception) {
                }
            }
            if (!isChecked) {
                //hideAd();
                if (charityLoad != charity) {
                    val bundle = Bundle()
                    bundle.putString("Charity", "user activate thanks")
                    mFirebaseAnalytics!!.logEvent("activate_charity", bundle)
                }
            } else {
                //showAd();
                if (charityLoad != charity) {
                    val bundle = Bundle()
                    bundle.putString("Charity", "user deactivate app")
                    mFirebaseAnalytics!!.logEvent("deactivate_charity", bundle)
                }
            }
            saveSettings(APP_PREFERENCES_CHARITY, charity)
        })

        // Check allows
        utils = Utils(this)
        checkAllows()
        // Protect Power Manager
        utils!!.protectAppManager()

        // Sound Mode
        soundMode = findViewById(R.id.soundMode)
        getSoundMode()
        soundMode.setOnClickListener(View.OnClickListener {
            val utils = Utils(applicationContext)
            if (checkAllows()) {
                utils.setSoundMode()
                soundMode.visibility = View.VISIBLE
            } else {
                soundMode.visibility = View.GONE
            }
        })

        // Settings
        mSettings = getSharedPreferences(APP_PREFERENCES_NAME, MODE_PRIVATE)

        // Dimmer
        dimmerSwitch.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
            dimmerOn = isChecked
            saveSettings(APP_PREFERENCES_DIMMER_ON, dimmerOn)
            overlayService()
        })

        // Timer
        timerSwitch.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
            if (checkAllows()) {
                timerOn = isChecked
                saveSettings(APP_PREFERENCES_TIMER_ON, timerOn)
                timerService()
            }
        })

        // Receiver
        val filter = IntentFilter()
        filter.addAction(BReceiver.APP_OVERLAY_ON)
        filter.addAction(BReceiver.APP_OVERLAY_OFF)
        filter.addAction(OverlayService.CLOSE_ACTION)
        filter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
        filter.addAction(CHECK_AUDIO_PERMISSION)
        registerReceiver(broadcastReceiver, filter)

        // DimmerColor control
        dimmerColor.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, b: Boolean) {
                dimmerColorValue = progress
                dimmerColorStatus.text = "${(dimmerColorValue / 2.55).toInt()}%"
                overlayService()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Save
                saveSettings(APP_PREFERENCES_DIMMER_COLOR, dimmerColorValue)
                // Apply
                overlayService()
            }
        })

        // Dimmer control
        dimmer.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, b: Boolean) {
                dimmerValue = progress
                dimmerStatus.text = "${(dimmerValue / 2.55).toInt()}%"
                overlayService()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Save
                saveSettings(APP_PREFERENCES_DIMMER, dimmerValue)
                // Apply
                overlayService()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        loadSettings()
    }

    // Load values from save
    private fun overlayService() {
        if (!first) {
            if (dimmerColorValue == 0 && dimmerValue == 0 || !dimmerOn) {
                overlayOff()
            } else if (checkAllows()) {
                overlayOn()
            }
        }
    }

    private fun overlayOff() {
        val intent = Intent(this, OverlayService::class.java)
        intent.action = "overlayOff"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun overlayOn() {
        val intent = Intent(this, OverlayService::class.java)
        intent.action = "overlayOn"
        intent.putExtra("theme", theme)
        intent.putExtra("dimmerColorValue", dimmerColorValue)
        intent.putExtra("dimmerValue", dimmerValue)
        intent.putExtra("temperature", temperature)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    // Save values
    private fun saveSettings(key: String, value: Int) {
        // Prepare for save
        val editor = mSettings!!.edit()
        // Edit Variables
        editor.putInt(key, value)
        // Save
        editor.apply()
    }

    private fun saveSettings(key: String, value: String) {
        // Prepare for save
        val editor = mSettings!!.edit()
        // Edit Variables
        editor.putString(key, value)
        // Save
        editor.apply()
    }

    private fun saveSettings(key: String, value: Boolean) {
        // Prepare for save
        val editor = mSettings!!.edit()
        // Edit Variables
        editor.putBoolean(key, value)
        // Save
        editor.apply()
    }

    // [END select timer]
    private fun clearSetting(key: String) {
        // Prepare for save
        val editor = mSettings!!.edit()
        // Edit Variables
        editor.remove(key)
        // Save
        editor.apply()
    }

    private fun showUISettings() {
        UISettings.visibility = View.VISIBLE
        // Animation alpha
        UISettings.animate().alpha(1.0f).duration = 150

        // ADS
        /*
        if (charity_icon) {
            //showAd();
        }
        */
    }

    private fun hideUISettings() {
        // Animation alpha
        UISettings.animate().alpha(0.0f).setDuration(150).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                // Visibility
                UISettings.visibility = View.GONE
                // Clear listener
                UISettings.animate().setListener(null)
                //hideAd();
            }
        })
    }

    private fun showAd() {
        val adRequest = AdRequest.Builder().addTestDevice("1D5A1AEA8E6CA40D5189183547904B82").addTestDevice("3EC30EB95D85614AD55C26E956492D9E").build()
        mAdView = findViewById(R.id.adView)
        mAdView.loadAd(adRequest)
        mAdView.visibility = View.VISIBLE
        mAdView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                // Code to be executed when an ad finishes loading.
                Log.e(TAG, "loaded")
            }

            override fun onAdFailedToLoad(errorCode: Int) {
                // Code to be executed when an ad request fails.
                Log.e(TAG, "failed: $errorCode")
            }

            override fun onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
                Log.e(TAG, "opened")
            }

            override fun onAdLeftApplication() {
                // Code to be executed when the user has left the app.
                Log.e(TAG, "left app")
            }

            override fun onAdClosed() {
                // Code to be executed when when the user is about to return
                // to the app after tapping on an ad.
                Log.e(TAG, "closed")
            }
        }
    }

    fun hideAd() {
        mAdView.destroy()
        mAdView.visibility = View.GONE
    }

    override fun onClick(v: View) {
        val intent: Intent
        when (v.id) {
            R.id.Wrapper -> closeActivity()
            R.id.Wrapper1 -> closeActivity()
            R.id.settingsButton -> {
                openUISettings = if (UISettings.visibility == View.GONE) {
                    // Show
                    showUISettings()
                    true
                } else {
                    // Hide
                    hideUISettings()
                    false
                }
                saveSettings(APP_PREFERENCES_OPEN_SETTINGS, openUISettings)
            }
            R.id.temperature1 -> {
                saveSettings(APP_PREFERENCES_TEMPERATURE, 1)
                temperature = 1
                overlayService()
            }
            R.id.temperature2 -> {
                saveSettings(APP_PREFERENCES_TEMPERATURE, 2)
                temperature = 2
                overlayService()
            }
            R.id.temperature3 -> {
                saveSettings(APP_PREFERENCES_TEMPERATURE, 3)
                temperature = 3
                overlayService()
            }
            R.id.temperature4 -> {
                saveSettings(APP_PREFERENCES_TEMPERATURE, 4)
                temperature = 4
                overlayService()
            }
            R.id.temperature5 -> {
                saveSettings(APP_PREFERENCES_TEMPERATURE, 5)
                temperature = 5
                overlayService()
            }
            R.id.darkTheme -> if (theme != "dark") {
                theme = "dark"
                saveSettings(APP_PREFERENCES_THEME, theme!!)
                if (dimmerSwitch.isChecked) {
                    intent = Intent(this, OverlayService::class.java)
                    intent.action = "theme"
                    intent.putExtra("theme", theme)
                    if (checkAllows()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(intent)
                        } else {
                            startService(intent)
                        }
                    }
                }
            }
            R.id.transparentTheme -> if (theme != "transparent") {
                theme = "transparent"
                saveSettings(APP_PREFERENCES_THEME, theme!!)
                if (dimmerSwitch.isChecked) {
                    intent = Intent(this, OverlayService::class.java)
                    intent.action = "theme"
                    intent.putExtra("theme", theme)
                    if (checkAllows()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(intent)
                        } else {
                            startService(intent)
                        }
                    }
                }
            }
            R.id.timerTimeOn -> pickTimer("timerOn")
            R.id.timerTimeOff -> pickTimer("timerOff")
            R.id.policyButton -> {
                val intentPolicyButton = Intent()
                intentPolicyButton.action = Intent.ACTION_VIEW
                intentPolicyButton.addCategory(Intent.CATEGORY_BROWSABLE)
                intentPolicyButton.data = Uri.parse("https://sites.google.com/view/ilushling/night-light-filter")
                startActivity(intentPolicyButton)
            }
            R.id.policyText -> {
                val intentPolicyText = Intent()
                intentPolicyText.action = Intent.ACTION_VIEW
                intentPolicyText.addCategory(Intent.CATEGORY_BROWSABLE)
                intentPolicyText.data = Uri.parse("https://sites.google.com/view/ilushling/night-light-filter")
                startActivity(intentPolicyText)
            }
        }
    }

    /**
     * 1 check value for contains in setting file [ELSE] save default value
     * 2 check value for valid value [ELSE] if invalid remove value and save default value
     */
    private fun loadSettings() {
        // Theme
        if (mSettings!!.contains(APP_PREFERENCES_THEME)) {
            try {
                theme = mSettings!!.getString(APP_PREFERENCES_THEME, "dark")
                when (theme) {
                    "dark" -> darkTheme.isChecked = true
                    "transparent" -> transparentTheme.isChecked = true
                    else -> {
                        darkTheme.isChecked = true
                        clearSetting(APP_PREFERENCES_THEME)
                        saveSettings(APP_PREFERENCES_THEME, "dark")
                    }
                }
            } catch (exc: Exception) {
                clearSetting(APP_PREFERENCES_THEME)
                saveSettings(APP_PREFERENCES_THEME, "dark")
            }
        } else {
            saveSettings(APP_PREFERENCES_THEME, "dark")
            theme = mSettings!!.getString(APP_PREFERENCES_THEME, "dark")
        }

        // [START Overlay]
        // Dimmer on
        if (mSettings!!.contains(APP_PREFERENCES_DIMMER_ON)) {
            try {
                dimmerOn = mSettings!!.getBoolean(APP_PREFERENCES_DIMMER_ON, false)
            } catch (exc: Exception) {
                clearSetting(APP_PREFERENCES_DIMMER_ON)
                saveSettings(APP_PREFERENCES_DIMMER_ON, false)
            }
        } else {
            saveSettings(APP_PREFERENCES_DIMMER_ON, false)
            dimmerOn = mSettings!!.getBoolean(APP_PREFERENCES_DIMMER_ON, false)
        }

        // DimmerColor
        if (mSettings!!.contains(APP_PREFERENCES_DIMMER_COLOR)) {
            try {
                dimmerColor.progress = mSettings!!.getInt(APP_PREFERENCES_DIMMER_COLOR, 0)
            } catch (exc: Exception) {
                clearSetting(APP_PREFERENCES_DIMMER_COLOR)
                saveSettings(APP_PREFERENCES_DIMMER_COLOR, 0)
            }
        } else {
            saveSettings(APP_PREFERENCES_DIMMER_COLOR, 0)
        }
        // Dimmer
        if (mSettings!!.contains(APP_PREFERENCES_DIMMER)) {
            try {
                dimmer.progress = mSettings!!.getInt(APP_PREFERENCES_DIMMER, 0)
            } catch (exc: Exception) {
                clearSetting(APP_PREFERENCES_DIMMER)
                saveSettings(APP_PREFERENCES_DIMMER, 0)
            }
        } else {
            saveSettings(APP_PREFERENCES_DIMMER, 0)
        }
        // Temperature
        if (mSettings!!.contains(APP_PREFERENCES_TEMPERATURE)) {
            try {
                temperature = mSettings!!.getInt(APP_PREFERENCES_TEMPERATURE, 4)
                when (temperature) {
                    1 -> temperature1RB.isChecked = true
                    2 -> temperature2RB.isChecked = true
                    3 -> temperature3RB.isChecked = true
                    4 -> temperature4RB.isChecked = true
                    5 -> temperature5RB.isChecked = true
                    else -> temperature4RB.isChecked = true
                }
            } catch (exc: Exception) {
                clearSetting(APP_PREFERENCES_TEMPERATURE)
                saveSettings(APP_PREFERENCES_TEMPERATURE, 4)
            }
        } else {
            saveSettings(APP_PREFERENCES_TEMPERATURE, 4)
        }
        // [END Overlay]

        // [START Timer]
        // Timer on
        timerOn = if (mSettings!!.contains(APP_PREFERENCES_TIMER_ON)) {
            try {
                mSettings!!.getBoolean(APP_PREFERENCES_TIMER_ON, false)
            } catch (exc: Exception) {
                clearSetting(APP_PREFERENCES_TIMER_ON)
                saveSettings(APP_PREFERENCES_TIMER_ON, false)
                mSettings!!.getBoolean(APP_PREFERENCES_TIMER_ON, false)
            }
        } else {
            saveSettings(APP_PREFERENCES_TIMER_ON, false)
            mSettings!!.getBoolean(APP_PREFERENCES_TIMER_ON, false)
        }

        // TIME ON
        // HOUR ON
        timerHourOn = if (mSettings!!.contains(APP_PREFERENCES_TIMER_HOUR_ON)) {
            try {
                mSettings!!.getString(APP_PREFERENCES_TIMER_HOUR_ON, "22")
            } catch (exc: Exception) {
                clearSetting(APP_PREFERENCES_TIMER_HOUR_ON)
                saveSettings(APP_PREFERENCES_TIMER_HOUR_ON, "22")
                mSettings!!.getString(APP_PREFERENCES_TIMER_HOUR_ON, "22")
            }
        } else {
            saveSettings(APP_PREFERENCES_TIMER_HOUR_ON, "22")
            mSettings!!.getString(APP_PREFERENCES_TIMER_HOUR_ON, "22")
        }
        // MINUTE ON
        timerMinuteOn = if (mSettings!!.contains(APP_PREFERENCES_TIMER_MINUTE_ON)) {
            try {
                mSettings!!.getString(APP_PREFERENCES_TIMER_MINUTE_ON, "0")
            } catch (exc: Exception) {
                clearSetting(APP_PREFERENCES_TIMER_MINUTE_ON)
                saveSettings(APP_PREFERENCES_TIMER_MINUTE_ON, "0")
                mSettings!!.getString(APP_PREFERENCES_TIMER_MINUTE_ON, "0")
            }
        } else {
            saveSettings(APP_PREFERENCES_TIMER_MINUTE_ON, "0")
            mSettings!!.getString(APP_PREFERENCES_TIMER_MINUTE_ON, "0")
        }

        // TIME OFF
        // HOUR OFF
        timerHourOff = if (mSettings!!.contains(APP_PREFERENCES_TIMER_HOUR_OFF)) {
            try {
                mSettings!!.getString(APP_PREFERENCES_TIMER_HOUR_OFF, "7")
            } catch (exc: Exception) {
                clearSetting(APP_PREFERENCES_TIMER_HOUR_OFF)
                saveSettings(APP_PREFERENCES_TIMER_HOUR_OFF, "7")
                mSettings!!.getString(APP_PREFERENCES_TIMER_HOUR_OFF, "7")
            }
        } else {
            saveSettings(APP_PREFERENCES_TIMER_HOUR_OFF, "7")
            mSettings!!.getString(APP_PREFERENCES_TIMER_HOUR_OFF, "7")
        }
        // MINUTE OFF
        timerMinuteOff = if (mSettings!!.contains(APP_PREFERENCES_TIMER_MINUTE_OFF)) {
            try {
                mSettings!!.getString(APP_PREFERENCES_TIMER_MINUTE_OFF, "0")
            } catch (exc: Exception) {
                clearSetting(APP_PREFERENCES_TIMER_MINUTE_OFF)
                saveSettings(APP_PREFERENCES_TIMER_MINUTE_OFF, "0")
                mSettings!!.getString(APP_PREFERENCES_TIMER_MINUTE_OFF, "0")
            }
        } else {
            saveSettings(APP_PREFERENCES_TIMER_MINUTE_OFF, "0")
            mSettings!!.getString(APP_PREFERENCES_TIMER_MINUTE_OFF, "0")
        }
        // [END Timer]

        // Open Settings
        if (mSettings!!.contains(APP_PREFERENCES_OPEN_SETTINGS)) {
            try {
                openUISettings = mSettings!!.getBoolean(APP_PREFERENCES_OPEN_SETTINGS, false)
                if (openUISettings) {
                    showUISettings()

                    //showAd();
                } else {
                    hideUISettings()

                    //hideAd();
                }
            } catch (exc: Exception) {
                clearSetting(APP_PREFERENCES_OPEN_SETTINGS)
                saveSettings(APP_PREFERENCES_OPEN_SETTINGS, false)
            }
        } else {
            saveSettings(APP_PREFERENCES_OPEN_SETTINGS, false)
        }

        // Charity
        if (mSettings!!.contains(APP_PREFERENCES_CHARITY)) {
            try {
                charity = mSettings!!.getBoolean(APP_PREFERENCES_CHARITY, false)
                charityCB.isChecked = charity
            } catch (exc: Exception) {
                clearSetting(APP_PREFERENCES_TIMER_MINUTE_OFF)
                saveSettings(APP_PREFERENCES_TIMER_MINUTE_OFF, false)
                charityCB.isChecked = charity
            }
        } else {
            saveSettings(APP_PREFERENCES_CHARITY, false)
            charityCB.isChecked = charity
        }

        // [START format minutes to 00]
        // ON
        var sb = StringBuilder()
        sb.append(timerHourOn)
        if (sb.length < 2) {
            sb.insert(0, '0') // pad with leading zero if needed
        }
        timerHourOn = sb.toString()
        sb = StringBuilder()
        sb.append(timerMinuteOn)
        if (sb.length < 2) {
            sb.insert(0, '0') // pad with leading zero if needed
        }
        timerMinuteOn = sb.toString()
        // OFF
        sb = StringBuilder()
        sb.append(timerHourOff)
        if (sb.length < 2) {
            sb.insert(0, '0') // pad with leading zero if needed
        }
        timerHourOff = sb.toString()
        sb = StringBuilder()
        sb.append(timerMinuteOff)
        if (sb.length < 2) {
            sb.insert(0, '0') // pad with leading zero if needed
        }
        timerMinuteOff = sb.toString()

        // [END format minutes to 00]
        timerTimeOn.text = "ON $timerHourOn:$timerMinuteOn"
        timerTimeOff.text = "OFF $timerHourOff:$timerMinuteOff"


        // Update UI
        /**
         * Switchers need after loading because they listeners triggering before load
         */
        // Switcher
        dimmerSwitch.isChecked = dimmerOn
        // Timer
        timerSwitch.isChecked = timerOn
        /**
         * if seekbars was changed it call overlayService there are 3 seekbars so need only once and after load
         */
        first = false
        overlayService()
    }

    // Sound Mode
    fun getSoundMode() {
        val utils = Utils(this)
        when (utils.soundMode) {
            0 -> {
                soundMode.visibility = View.VISIBLE
                soundMode.setImageResource(R.drawable.sound_off)
            }
            1 -> {
                soundMode.visibility = View.VISIBLE
                soundMode.setImageResource(R.drawable.sound_vibration)
            }
            2 -> {
                soundMode.visibility = View.VISIBLE
                soundMode.setImageResource(R.drawable.sound_on)
            }
            else -> soundMode.visibility = View.GONE
        }
    }

    private fun permitted() {
        dimmerSwitch.isEnabled = true
        settingsButton.isEnabled = true
        dimmerColor.isEnabled = true
        dimmer.isEnabled = true
        timerSwitch.isEnabled = true
        //timerTimeOn.setEnabled(true);
        //timerTimeOff.setEnabled(true);
    }

    private fun resricted() {
        //dimmerSwitch.setEnabled(false);
        dimmerSwitch.isChecked = false
        settingsButton.isEnabled = false
        hideUISettings()
        dimmerColor.isEnabled = false
        dimmer.isEnabled = false
        //timerSwitch.setEnabled(false);
        timerSwitch.isChecked = false
        //timerTimeOn.setEnabled(false);
        //timerTimeOff.setEnabled(false);
    }

    // [START Permissions]
    private fun checkAllows(): Boolean {
        /**
         * for Android 6 and below need to check permissions
         * 1 check version SDK (>= M)
         * 2 check Permission if don't have need request them (Dialog)
         * 3 if don't work dialog run alternative via settings (Activity result)
         */
        return if (checkPermissionOverlay()) {
            permitted()
            // CLOSE_SYSTEM_DIALOGS
            val it = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            sendBroadcast(it)
            true
        } else {
            resricted()
            false
        }
    }

    private fun checkPermissionOverlay(): Boolean {
        // via settings
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Overlay
            if (!Settings.canDrawOverlays(this)) {
                showPermission("overlay", getString(R.string.restriction_overlay))
                false
            } else {
                true
            }
        } else true
    }

    // [START select timer]
    private fun pickTimer(action: String?) {
        var hour = 0
        var minute = 0
        when (action) {
            "timerOn" -> {
                hour = timerHourOn!!.toInt()
                minute = timerMinuteOn!!.toInt()
            }
            "timerOff" -> {
                hour = timerHourOff!!.toInt()
                minute = timerMinuteOff!!.toInt()
            }
        }
        val timePickerDialog = TimePickerDialog(this@MainActivity, TimePickerDialog.THEME_DEVICE_DEFAULT_DARK, { view, hour, minute ->
            when (action) {
                "timerOn" -> {
                    timerHourOn = hour.toString()
                    timerMinuteOn = minute.toString()
                    saveSettings(APP_PREFERENCES_TIMER_HOUR_ON, timerHourOn!!)
                    saveSettings(APP_PREFERENCES_TIMER_MINUTE_ON, timerMinuteOn!!)

                    // format minutes to 00
                    val sb = StringBuilder()
                    sb.append(timerMinuteOn)
                    if (sb.length < 2) {
                        sb.insert(0, '0') // pad with leading zero if needed
                    }
                    timerMinuteOn = sb.toString()

                    // Update UI
                    timerTimeOn.text = "ON $timerHourOn:$timerMinuteOn"
                }
                "timerOff" -> {
                    timerHourOff = hour.toString()
                    timerMinuteOff = minute.toString()
                    saveSettings(APP_PREFERENCES_TIMER_HOUR_OFF, timerHourOff!!)
                    saveSettings(APP_PREFERENCES_TIMER_MINUTE_OFF, timerMinuteOff!!)

                    // format minutes to 00
                    val sb = StringBuilder()
                    sb.append(timerMinuteOff)
                    if (sb.length < 2) {
                        sb.insert(0, '0') // pad with leading zero if needed
                    }
                    timerMinuteOff = sb.toString()
                    timerTimeOff.text = "OFF $timerHourOff:$timerMinuteOff"
                }
            }
            timerService()
        }, hour, minute, true)
        timePickerDialog.show()
    }

    // Run Alarm in service
    private fun timerService() {
        val intent = Intent(this, OverlayService::class.java)
        if (timerOn && timerHourOn != null && timerMinuteOn != null && timerHourOff != null && timerMinuteOff != null) {
            // Timer on
            intent.action = "timerOn"
            intent.putExtra("timerHourOn", timerHourOn)
            intent.putExtra("timerMinuteOn", timerMinuteOn)
            intent.putExtra("timerHourOff", timerHourOff)
            intent.putExtra("timerMinuteOff", timerMinuteOff)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } else if (!timerOn) {
            // Timer off
            intent.action = "timerOff"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    private fun fetchRemoteConfig() {
        // cache expiration in seconds
        var cacheExpiration = 30
        //expire the cache immediately for development mode.
        if (mFirebaseRemoteConfig!!.info.configSettings.isDeveloperModeEnabled) {
            cacheExpiration = 0
        }
        mFirebaseRemoteConfig!!.fetch(cacheExpiration.toLong())
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // After config data is successfully fetched, it must be activated before newly fetched
                        // values are returned.
                        mFirebaseRemoteConfig!!.activateFetched()
                    }
                }
    }

    // Dialog for permission
    fun showPermission(permission: String?, message: String?) {
        when (permission) {
            "overlay" -> AlertDialog.Builder(this)
                    .setMessage(message)
                    .setPositiveButton("OK", listenerOverlay)
                    .setNegativeButton("Cancel", listenerOverlay)
                    .create()
                    .show()
            "sound" -> AlertDialog.Builder(this)
                    .setMessage(message)
                    .setPositiveButton("OK", listenerSound)
                    .setNegativeButton("Cancel", listenerSound)
                    .create()
                    .show()
        }
    }

    // Alternative permission
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == 1) {
            // ** if so check once again if we have permission */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    // continue here - permission was granted
                    permitted()
                    Log.e(TAG, "Granted 1")
                }
            }
        }
        if (requestCode == 2) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // continue here - permission was granted
                Log.e(TAG, "Granted 2")
            }
        }
        if (requestCode == 3) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // continue here - permission was granted
                Log.e(TAG, "Granted 3")
            }
        }
    }

    // [END Permissions]
    private fun closeActivity() {
        Wrapper.animate().alpha(0.0f).setDuration(150).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                finish()
            }
        })
    }

    override fun onPause() {
        super.onPause()
        Wrapper.animate().alpha(0.0f).duration = 150
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter()
        filter.addAction(OverlayService.CLOSE_ACTION)
        registerReceiver(broadcastReceiver, filter)
        Wrapper.animate().alpha(1.0f).duration = 150
    }

    override fun onStop() {
        super.onStop()
        Wrapper.animate().alpha(0.0f).duration = 150
    }

    override fun onDestroy() {
        super.onDestroy()
        val bundle = Bundle()
        bundle.putInt("dimmer", dimmerValue)
        bundle.putInt("dimmerColor", dimmerColorValue)
        bundle.putBoolean("timerOn", timerOn)
        bundle.putString("timerTimeOn", "$timerHourOn:$timerMinuteOn")
        bundle.putString("timerTimeOff", "$timerHourOff:$timerMinuteOff")
        bundle.putInt("temperature", temperature)
        bundle.putString("theme", theme)
        mFirebaseAnalytics!!.logEvent("settings", bundle)
        try {
            // Free receiver
            unregisterReceiver(broadcastReceiver)
            // Kill process
        } catch (exc: Exception) {
            Log.e(TAG, "onDestroy: $exc")
        }
    }

    companion object {
        // Common
        const val TAG = "MainActivity"

        // Timer
        const val APP_PREFERENCES_TIMER_ON = "timerOn"
        const val APP_PREFERENCES_TIMER_HOUR_ON = "timerHourOn"
        const val APP_PREFERENCES_TIMER_MINUTE_ON = "timerMinuteOn"
        const val APP_PREFERENCES_TIMER_HOUR_OFF = "timerHourOff"
        const val APP_PREFERENCES_TIMER_MINUTE_OFF = "timerMinuteOff"
        const val CHECK_AUDIO_PERMISSION = "CHECK_AUDIO_PERMISSION"

        // Dimmer
        const val APP_PREFERENCES_DIMMER_ON = "dimmerOn"
        const val APP_PREFERENCES_DIMMER_COLOR = "dimmerColor"
        const val APP_PREFERENCES_DIMMER = "dimmer"
        const val APP_PREFERENCES_TEMPERATURE = "temperature"
        const val APP_PREFERENCES_CHARITY = "charity_icon"
        const val APP_PREFERENCES_OPEN_SETTINGS = "openSettings"
        const val APP_PREFERENCES_THEME = "theme"

        // Name setting file
        const val APP_PREFERENCES_NAME = "PREFERENCE_FILE"
    }
}
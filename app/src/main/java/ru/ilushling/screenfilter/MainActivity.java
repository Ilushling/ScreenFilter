package ru.ilushling.screenfilter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import static ru.ilushling.screenfilter.BReceiver.APP_OVERLAY_OFF;
import static ru.ilushling.screenfilter.BReceiver.APP_OVERLAY_ON;
import static ru.ilushling.screenfilter.OverlayService.CLOSE_ACTION;


public class MainActivity extends Activity implements View.OnClickListener {

    // Common
    static final String TAG = "MainActivity";
    // Timer
    public static final String APP_PREFERENCES_TIMER_ON = "timerOn",
            APP_PREFERENCES_TIMER_HOUR_ON = "timerHourOn", APP_PREFERENCES_TIMER_MINUTE_ON = "timerMinuteOn",
            APP_PREFERENCES_TIMER_HOUR_OFF = "timerHourOff", APP_PREFERENCES_TIMER_MINUTE_OFF = "timerMinuteOff", CHECK_AUDIO_PERMISSION = "CHECK_AUDIO_PERMISSION";

    // Dimmer
    public static final String APP_PREFERENCES_DIMMER_ON = "dimmerOn", APP_PREFERENCES_DIMMER_COLOR = "dimmerColor",
            APP_PREFERENCES_DIMMER = "dimmer", APP_PREFERENCES_TEMPERATURE = "temperature", APP_PREFERENCES_CHARITY = "charity_icon", APP_PREFERENCES_OPEN_SETTINGS = "openSettings";
    boolean first = true, charity, openUISettings;
    CheckBox charity_cb;
    public static final String APP_PREFERENCES_THEME = "theme";
    protected TextView timerTimeOn, timerTimeOff;
    SeekBar dimmer, dimmerColor;
    // UI
    ConstraintLayout UIMain, Wrapper, Wrapper1, UISettings;
    Utils utils;
    TextView dimmerColor_status, dimmer_status;
    private AdView mAdView;

    // Policy
    ImageButton policyButton;
    TextView policyText;

    // Save Settings
    SharedPreferences mSettings;
    private FirebaseAnalytics mFirebaseAnalytics;
    // Listener for dialog
    public DialogInterface.OnClickListener listenerOverlay = new DialogInterface.OnClickListener() {

        final int BUTTON_NEGATIVE = -2;
        final int BUTTON_POSITIVE = -1;

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case BUTTON_NEGATIVE:
                    // int which = -2
                    dialog.dismiss();
                    break;
                case BUTTON_POSITIVE:
                    // int which = -1
                    Intent intent = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                    }
                    startActivityForResult(intent, 1);
                    dialog.dismiss();
                    break;
            }
        }
    };
    // Name setting file
    public static final String APP_PREFERENCES_NAME = "PREFERENCE_FILE";
    ImageButton settingsButton, soundMode;
    Switch dimmerSwitch;
    // Variables
    String theme;
    private int dimmerColorValue, dimmerValue;
    boolean dimmerOn, timerOn;
    int temperature;
    protected String timerHourOn, timerMinuteOn, timerHourOff, timerMinuteOff;
    Switch timerSwitch;
    // Listener for dialog
    public DialogInterface.OnClickListener listenerSound = new DialogInterface.OnClickListener() {

        final int BUTTON_NEGATIVE = -2;
        final int BUTTON_POSITIVE = -1;

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case BUTTON_NEGATIVE:
                    // int which = -2
                    dialog.dismiss();
                    break;
                case BUTTON_POSITIVE:
                    // int which = -1
                    Intent intent = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                    }
                    startActivityForResult(intent, 2);
                    dialog.dismiss();
                    break;
            }
        }
    };
    //RadioButton lightTheme;
    RadioButton temperature1_rb, temperature2_rb, temperature3_rb, temperature4_rb, temperature5_rb, darkTheme, transparentTheme;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    // Close
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Log.e(TAG, intent.getAction());

            switch (intent.getAction() != null ? intent.getAction() : "") {
                case APP_OVERLAY_ON:
                    dimmerSwitch.setChecked(true);
                    break;
                case APP_OVERLAY_OFF:
                    dimmerSwitch.setChecked(false);
                    break;
                case CLOSE_ACTION:
                    dimmerSwitch.setChecked(false);
                    break;
                case AudioManager.RINGER_MODE_CHANGED_ACTION:
                    getSoundMode();
                    break;
                case CHECK_AUDIO_PERMISSION:
                    showPermission("sound", context.getString(R.string.restriction_sound));
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mAdView = findViewById(R.id.adView);
        MobileAds.initialize(this, getString(R.string.ad_app_id));

        Bundle bundle = new Bundle();
        mFirebaseAnalytics.logEvent("open_app", bundle);

        // ADS
        // Remote settings
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings remoteConfigSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(false)
                .build();
        mFirebaseRemoteConfig.setConfigSettings(remoteConfigSettings);
        mFirebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults);
        fetchRemoteConfig();
        boolean enableAd = mFirebaseRemoteConfig.getBoolean("enableAd");

        if (enableAd) {
            showAd();
        }
        //Log.e(TAG, "enableAd: " + enableAd);

        temperature1_rb = findViewById(R.id.temperature1);
        temperature1_rb.setOnClickListener(this);
        temperature2_rb = findViewById(R.id.temperature2);
        temperature2_rb.setOnClickListener(this);
        temperature3_rb = findViewById(R.id.temperature3);
        temperature3_rb.setOnClickListener(this);
        temperature4_rb = findViewById(R.id.temperature4);
        temperature4_rb.setOnClickListener(this);
        temperature5_rb = findViewById(R.id.temperature5);
        temperature5_rb.setOnClickListener(this);

        GradientDrawable gd = (GradientDrawable) temperature1_rb.getBackground();
        gd.setColor(Color.rgb(255, 50, 20));
        GradientDrawable gd2 = (GradientDrawable) temperature2_rb.getBackground();
        gd2.setColor(Color.rgb(255, 90, 40));
        GradientDrawable gd3 = (GradientDrawable) temperature3_rb.getBackground();
        gd3.setColor(Color.rgb(255, 110, 50));
        GradientDrawable gd4 = (GradientDrawable) temperature4_rb.getBackground();
        gd4.setColor(Color.rgb(255, 135, 60));
        GradientDrawable gd5 = (GradientDrawable) temperature5_rb.getBackground();
        gd5.setColor(Color.rgb(255, 160, 75));

        // UI
        Wrapper = findViewById(R.id.Wrapper);
        Wrapper.setOnClickListener(this);
        Wrapper1 = findViewById(R.id.Wrapper1);
        Wrapper1.setOnClickListener(this);

        UIMain = findViewById(R.id.UIMain);
        UIMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        UISettings = findViewById(R.id.UISettings);
        UISettings.setVisibility(View.GONE);
        UISettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(this);
        // Dimmer
        dimmerSwitch = findViewById(R.id.dimmerSwitch);
        dimmerColor = findViewById(R.id.dimmerColorSeekbar);
        dimmerColor_status = findViewById(R.id.dimmerColorStatus);
        dimmer = findViewById(R.id.dimmerSeekbar);
        dimmer_status = findViewById(R.id.dimmerStatus);
        // Timer
        timerTimeOn = findViewById(R.id.timerTimeOn);
        timerTimeOn.setOnClickListener(this);
        timerTimeOff = findViewById(R.id.timerTimeOff);
        timerTimeOff.setOnClickListener(this);
        timerSwitch = findViewById(R.id.timerSwitch);

        // Theme
        darkTheme = findViewById(R.id.darkTheme);
        darkTheme.setOnClickListener(this);
        /*
        lightTheme = findViewById(R.id.lightTheme);
        lightTheme.setOnClickListener(this);
        */
        transparentTheme = findViewById(R.id.transparentTheme);
        transparentTheme.setOnClickListener(this);

        // Policy
        policyButton = findViewById(R.id.policyButton);
        policyButton.setOnClickListener(this);
        policyText = findViewById(R.id.policyText);
        policyText.setOnClickListener(this);

        // Charity
        charity_cb = findViewById(R.id.charity);
        charity_cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                charity = isChecked;
                // Analytics
                boolean charityLoad = charity;
                if (mSettings.contains(APP_PREFERENCES_CHARITY)) {
                    try {
                        charityLoad = mSettings.getBoolean(APP_PREFERENCES_CHARITY, false);
                    } catch (Exception e) {
                    }
                }

                if (!isChecked) {
                    //hideAd();

                    if (charityLoad != charity) {
                        Bundle bundle = new Bundle();
                        bundle.putString("Charity", "user activate thanks");
                        mFirebaseAnalytics.logEvent("activate_charity", bundle);
                    }
                } else {
                    //showAd();

                    if (charityLoad != charity) {
                        Bundle bundle = new Bundle();
                        bundle.putString("Charity", "user deactivate app");
                        mFirebaseAnalytics.logEvent("deactivate_charity", bundle);
                    }
                }

                saveSettings(APP_PREFERENCES_CHARITY, charity);
            }
        });

        // Check allows
        utils = new Utils(this);
        checkAllows();
        // Protect Power Manager
        utils.protectAppManager();

        // Sound Mode
        soundMode = findViewById(R.id.soundMode);
        getSoundMode();
        soundMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils utils = new Utils(getApplicationContext());

                if (checkAllows()) {
                    utils.setSoundMode();
                    soundMode.setVisibility(View.VISIBLE);
                } else {
                    soundMode.setVisibility(View.GONE);
                }

            }
        });

        // Settings
        mSettings = getSharedPreferences(APP_PREFERENCES_NAME, Context.MODE_PRIVATE);

        // Dimmer
        dimmerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                dimmerOn = isChecked;
                saveSettings(APP_PREFERENCES_DIMMER_ON, dimmerOn);
                overlayService();
            }
        });

        // Timer
        timerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (checkAllows()) {
                    timerOn = isChecked;
                    saveSettings(APP_PREFERENCES_TIMER_ON, timerOn);
                    timerService();
                }
            }

        });

        // Receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(APP_OVERLAY_ON);
        filter.addAction(APP_OVERLAY_OFF);
        filter.addAction(CLOSE_ACTION);
        filter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
        filter.addAction(CHECK_AUDIO_PERMISSION);
        registerReceiver(broadcastReceiver, filter);

        // DimmerColor control
        dimmerColor.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                dimmerColorValue = progress;
                dimmerColor_status.setText((int) (dimmerColorValue / 2.55) + "%");
                overlayService();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Save
                saveSettings(APP_PREFERENCES_DIMMER_COLOR, dimmerColorValue);
                // Apply
                overlayService();
            }
        });

        // Dimmer control
        dimmer.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                dimmerValue = progress;
                dimmer_status.setText((int) (dimmerValue / 2.55) + "%");
                overlayService();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Save
                saveSettings(APP_PREFERENCES_DIMMER, dimmerValue);
                // Apply
                overlayService();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        loadSettings();
    }

    // Load values from save

    private void overlayService() {
        if (!first) {
            if (dimmerColorValue == 0 && dimmerValue == 0 || !dimmerOn) {
                overlayOff();
            } else if (checkAllows()) {
                overlayOn();
            }
        }
    }

    void overlayOff() {
        Intent intent = new Intent(this, OverlayService.class);
        intent.setAction("overlayOff");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    void overlayOn() {
        Intent intent = new Intent(this, OverlayService.class);
        intent.setAction("overlayOn");
        intent.putExtra("theme", theme);
        intent.putExtra("dimmerColorValue", dimmerColorValue);
        intent.putExtra("dimmerValue", dimmerValue);
        intent.putExtra("temperature", temperature);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    // Save values
    private void saveSettings(String key, int value) {
        // Prepare for save
        SharedPreferences.Editor editor = mSettings.edit();
        // Edit Variables
        editor.putInt(key, value);
        // Save
        editor.apply();
    }

    private void saveSettings(String key, String value) {
        // Prepare for save
        SharedPreferences.Editor editor = mSettings.edit();
        // Edit Variables
        editor.putString(key, value);
        // Save
        editor.apply();
    }

    private void saveSettings(String key, boolean value) {
        // Prepare for save
        SharedPreferences.Editor editor = mSettings.edit();
        // Edit Variables
        editor.putBoolean(key, value);
        // Save
        editor.apply();
    }
    // [END select timer]

    private void clearSetting(String key) {
        // Prepare for save
        SharedPreferences.Editor editor = mSettings.edit();
        // Edit Variables
        editor.remove(key);
        // Save
        editor.apply();
    }

    void showUISettings() {
        UISettings.setVisibility(View.VISIBLE);
        // Animation alpha
        UISettings.animate().alpha(1.0f).setDuration(150);

        // ADS
        /*
        if (charity_icon) {
            //showAd();
        }
        */
    }

    void hideUISettings() {
        // Animation alpha
        UISettings.animate().alpha(0.0f).setDuration(150).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                // Visibility
                UISettings.setVisibility(View.GONE);
                // Clear listener
                UISettings.animate().setListener(null);
                //hideAd();
            }
        });
    }

    void showAd() {
        AdRequest adRequest = new AdRequest.Builder().addTestDevice("1D5A1AEA8E6CA40D5189183547904B82").addTestDevice("3EC30EB95D85614AD55C26E956492D9E").build();
        mAdView = findViewById(R.id.adView);
        mAdView.loadAd(adRequest);
        mAdView.setVisibility(View.VISIBLE);
        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // Code to be executed when an ad finishes loading.
                Log.e(TAG, "loaded");
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                // Code to be executed when an ad request fails.
                Log.e(TAG, "failed: " + errorCode);
            }

            @Override
            public void onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.

                Log.e(TAG, "opened");
            }

            @Override
            public void onAdLeftApplication() {
                // Code to be executed when the user has left the app.

                Log.e(TAG, "left app");
            }

            @Override
            public void onAdClosed() {
                // Code to be executed when when the user is about to return
                // to the app after tapping on an ad.

                Log.e(TAG, "closed");
            }
        });
    }

    void hideAd() {
        mAdView.destroy();
        mAdView.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.Wrapper:
                closeActivity();
                break;
            case R.id.Wrapper1:
                closeActivity();
                break;
            case R.id.settingsButton:
                if (UISettings.getVisibility() == View.GONE) {
                    // Show
                    showUISettings();
                    openUISettings = true;
                } else {
                    // Hide
                    hideUISettings();
                    openUISettings = false;
                }
                saveSettings(APP_PREFERENCES_OPEN_SETTINGS, openUISettings);
                break;
            case R.id.temperature1:
                saveSettings(APP_PREFERENCES_TEMPERATURE, 1);
                temperature = 1;
                overlayService();
                break;
            case R.id.temperature2:
                saveSettings(APP_PREFERENCES_TEMPERATURE, 2);
                temperature = 2;
                overlayService();
                break;
            case R.id.temperature3:
                saveSettings(APP_PREFERENCES_TEMPERATURE, 3);
                temperature = 3;
                overlayService();
                break;
            case R.id.temperature4:
                saveSettings(APP_PREFERENCES_TEMPERATURE, 4);
                temperature = 4;
                overlayService();
                break;
            case R.id.temperature5:
                saveSettings(APP_PREFERENCES_TEMPERATURE, 5);
                temperature = 5;
                overlayService();
                break;
            case R.id.darkTheme:
                if (!theme.equals("dark")) {
                    theme = "dark";
                    saveSettings(APP_PREFERENCES_THEME, theme);
                    if (dimmerSwitch.isChecked()) {
                        intent = new Intent(this, OverlayService.class);
                        intent.setAction("theme");
                        intent.putExtra("theme", theme);

                        if (checkAllows()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(intent);
                            } else {
                                startService(intent);
                            }
                        }
                    }
                }
                break;
                /*
            case R.id.lightTheme:
                if (!theme.equals("light")) {
                    theme = "light";
                    saveSettings(APP_PREFERENCES_THEME, theme);
                    if (dimmerSwitch.isChecked()) {
                        intent = new Intent(this, OverlayService.class);
                        intent.setAction("theme");
                        intent.putExtra("theme", theme);

                        if (checkAllows()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(intent);
                            } else {
                                startService(intent);
                            }
                        }
                    }
                }
                break;
                */
            case R.id.transparentTheme:
                if (!theme.equals("transparent")) {
                    theme = "transparent";
                    saveSettings(APP_PREFERENCES_THEME, theme);
                    if (dimmerSwitch.isChecked()) {
                        intent = new Intent(this, OverlayService.class);
                        intent.setAction("theme");
                        intent.putExtra("theme", theme);

                        if (checkAllows()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(intent);
                            } else {
                                startService(intent);
                            }
                        }
                    }
                }
                break;
            case R.id.timerTimeOn:
                pickTimer("timerOn");
                break;
            case R.id.timerTimeOff:
                pickTimer("timerOff");
                break;
            case R.id.policyButton:
                Intent intentPolicyButton = new Intent();
                intentPolicyButton.setAction(Intent.ACTION_VIEW);
                intentPolicyButton.addCategory(Intent.CATEGORY_BROWSABLE);
                intentPolicyButton.setData(Uri.parse("https://sites.google.com/view/ilushling/night-light-filter"));
                startActivity(intentPolicyButton);
                break;
            case R.id.policyText:
                Intent intentPolicyText = new Intent();
                intentPolicyText.setAction(Intent.ACTION_VIEW);
                intentPolicyText.addCategory(Intent.CATEGORY_BROWSABLE);
                intentPolicyText.setData(Uri.parse("https://sites.google.com/view/ilushling/night-light-filter"));
                startActivity(intentPolicyText);
                break;
        }
    }

    /**
     * 1 check value for contains in setting file [ELSE] save default value
     * 2 check value for valid value [ELSE] if invalid remove value and save default value
     */
    private void loadSettings() {
        // Theme
        if (mSettings.contains(APP_PREFERENCES_THEME)) {
            try {
                theme = mSettings.getString(APP_PREFERENCES_THEME, "dark");

                switch (theme) {
                    case "dark":
                        darkTheme.setChecked(true);
                        break;
                        /*
                    case "light":
                        lightTheme.setChecked(true);
                        break;
                        */
                    case "transparent":
                        transparentTheme.setChecked(true);
                        break;
                    default:
                        darkTheme.setChecked(true);
                        clearSetting(APP_PREFERENCES_THEME);
                        saveSettings(APP_PREFERENCES_THEME, "dark");
                        break;
                }

            } catch (Exception exc) {
                clearSetting(APP_PREFERENCES_THEME);
                saveSettings(APP_PREFERENCES_THEME, "dark");
            }
        } else {
            saveSettings(APP_PREFERENCES_THEME, "dark");
            theme = mSettings.getString(APP_PREFERENCES_THEME, "dark");
        }

        // [START Overlay]
        // Dimmer on
        if (mSettings.contains(APP_PREFERENCES_DIMMER_ON)) {
            try {
                dimmerOn = mSettings.getBoolean(APP_PREFERENCES_DIMMER_ON, false);
            } catch (Exception exc) {
                clearSetting(APP_PREFERENCES_DIMMER_ON);
                saveSettings(APP_PREFERENCES_DIMMER_ON, false);
            }
        } else {
            saveSettings(APP_PREFERENCES_DIMMER_ON, false);
            dimmerOn = mSettings.getBoolean(APP_PREFERENCES_DIMMER_ON, false);
        }

        // DimmerColor
        if (mSettings.contains(APP_PREFERENCES_DIMMER_COLOR)) {
            try {
                dimmerColor.setProgress(mSettings.getInt(APP_PREFERENCES_DIMMER_COLOR, 0));
            } catch (Exception exc) {
                clearSetting(APP_PREFERENCES_DIMMER_COLOR);
                saveSettings(APP_PREFERENCES_DIMMER_COLOR, 0);
            }
        } else {
            saveSettings(APP_PREFERENCES_DIMMER_COLOR, 0);
        }
        // Dimmer
        if (mSettings.contains(APP_PREFERENCES_DIMMER)) {
            try {
                dimmer.setProgress(mSettings.getInt(APP_PREFERENCES_DIMMER, 0));
            } catch (Exception exc) {
                clearSetting(APP_PREFERENCES_DIMMER);
                saveSettings(APP_PREFERENCES_DIMMER, 0);
            }
        } else {
            saveSettings(APP_PREFERENCES_DIMMER, 0);
        }
        // Temperature
        if (mSettings.contains(APP_PREFERENCES_TEMPERATURE)) {
            try {
                temperature = mSettings.getInt(APP_PREFERENCES_TEMPERATURE, 4);
                switch (temperature) {
                    case 1:
                        temperature1_rb.setChecked(true);
                        break;
                    case 2:
                        temperature2_rb.setChecked(true);
                        break;
                    case 3:
                        temperature3_rb.setChecked(true);
                        break;
                    case 4:
                        temperature4_rb.setChecked(true);
                        break;
                    case 5:
                        temperature5_rb.setChecked(true);
                        break;
                    default:
                        temperature4_rb.setChecked(true);
                        break;

                }
            } catch (Exception exc) {
                clearSetting(APP_PREFERENCES_TEMPERATURE);
                saveSettings(APP_PREFERENCES_TEMPERATURE, 4);
            }
        } else {
            saveSettings(APP_PREFERENCES_TEMPERATURE, 4);
        }
        // [END Overlay]

        // [START Timer]
        // Timer on
        if (mSettings.contains(APP_PREFERENCES_TIMER_ON)) {
            try {
                timerOn = mSettings.getBoolean(APP_PREFERENCES_TIMER_ON, false);
            } catch (Exception exc) {
                clearSetting(APP_PREFERENCES_TIMER_ON);
                saveSettings(APP_PREFERENCES_TIMER_ON, false);
                timerOn = mSettings.getBoolean(APP_PREFERENCES_TIMER_ON, false);
            }
        } else {
            saveSettings(APP_PREFERENCES_TIMER_ON, false);
            timerOn = mSettings.getBoolean(APP_PREFERENCES_TIMER_ON, false);
        }

        // TIME ON
        // HOUR ON
        if (mSettings.contains(APP_PREFERENCES_TIMER_HOUR_ON)) {
            try {
                timerHourOn = mSettings.getString(APP_PREFERENCES_TIMER_HOUR_ON, "22");
            } catch (Exception exc) {
                clearSetting(APP_PREFERENCES_TIMER_HOUR_ON);
                saveSettings(APP_PREFERENCES_TIMER_HOUR_ON, "22");
                timerHourOn = mSettings.getString(APP_PREFERENCES_TIMER_HOUR_ON, "22");
            }
        } else {
            saveSettings(APP_PREFERENCES_TIMER_HOUR_ON, "22");
            timerHourOn = mSettings.getString(APP_PREFERENCES_TIMER_HOUR_ON, "22");
        }
        // MINUTE ON
        if (mSettings.contains(APP_PREFERENCES_TIMER_MINUTE_ON)) {
            try {
                timerMinuteOn = mSettings.getString(APP_PREFERENCES_TIMER_MINUTE_ON, "0");
            } catch (Exception exc) {
                clearSetting(APP_PREFERENCES_TIMER_MINUTE_ON);
                saveSettings(APP_PREFERENCES_TIMER_MINUTE_ON, "0");
                timerMinuteOn = mSettings.getString(APP_PREFERENCES_TIMER_MINUTE_ON, "0");
            }
        } else {
            saveSettings(APP_PREFERENCES_TIMER_MINUTE_ON, "0");
            timerMinuteOn = mSettings.getString(APP_PREFERENCES_TIMER_MINUTE_ON, "0");
        }

        // TIME OFF
        // HOUR OFF
        if (mSettings.contains(APP_PREFERENCES_TIMER_HOUR_OFF)) {
            try {
                timerHourOff = mSettings.getString(APP_PREFERENCES_TIMER_HOUR_OFF, "7");
            } catch (Exception exc) {
                clearSetting(APP_PREFERENCES_TIMER_HOUR_OFF);
                saveSettings(APP_PREFERENCES_TIMER_HOUR_OFF, "7");
                timerHourOff = mSettings.getString(APP_PREFERENCES_TIMER_HOUR_OFF, "7");
            }
        } else {
            saveSettings(APP_PREFERENCES_TIMER_HOUR_OFF, "7");
            timerHourOff = mSettings.getString(APP_PREFERENCES_TIMER_HOUR_OFF, "7");
        }
        // MINUTE OFF
        if (mSettings.contains(APP_PREFERENCES_TIMER_MINUTE_OFF)) {
            try {
                timerMinuteOff = mSettings.getString(APP_PREFERENCES_TIMER_MINUTE_OFF, "0");
            } catch (Exception exc) {
                clearSetting(APP_PREFERENCES_TIMER_MINUTE_OFF);
                saveSettings(APP_PREFERENCES_TIMER_MINUTE_OFF, "0");
                timerMinuteOff = mSettings.getString(APP_PREFERENCES_TIMER_MINUTE_OFF, "0");
            }
        } else {
            saveSettings(APP_PREFERENCES_TIMER_MINUTE_OFF, "0");
            timerMinuteOff = mSettings.getString(APP_PREFERENCES_TIMER_MINUTE_OFF, "0");
        }
        // [END Timer]

        // Open Settings
        if (mSettings.contains(APP_PREFERENCES_OPEN_SETTINGS)) {
            try {
                openUISettings = mSettings.getBoolean(APP_PREFERENCES_OPEN_SETTINGS, false);
                if (openUISettings) {
                    showUISettings();

                    //showAd();
                } else {
                    hideUISettings();

                    //hideAd();
                }
            } catch (Exception exc) {
                clearSetting(APP_PREFERENCES_OPEN_SETTINGS);
                saveSettings(APP_PREFERENCES_OPEN_SETTINGS, false);
            }
        } else {
            saveSettings(APP_PREFERENCES_OPEN_SETTINGS, false);
        }

        // Charity
        if (mSettings.contains(APP_PREFERENCES_CHARITY)) {
            try {
                charity = mSettings.getBoolean(APP_PREFERENCES_CHARITY, false);
                charity_cb.setChecked(charity);
            } catch (Exception exc) {
                clearSetting(APP_PREFERENCES_TIMER_MINUTE_OFF);
                saveSettings(APP_PREFERENCES_TIMER_MINUTE_OFF, false);
                charity_cb.setChecked(charity);
            }
        } else {
            saveSettings(APP_PREFERENCES_CHARITY, false);
            charity_cb.setChecked(charity);
        }

        // [START format minutes to 00]
        // ON
        StringBuilder sb = new StringBuilder();
        sb.append(timerHourOn);
        if (sb.length() < 2) {
            sb.insert(0, '0'); // pad with leading zero if needed
        }
        timerHourOn = sb.toString();

        sb = new StringBuilder();
        sb.append(timerMinuteOn);
        if (sb.length() < 2) {
            sb.insert(0, '0'); // pad with leading zero if needed
        }
        timerMinuteOn = sb.toString();
        // OFF
        sb = new StringBuilder();
        sb.append(timerHourOff);
        if (sb.length() < 2) {
            sb.insert(0, '0'); // pad with leading zero if needed
        }
        timerHourOff = sb.toString();

        sb = new StringBuilder();
        sb.append(timerMinuteOff);
        if (sb.length() < 2) {
            sb.insert(0, '0'); // pad with leading zero if needed
        }
        timerMinuteOff = sb.toString();

        // [END format minutes to 00]
        timerTimeOn.setText("ON " + timerHourOn + ":" + timerMinuteOn);
        timerTimeOff.setText("OFF " + timerHourOff + ":" + timerMinuteOff);


        // Update UI
        /**
         * Switchers need after loading because they listeners triggering before load
         */
        // Switcher
        dimmerSwitch.setChecked(dimmerOn);
        // Timer
        timerSwitch.setChecked(timerOn);
        /**
         * if seekbars was changed it call overlayService there are 3 seekbars so need only once and after load
         */
        first = false;
        overlayService();
    }

    // Sound Mode
    void getSoundMode() {
        Utils utils = new Utils(this);
        switch (utils.getSoundMode()) {
            case 0:
                soundMode.setVisibility(View.VISIBLE);
                soundMode.setImageResource(R.drawable.sound_off);
                break;
            case 1:
                soundMode.setVisibility(View.VISIBLE);
                soundMode.setImageResource(R.drawable.sound_vibration);
                break;
            case 2:
                soundMode.setVisibility(View.VISIBLE);
                soundMode.setImageResource(R.drawable.sound_on);
                break;
            default:
                soundMode.setVisibility(View.GONE);
                break;
        }
    }

    void permitted() {
        dimmerSwitch.setEnabled(true);
        settingsButton.setEnabled(true);
        dimmerColor.setEnabled(true);
        dimmer.setEnabled(true);
        timerSwitch.setEnabled(true);
        //timerTimeOn.setEnabled(true);
        //timerTimeOff.setEnabled(true);
    }

    void resricted() {
        //dimmerSwitch.setEnabled(false);
        dimmerSwitch.setChecked(false);
        settingsButton.setEnabled(false);
        hideUISettings();
        dimmerColor.setEnabled(false);
        dimmer.setEnabled(false);
        //timerSwitch.setEnabled(false);
        timerSwitch.setChecked(false);
        //timerTimeOn.setEnabled(false);
        //timerTimeOff.setEnabled(false);
    }

    // [START Permissions]
    boolean checkAllows() {
        /**
         * for Android 6 and below need to check permissions
         * 1 check version SDK (>= M)
         * 2 check Permission if don't have need request them (Dialog)
         * 3 if don't work dialog run alternative via settings (Activity result)
         */

        if (checkPermissionOverlay()) {
            permitted();
            // CLOSE_SYSTEM_DIALOGS
            Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            sendBroadcast(it);
            return true;
        } else {
            resricted();
            return false;
        }
    }

    private boolean checkPermissionOverlay() {
        // via settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Overlay
            if (!Settings.canDrawOverlays(this)) {
                showPermission("overlay", getString(R.string.restriction_overlay));
                return false;
            } else {
                return true;
            }
        }
        return true;
    }

    // [START select timer]
    void pickTimer(final String action) {
        int hour = 0, minute = 0;
        switch (action) {
            case "timerOn":
                hour = Integer.parseInt(timerHourOn);
                minute = Integer.parseInt(timerMinuteOn);
                break;
            case "timerOff":
                hour = Integer.parseInt(timerHourOff);
                minute = Integer.parseInt(timerMinuteOff);
                break;
        }

        TimePickerDialog timePickerDialog = new TimePickerDialog(MainActivity.this, TimePickerDialog.THEME_DEVICE_DEFAULT_DARK, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hour, int minute) {
                switch (action) {
                    case "timerOn":
                        timerHourOn = String.valueOf(hour);
                        timerMinuteOn = String.valueOf(minute);

                        saveSettings(APP_PREFERENCES_TIMER_HOUR_ON, timerHourOn);
                        saveSettings(APP_PREFERENCES_TIMER_MINUTE_ON, timerMinuteOn);

                        // format minutes to 00
                        StringBuilder sb = new StringBuilder();
                        sb.append(timerMinuteOn);
                        if (sb.length() < 2) {
                            sb.insert(0, '0'); // pad with leading zero if needed
                        }
                        timerMinuteOn = sb.toString();

                        // Update UI
                        timerTimeOn.setText("ON " + timerHourOn + ":" + timerMinuteOn);
                        break;
                    case "timerOff":
                        timerHourOff = String.valueOf(hour);
                        timerMinuteOff = String.valueOf(minute);

                        saveSettings(APP_PREFERENCES_TIMER_HOUR_OFF, timerHourOff);
                        saveSettings(APP_PREFERENCES_TIMER_MINUTE_OFF, timerMinuteOff);

                        // format minutes to 00
                        sb = new StringBuilder();
                        sb.append(timerMinuteOff);
                        if (sb.length() < 2) {
                            sb.insert(0, '0'); // pad with leading zero if needed
                        }
                        timerMinuteOff = sb.toString();

                        timerTimeOff.setText("OFF " + timerHourOff + ":" + timerMinuteOff);
                        break;
                }

                timerService();
            }
        }, hour, minute, true);
        timePickerDialog.show();
    }

    // Run Alarm in service
    public void timerService() {
        Intent intent = new Intent(this, OverlayService.class);

        if (timerOn && timerHourOn != null && timerMinuteOn != null && timerHourOff != null && timerMinuteOff != null) {
            // Timer on
            intent.setAction("timerOn");
            intent.putExtra("timerHourOn", timerHourOn);
            intent.putExtra("timerMinuteOn", timerMinuteOn);
            intent.putExtra("timerHourOff", timerHourOff);
            intent.putExtra("timerMinuteOff", timerMinuteOff);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        } else if (!timerOn) {
            // Timer off
            intent.setAction("timerOff");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        }
    }

    void fetchRemoteConfig() {
        // cache expiration in seconds
        int cacheExpiration = 30;
        //expire the cache immediately for development mode.
        if (mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }
        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // After config data is successfully fetched, it must be activated before newly fetched
                            // values are returned.
                            mFirebaseRemoteConfig.activateFetched();
                        }
                    }
                });
    }


    // Dialog for permission
    public void showPermission(String permission, String message) {
        switch (permission) {
            case "overlay":
                new AlertDialog.Builder(this)
                        .setMessage(message)
                        .setPositiveButton("OK", listenerOverlay)
                        .setNegativeButton("Cancel", listenerOverlay)
                        .create()
                        .show();
                break;
            case "sound":
                new AlertDialog.Builder(this)
                        .setMessage(message)
                        .setPositiveButton("OK", listenerSound)
                        .setNegativeButton("Cancel", listenerSound)
                        .create()
                        .show();
                break;
        }
    }

    // Alternative permission
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            // ** if so check once again if we have permission */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    // continue here - permission was granted
                    permitted();
                    Log.e(TAG, "Granted 1");
                }
            }
        }
        if (requestCode == 2) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // continue here - permission was granted
                Log.e(TAG, "Granted 2");
            }
        }
        if (requestCode == 3) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // continue here - permission was granted
                Log.e(TAG, "Granted 3");
            }
        }
    }
    // [END Permissions]

    void closeActivity() {
        Wrapper.animate().alpha(0.0f).setDuration(150).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                finish();
            }
        });
    }


    @Override
    protected void onPause() {
        super.onPause();

        Wrapper.animate().alpha(0.0f).setDuration(150);
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction(CLOSE_ACTION);
        registerReceiver(broadcastReceiver, filter);

        Wrapper.animate().alpha(1.0f).setDuration(150);
    }

    @Override
    protected void onStop() {
        super.onStop();

        Wrapper.animate().alpha(0.0f).setDuration(150);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Bundle bundle = new Bundle();
        bundle.putInt("dimmer", dimmerValue);
        bundle.putInt("dimmerColor", dimmerColorValue);
        bundle.putBoolean("timerOn", timerOn);
        bundle.putString("timerTimeOn", timerHourOn + ":" + timerMinuteOn);
        bundle.putString("timerTimeOff", timerHourOff + ":" + timerMinuteOff);
        bundle.putInt("temperature", temperature);
        bundle.putString("theme", theme);
        mFirebaseAnalytics.logEvent("settings", bundle);

        try {
            // Free receiver
            unregisterReceiver(broadcastReceiver);
            // Kill process
        } catch (Exception exc) {
            Log.e(TAG, "onDestroy: " + exc);
        }
    }
}

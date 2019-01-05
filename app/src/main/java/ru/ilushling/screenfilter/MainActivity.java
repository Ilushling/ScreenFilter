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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import static ru.ilushling.screenfilter.Utils.protectAppManager;


public class MainActivity extends Activity implements View.OnClickListener {

    // Common
    String TAG = "MainActivity";
    boolean first = true;
    public static final String APP_PREFERENCES_THEME = "theme";
    Switch dimmerSwitch, timerSwitch;
    protected TextView timerTimeOn, timerTimeOff;
    SeekBar dimmer, dimmerColor;
    // Dimmer
    public static final String APP_PREFERENCES_DIMMER_ON = "dimmerOn", APP_PREFERENCES_DIMMER_COLOR = "dimmerColor", APP_PREFERENCES_DIMMER = "dimmer", APP_PREFERENCES_TEMPERATURE = "temperature";
    TextView dimmerColor_status, dimmer_status;
    // UI
    ConstraintLayout UIMain, Wrapper, UISettings;

    // Save Settings
    SharedPreferences mSettings;
    RadioButton temperature1rb, temperature2rb, temperature3rb, temperature4rb, temperature5rb, darkTheme, lightTheme;
    // Name setting file
    public static final String APP_PREFERENCES_NAME = "PREFERENCE_FILE";
    ImageButton settingsButton;
    // Timer
    public static final String APP_PREFERENCES_TIMER_ON = "timerOn",
            APP_PREFERENCES_TIMER_HOUR_ON = "timerHourOn", APP_PREFERENCES_TIMER_MINUTE_ON = "timerMinuteOn",
            APP_PREFERENCES_TIMER_HOUR_OFF = "timerHourOff", APP_PREFERENCES_TIMER_MINUTE_OFF = "timerMinuteOff";
    // Variables
    String theme;
    private int dimmerColorValue, dimmerValue;
    boolean dimmerOn, timerOn;
    int temperature;
    protected String timerHourOn, timerMinuteOn, timerHourOff, timerMinuteOff;
    // Close
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Log.e(TAG, intent.getAction());

            switch (intent.getAction()) {
                case BReceiver.APP_OVERLAY_ON:
                    dimmerSwitch.setChecked(true);
                    break;
                case BReceiver.APP_OVERLAY_OFF:
                    dimmerSwitch.setChecked(false);
                    break;
                case OverlayService.CLOSE_ACTION:
                    finish();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        temperature1rb = findViewById(R.id.temperature1);
        temperature1rb.setOnClickListener(this);
        temperature2rb = findViewById(R.id.temperature2);
        temperature2rb.setOnClickListener(this);
        temperature3rb = findViewById(R.id.temperature3);
        temperature3rb.setOnClickListener(this);
        temperature4rb = findViewById(R.id.temperature4);
        temperature4rb.setOnClickListener(this);
        temperature5rb = findViewById(R.id.temperature5);
        temperature5rb.setOnClickListener(this);

        GradientDrawable gd = (GradientDrawable) temperature1rb.getBackground();
        gd.setColor(Color.rgb(255, 50, 20));
        GradientDrawable gd2 = (GradientDrawable) temperature2rb.getBackground();
        gd2.setColor(Color.rgb(255, 90, 40));
        GradientDrawable gd3 = (GradientDrawable) temperature3rb.getBackground();
        gd3.setColor(Color.rgb(255, 110, 50));
        GradientDrawable gd4 = (GradientDrawable) temperature4rb.getBackground();
        gd4.setColor(Color.rgb(255, 135, 60));
        GradientDrawable gd5 = (GradientDrawable) temperature5rb.getBackground();
        gd5.setColor(Color.rgb(255, 160, 75));

        // UI
        Wrapper = findViewById(R.id.Wrapper);
        Wrapper.setOnClickListener(this);
        Wrapper.setAlpha(0);

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
        Wrapper.setAlpha(0);

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
        lightTheme = findViewById(R.id.lightTheme);
        lightTheme.setOnClickListener(this);

        // Settings
        mSettings = getSharedPreferences(APP_PREFERENCES_NAME, Context.MODE_PRIVATE);

        // Dimmer
        dimmerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                dimmerOn = isChecked;
                saveSettings(APP_PREFERENCES_DIMMER_ON, dimmerOn);

                if (dimmerOn) {
                    overlayService();
                } else {
                    overlayOff();
                }
            }
        });

        // Timer
        timerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                timerOn = isChecked;
                saveSettings(APP_PREFERENCES_TIMER_ON, timerOn);
                timerService();

                if (timerOn) {
                    timerTimeOn.setEnabled(true);
                    timerTimeOff.setEnabled(true);
                } else {
                    timerTimeOn.setEnabled(false);
                    timerTimeOff.setEnabled(false);
                }
            }

        });

        // Receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(BReceiver.APP_OVERLAY_ON);
        filter.addAction(BReceiver.APP_OVERLAY_OFF);
        filter.addAction(OverlayService.CLOSE_ACTION);
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

    void permitted() {
        dimmerSwitch.setEnabled(true);
        dimmerColor.setEnabled(true);
        dimmer.setEnabled(true);
        timerSwitch.setEnabled(true);
        timerTimeOn.setEnabled(true);
        timerTimeOff.setEnabled(true);
    }

    void resricted() {
        dimmerSwitch.setEnabled(false);
        dimmerSwitch.setChecked(false);
        dimmerColor.setEnabled(false);
        dimmer.setEnabled(false);
        timerSwitch.setEnabled(false);
        timerSwitch.setChecked(false);
        timerTimeOn.setEnabled(false);
        timerTimeOff.setEnabled(false);
    }

    @Override
    protected void onStart() {
        super.onStart();

        /**
         * for Android 6 and below need to check permissions
         * 1 check version SDK (>= M)
         * 2 check Permission if don't have need request them (Dialog)
         * 3 if don't work dialog run alternative via settings (Activity result)
         */
        if (isCheckPermissionAlternative()) {
            permitted();

            loadSettings();

            // CLOSE_SYSTEM_DIALOGS
            Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            sendBroadcast(it);
        } else {
            resricted();
        }
        // Protect Power Manager
        protectAppManager(this);
    }

    private void overlayService() {
        if (dimmerColorValue == 0 && dimmerValue == 0) {
            overlayOff();
        } else if (dimmerOn && !first) {
            overlayOn();
        }
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.Wrapper:
                closeActivity();
                break;
            case R.id.settingsButton:
                if (UISettings.getVisibility() == View.GONE) {
                    // Show
                    // Visibility
                    UISettings.setVisibility(View.VISIBLE);
                    // Animation alpha
                    UISettings.animate().alpha(1.0f).setDuration(150);
                } else {
                    // Hide
                    // Animation alpha
                    UISettings.animate().alpha(0.0f).setDuration(150).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            // Visibility
                            UISettings.setVisibility(View.GONE);
                            // Clear listener
                            UISettings.animate().setListener(null);
                        }
                    });
                }
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
                    intent = new Intent(this, OverlayService.class);
                    intent.setAction("theme");
                    intent.putExtra("theme", theme);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent);
                    } else {
                        startService(intent);
                    }
                }
                break;
            case R.id.lightTheme:
                if (!theme.equals("light")) {
                    theme = "light";
                    saveSettings(APP_PREFERENCES_THEME, theme);
                    intent = new Intent(this, OverlayService.class);
                    intent.setAction("theme");
                    intent.putExtra("theme", theme);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent);
                    } else {
                        startService(intent);
                    }
                }
                break;
            case R.id.timerTimeOn:
                pickTimer("timerOn");
                break;
            case R.id.timerTimeOff:
                pickTimer("timerOff");
                break;
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

    // Load values from save

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

    private void clearSetting(String key) {
        // Prepare for save
        SharedPreferences.Editor editor = mSettings.edit();
        // Edit Variables
        editor.remove(key);
        // Save
        editor.apply();
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
    // [END select timer]

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

    // [START Permissions]
    // Dialog for permission
    private void showMessageOKCancel(String message) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", listener)
                .setNegativeButton("Cancel", listener)
                .create()
                .show();
    }

    // Listener for dialog
    DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {

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
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, 1);
                    dialog.dismiss();
                    break;
            }
        }
    };

    /**
     * 1 check value for contains in setting file [ELSE] save default value
     * 2 check value for valid value [ELSE] if invalid remove value and save default value
     */
    private void loadSettings() {
        // UI
        if (mSettings.contains(APP_PREFERENCES_THEME)) {
            try {
                theme = mSettings.getString(APP_PREFERENCES_THEME, "dark");

                switch (theme) {
                    case "dark":
                        darkTheme.setChecked(true);
                        break;
                    case "light":
                        lightTheme.setChecked(true);
                        break;
                    default:
                        darkTheme.setChecked(true);
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
                temperature = mSettings.getInt(APP_PREFERENCES_TEMPERATURE, 3);
                switch (temperature) {
                    case 1:
                        temperature1rb.setChecked(true);
                        break;
                    case 2:
                        temperature2rb.setChecked(true);
                        break;
                    case 3:
                        temperature3rb.setChecked(true);
                        break;
                    case 4:
                        temperature4rb.setChecked(true);
                        break;
                    case 5:
                        temperature5rb.setChecked(true);
                        break;
                    default:
                        temperature3rb.setChecked(true);
                        break;

                }
            } catch (Exception exc) {
                clearSetting(APP_PREFERENCES_TEMPERATURE);
                saveSettings(APP_PREFERENCES_TEMPERATURE, 3);
            }
        } else {
            saveSettings(APP_PREFERENCES_TEMPERATURE, 3);
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
        timerTimeOn.setEnabled(timerOn);
        timerTimeOff.setEnabled(timerOn);
        /**
         * if seekbars was changed it call overlayService there are 3 seekbars so need only once and after load
         */
        first = false;
        overlayService();
    }

    // Alternative permission
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            // ** if so check once again if we have permission */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    // continue here - permission was granted
                    Log.e(TAG, "Granted alternative");
                }
            }
        }
    }

    // [END Permissions]

    private boolean isCheckPermissionAlternative() {
        // via settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                showMessageOKCancel(getString(R.string.restriction_overlay));
                return false;
            } else {
                return true;
            }
        }
        return true;
    }

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
        filter.addAction(OverlayService.CLOSE_ACTION);
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
        try {
            // Free receiver
            unregisterReceiver(broadcastReceiver);
            // Kill process
        } catch (Exception exc) {
            Log.e(TAG, "onDestroy: " + exc);
        }
    }
}

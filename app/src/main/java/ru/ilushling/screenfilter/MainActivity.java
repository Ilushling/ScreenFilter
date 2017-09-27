package ru.ilushling.screenfilter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;


public class MainActivity extends Activity implements View.OnClickListener {

    // Common
    String TAG = "MainActivity";
    boolean first = true;
    // UI
    Switch dimmerSwitch, timerSwitch;
    protected TextView timerTimeOn, timerTimeOff;
    SeekBar dimmer, dimmerColor;
    TextView dimmerColor_status, dimmer_status;
    Button button_close;

    // Save Settings
    SharedPreferences mSettings;
    // Name setting file
    public static final String APP_PREFERENCES_NAME = "PREFERENCE_FILE";
    // Dimmer
    public static final String APP_PREFERENCES_DIMMER_ON = "dimmerOn", APP_PREFERENCES_DIMMER_COLOR = "dimmerColor", APP_PREFERENCES_DIMMER = "dimmer";
    // Timer
    public static final String APP_PREFERENCES_TIMER_ON = "timerOn",
            APP_PREFERENCES_TIMER_HOUR_ON = "timerHourOn", APP_PREFERENCES_TIMER_MINUTE_ON = "timerMinuteOn",
            APP_PREFERENCES_TIMER_HOUR_OFF = "timerHourOff", APP_PREFERENCES_TIMER_MINUTE_OFF = "timerMinuteOff";
    // Variables
    private int dimmerColorValue, dimmerValue;
    boolean dimmerOn, timerOn;
    protected String timerHourOn, timerMinuteOn, timerHourOff, timerMinuteOff;
    // Permissions
    private static final int REQUEST_PERMISSION = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // UI
        // Dimmer
        dimmerSwitch = findViewById(R.id.dimmerSwitch);
        dimmerColor = findViewById(R.id.DimmerColor_seekbar);
        dimmerColor_status = findViewById(R.id.DimmerColor_status);
        dimmer = findViewById(R.id.Dimmer_seekbar);
        dimmer_status = findViewById(R.id.Dimmer_status);
        // Timer
        timerTimeOn = findViewById(R.id.timerTimeOn);
        timerTimeOn.setOnClickListener(this);
        timerTimeOff = findViewById(R.id.timerTimeOff);
        timerTimeOff.setOnClickListener(this);
        timerSwitch = findViewById(R.id.timerSwitch);

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

        // Close Button
        button_close = findViewById(R.id.button_close);
        button_close.setOnClickListener(this);

        // Settings
        mSettings = getSharedPreferences(APP_PREFERENCES_NAME, Context.MODE_PRIVATE);

        // Receiver
        IntentFilter filter = new IntentFilter(BReceiver.APP_PREFERENCES_OVERLAY);
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

    @Override
    protected void onStart() {
        super.onStart();

        /**
         * for Android 6 and below need to check permissions
         * 1 check version SDK (>= M)
         * 2 check Permission if dont have need request them (Dialog)
         * 3 if dont work dialog run alternative via settings (Activity result)
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
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(BReceiver.APP_PREFERENCES_OVERLAY);
        filter.addAction(OverlayService.CLOSE_ACTION);
        registerReceiver(broadcastReceiver, filter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_close:
                finish();
                break;
            case R.id.timerTimeOn:
                pickTimer("timerOn");
                break;
            case R.id.timerTimeOff:
                pickTimer("timerOff");
                break;
        }
    }

    private void overlayService() {
        if (dimmerColorValue == 0 && dimmerValue == 0) {
            overlayOff();
        } else if (dimmerOn && !first) {
            overlayOn();
        }
    }

    void overlayOn() {
        Intent intent = new Intent(this, OverlayService.class);
        intent.setAction("overlayOn");
        intent.putExtra("dimmerColorValue", dimmerColorValue);
        intent.putExtra("dimmerValue", dimmerValue);
        startService(intent);
    }

    void overlayOff() {
        Intent intent = new Intent(this, OverlayService.class);
        intent.setAction("overlayOff");
        startService(intent);
    }

    // Load values from save

    /**
     * 1 check value for contains in setting file [ELSE] save default value
     * 2 check value for valid value [ELSE] if invalid remove value and save default value
     */
    private void loadSettings() {
        // [START Overlay]
        // Dimmer on
        if (mSettings.contains(APP_PREFERENCES_DIMMER_ON)) {
            try {
                dimmerOn = mSettings.getBoolean(APP_PREFERENCES_DIMMER_ON, false);
            } catch (Exception e) {
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
            } catch (Exception e) {
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
            } catch (Exception e) {
                clearSetting(APP_PREFERENCES_DIMMER);
                saveSettings(APP_PREFERENCES_DIMMER, 0);
            }
        } else {
            saveSettings(APP_PREFERENCES_DIMMER, 0);
        }
        // [END Overlay]

        // [START Timer]
        // Timer on
        if (mSettings.contains(APP_PREFERENCES_TIMER_ON)) {
            try {
                timerOn = mSettings.getBoolean(APP_PREFERENCES_TIMER_ON, false);
            } catch (Exception e) {
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
            } catch (Exception e) {
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
            } catch (Exception e) {
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
            } catch (Exception e) {
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
            } catch (Exception e) {
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
        dimmerSwitch.setChecked(dimmerOn);
        timerSwitch.setChecked(timerOn);
        /**
         * if seekbars was changed it call overlayService there are 3 seekbars so need only once and after load
         */
        first = false;
        overlayService();
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
            startService(intent);
        } else if (!timerOn) {
            // Timer off
            intent.setAction("timerOff");
            startService(intent);
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


    private boolean isCheckPermissionAlternative() {
        // via settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                showMessageOKCancel("Разрешить приложению использовать оверлей?");
                return false;
            } else {
                return true;
            }
        }
        return true;
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

    // Close
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Log.e(TAG, intent.getAction());

            switch (intent.getAction()) {
                case BReceiver.APP_PREFERENCES_OVERLAY:
                    dimmerOn = intent.getBooleanExtra("overlayOn", false);

                    if (dimmerOn) {
                        dimmerSwitch.setChecked(true);
                    } else {
                        dimmerSwitch.setChecked(false);
                    }
                    break;
                case OverlayService.CLOSE_ACTION:
                    finish();
                    break;
            }
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            // Free receiver
            unregisterReceiver(broadcastReceiver);
            // Kill process
        } catch (Exception ex) {

        }
    }
}

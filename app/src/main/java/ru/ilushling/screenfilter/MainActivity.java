package ru.ilushling.screenfilter;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import static ru.ilushling.screenfilter.R.id.DimmerColor_status;
import static ru.ilushling.screenfilter.R.id.Dimmer_status;

public class MainActivity extends Activity implements View.OnClickListener {

    SeekBar dimmer, dimmerColor;
    TextView dimmerColor_status, dimmer_status;
    int dimmerColorValue, dimmerValue;
    Intent intent;
    Button btnShow;

    // Save Settings
    SharedPreferences mSettings;
    // Name setting file
    public static final String APP_PREFERENCES_NAME = "PREFERENCE_FILE";
    // Variables
    // Color
    public static final String APP_PREFERENCES_DimmerColor = "dimmerColor";
    // Dimmer
    public static final String APP_PREFERENCES_Dimmer = "dimmer";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Controls
        // Color
        dimmerColor = (SeekBar) findViewById(R.id.DimmerColor_seekbar);
        dimmerColor_status = (TextView) findViewById(DimmerColor_status);
        // Dimmer
        dimmer = (SeekBar) findViewById(R.id.Dimmer_seekbar);
        dimmer_status = (TextView) findViewById(Dimmer_status);
        // Settings
        mSettings = getSharedPreferences(APP_PREFERENCES_NAME, Context.MODE_PRIVATE);

        // Service
        intent = new Intent(this, OverlayService.class);

        registerReceiver(broadcastReceiver, new IntentFilter("ru.ilushling.screenfilter.br_close"));


        // DimmerColor control
        dimmerColor.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                dimmerColorValue = progress;
                dimmerColor_status.setText("" + (int) (dimmerColorValue / 2.55) + "%");
                toggleService();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Save
                saveSettings(APP_PREFERENCES_DimmerColor, dimmerColorValue);
                // Apply
                toggleService();
            }
        });

        // Dimmer control
        dimmer.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                dimmerValue = progress;
                dimmer_status.setText("" + (int) (dimmerValue / 2.55) + "%");
                toggleService();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Save
                saveSettings(APP_PREFERENCES_Dimmer, dimmerValue);
                // Apply
                toggleService();
            }
        });


        btnShow = (Button) findViewById(R.id.button);

        btnShow.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadSettings();

        // wtf
        Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        sendBroadcast(it);
    }

    @Override
    protected void onResume() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(OverlayService.CLOSE_ACTION);
        registerReceiver(broadcastReceiver, filter);
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver(broadcastReceiver);
        }catch(Exception ex){

        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        finish();
    }

    private void toggleService() {
        Intent intent = new Intent(this, OverlayService.class);

        if (dimmerColorValue == 0 && dimmerValue == 0) {
            stopService(intent);
        } else {
            intent.putExtra("dimmerColorValue", dimmerColorValue);
            intent.putExtra("dimmerValue", dimmerValue);
            startService(intent);
        }
    }

    private void loadSettings() {
        // Load values from save
        if (mSettings.contains(APP_PREFERENCES_DimmerColor)) {
            dimmerColor.setProgress(mSettings.getInt(APP_PREFERENCES_DimmerColor, 1));
        }
        if (mSettings.contains(APP_PREFERENCES_Dimmer)) {
            dimmer.setProgress(mSettings.getInt(APP_PREFERENCES_Dimmer, 1));
        }
    }

    private void saveSettings(String key, int value) {
        // Prepare for save
        SharedPreferences.Editor editor = mSettings.edit();
        // Edit Variables
        editor.putInt(key, value);
        // Save
        editor.apply();
    }

    // Close
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()){
                case OverlayService.CLOSE_ACTION:
                    finish();
                    break;
            }
        }
    };

}

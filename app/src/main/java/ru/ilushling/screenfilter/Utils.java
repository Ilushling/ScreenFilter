package ru.ilushling.screenfilter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.List;

import static ru.ilushling.screenfilter.MainActivity.APP_PREFERENCES_NAME;

public class Utils {
    private AudioManager audioManager;

    Utils(Context context) {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    // [START Battery protection]
    public static void protectAppManager(final Context context) {
        SharedPreferences settings = context.getSharedPreferences(APP_PREFERENCES_NAME, Context.MODE_PRIVATE);
        boolean skipMessage = settings.getBoolean("skipProtectedAppCheck", false);

        // Obtain the FirebaseAnalytics instance.
        final FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);

        if (!skipMessage) {
            final SharedPreferences.Editor editor = settings.edit();
            boolean foundCorrectIntent = false;
            for (final Intent intent : ListPowerManager.POWERMANAGER_INTENTS) {
                if (isCallable(context, intent)) {
                    // Firebase
                    Bundle bundle = new Bundle();
                    bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "1");
                    bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "protectAppManager");
                    bundle.putString(FirebaseAnalytics.Param.ITEM_BRAND, Build.MANUFACTURER);
                    bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "Show");
                    mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

                    foundCorrectIntent = true;

                    View dontShowView = View.inflate(context, R.layout.check_box, null);
                    CheckBox dontShowAgain = dontShowView.findViewById(R.id.checkBox);
                    dontShowAgain.setText(R.string.do_not_show_again);
                    dontShowAgain.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            editor.putBoolean("skipProtectedAppCheck", isChecked);
                            editor.apply();
                        }
                    });

                    new AlertDialog.Builder(context)
                            .setTitle(Build.MANUFACTURER + " Protected Apps")
                            .setMessage(String.format(context.getString(R.string.protected_apps_dialog), context.getString(R.string.app_name)))
                            .setView(dontShowView)
                            .setPositiveButton(R.string.go_to_settings, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    context.startActivity(intent);

                                    Bundle bundle = new Bundle();
                                    bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "1");
                                    bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "protectAppManager");
                                    bundle.putString(FirebaseAnalytics.Param.ITEM_BRAND, Build.MANUFACTURER);
                                    bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "Set");
                                    mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                    break;
                }
            }
            if (!foundCorrectIntent) {
                editor.putBoolean("skipProtectedAppCheck", true);
                editor.apply();
            }
        }
    }

    private static boolean isCallable(Context context, Intent intent) {
        List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }
    // [END Battery protection]

    int getSoundMode() {
        return audioManager.getRingerMode();
    }

    void setSoundMode() {
        switch (getSoundMode()) {
            case 0:
                audioManager.setRingerMode(1);
                break;
            case 1:
                audioManager.setRingerMode(2);
                break;
            case 2:
                audioManager.setRingerMode(0);
                break;
        }
    }
}

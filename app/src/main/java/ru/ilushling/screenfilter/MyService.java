package ru.ilushling.screenfilter;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class MyService extends IntentService {
    public MyService() {
        super("name1");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        int DimmerColorValue = intent.getIntExtra("DimmerColorValue", 0);
        int DimmerValue = intent.getIntExtra("DimmerValue", 0);

        Intent intent1 = new Intent(this, OverlayService.class);
        intent.putExtra("DimmerColorValue", DimmerColorValue);
        intent.putExtra("DimmerValue", DimmerValue);
        Log.e("egor1: ", "" + DimmerColorValue);
        // Try to stop the service if it is already running
        // Otherwise start the service
        if (!stopService(intent1)) {
            startService(intent1);
        }
    }
}

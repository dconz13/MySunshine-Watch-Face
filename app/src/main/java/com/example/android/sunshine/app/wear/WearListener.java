package com.example.android.sunshine.app.wear;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by dillon on 4/11/16.
 */
public class WearListener extends WearableListenerService {
    public static final String ACTION_WEAR_UPDATED =
            "com.example.android.sunshine.app.ACTION_WEAR_UPDATED";
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        super.onDataChanged(dataEvents);
        Log.d("WearListener", "onDataChanged");

        final List events = FreezableUtils
                .freezeIterable(dataEvents);

        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        ConnectionResult connectionResult =
                googleApiClient.blockingConnect(30, TimeUnit.SECONDS);

        if (!connectionResult.isSuccess()) {
            Log.e("WearListener", "Failed to connect to GoogleApiClient.");
            return;
        }
        // Create intent to call onHandleIntent on WearIntentService
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath().equals("/data_request")) {
                startService(new Intent(ACTION_WEAR_UPDATED).setClass(this, WearIntentService.class));
            }
        }
    }
}

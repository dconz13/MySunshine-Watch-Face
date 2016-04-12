package com.example.android.sunshine.app.wear;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.example.android.sunshine.app.Utility;
import com.example.android.sunshine.app.data.WeatherContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

public class WearIntentService extends IntentService implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String LOG_TAG = WearIntentService.class.getSimpleName();
    // TODO: Add an extra column to the contract for keeping track of previous hi / lo / weather_id values to reduce watch update as much as possible
    private static final String[] FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
    };
    // these indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;

    GoogleApiClient mGoogleApiClient;

    public WearIntentService() {
        super("WearIntentService");
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onCreate() {
        super.onCreate();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(Log.isLoggable(LOG_TAG,Log.DEBUG)){
            Log.d(LOG_TAG,"onHandleIntent");
        }
        // TODO: differentiate from phone requesting update and sync update
        sendDataToWear();
    }

    private void sendDataToWear(){
        mGoogleApiClient.connect();

        String location = Utility.getPreferredLocation(this);
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                location, System.currentTimeMillis());
        Cursor data = getContentResolver().query(weatherForLocationUri, FORECAST_COLUMNS, null,
                null, WeatherContract.WeatherEntry.COLUMN_DATE + " ASC");
        if (data == null) {
            return;
        }
        if (!data.moveToFirst()) {
            data.close();
            return;
        }

        int weatherId = data.getInt(INDEX_WEATHER_ID);
        int weatherArtResource = Utility.getArtResourceForWeatherCondition(weatherId);
        double temperatureMax = data.getDouble(INDEX_MAX_TEMP);
        double temperatureMin = data.getDouble(INDEX_MIN_TEMP);
        boolean isMetric = Utility.isMetric(this);

        Asset weatherArtAsset = Utility.createAssetFromBitmap(
                BitmapFactory.decodeResource(getResources(), weatherArtResource));

        // TODO: check data to see if it has changed. Only send data that has changed.
        // TODO: ideally only do one data send.
        if(mGoogleApiClient.isConnected()){
            Log.d(LOG_TAG, "mGoogleApiClient is connected");
            PutDataMapRequest imageDataMap = PutDataMapRequest.create("/image");
            imageDataMap.getDataMap().putAsset("weatherArt", weatherArtAsset);
            imageDataMap.getDataMap().putDouble("high", temperatureMax);
            imageDataMap.getDataMap().putDouble("low", temperatureMin);
            imageDataMap.getDataMap().putLong("time", System.currentTimeMillis());
            imageDataMap.getDataMap().putBoolean("isMetric", isMetric);
            Wearable.DataApi.putDataItem(mGoogleApiClient, imageDataMap.asPutDataRequest());

        }
    }

}

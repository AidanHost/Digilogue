package com.greenman.digilogue;

import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.Time;
import android.util.TimeUtils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.greenman.common.Utility;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class WeatherService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, MessageApi.MessageListener {
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(Utility.PATH_DIGILOGUE_SETTINGS)) {
            byte[] rawData = messageEvent.getData();
            DataMap config = DataMap.fromByteArray(rawData);

            if (config == null) {
                config = new DataMap();
            }

            if (!mGoogleApiClient.isConnected()) {
                ConnectionResult connectionResult = mGoogleApiClient.blockingConnect(30, TimeUnit.SECONDS);

                if (!connectionResult.isSuccess()) {
                    return;
                }
            }

            if (mLastLocation == null)
                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            boolean autoLocation = config.getBoolean(Utility.KEY_WIDGET_WEATHER_AUTO_LOCATION);
            String location = config.getString(Utility.KEY_WIDGET_WEATHER_LOCATION, "");

            Time currentTime = new Time();
            currentTime.setToNow();

            Time lastTime = new Time();
            lastTime.set(config.getLong(Utility.KEY_WIDGET_WEATHER_DATA_DATETIME));

            long hours = TimeUnit.HOURS.toMillis(3);
            long lastHours = lastTime.toMillis(true) + hours;

            boolean timeToUpdate = currentTime.toMillis(true) >= lastHours;

            String dataString = "";

            if (autoLocation && mLastLocation != null) {
                location = mLastLocation.getLatitude() + "," + mLastLocation.getLongitude();
            } else {
                String[] split = location.split("/,/");

                StringBuilder builder = new StringBuilder();
                for (String aSplit : split) {
                    builder.append(aSplit.trim().replace(" ", "+")).append(",");
                }

                if (builder.length() > 0)
                    location = builder.subSequence(0, builder.length() - 1).toString();
            }

            if (!location.isEmpty() && timeToUpdate) {
                // Call API endpoint with apiURL
                HttpURLConnection con = null;
                InputStream is = null;

                // TODO: get weather from service (use async task?)
                try {
                    String apiURL = "http://api.worldweatheronline.com/free/v2/weather.ashx?key=30566152af44998e55196aeacb2e1&format=json&fx=no&q=";
                    con = (HttpURLConnection) (new URL(apiURL + location)).openConnection();
                    con.setRequestMethod("GET");
                    con.setDoInput(true);
                    con.setDoOutput(true);
                    con.connect();

                    // Let's read the response
                    StringBuilder builder = new StringBuilder();
                    is = con.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    String line;
                    while ((line = br.readLine()) != null)
                        builder.append(line + "\r\n");

                    is.close();
                    con.disconnect();
                    dataString = builder.toString();
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    try {
                        is.close();
                    } catch (Throwable t) {
                    }
                    try {
                        con.disconnect();
                    } catch (Throwable t) {
                    }
                }

                config.putLong(Utility.KEY_WIDGET_WEATHER_DATA_DATETIME, currentTime.toMillis(true));

                // send data to message function
                try {
                    sendWeatherDataMessage(messageEvent.getSourceNodeId(), config, new WeatherData(dataString));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }

    private void sendWeatherDataMessage(String nodeId, final DataMap config, WeatherData weatherData) {
        config.putInt(Utility.KEY_WIDGET_WEATHER_DATA_TEMPERATURE_C, weatherData.getTemperatureC());
        config.putInt(Utility.KEY_WIDGET_WEATHER_DATA_TEMPERATURE_F, weatherData.getTemperatureF());
        config.putInt(Utility.KEY_WIDGET_WEATHER_DATA_CODE, weatherData.getCode());
        config.putString(Utility.KEY_WIDGET_WEATHER_LOCATION, weatherData.getLocation());

        if (mGoogleApiClient.isConnected()) {
            byte[] rawData = config.toByteArray();
            Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, Utility.PATH_DIGILOGUE_SETTINGS, rawData);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        mGoogleApiClient.disconnect();
    }
}

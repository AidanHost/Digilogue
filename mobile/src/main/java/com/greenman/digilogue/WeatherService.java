package com.greenman.digilogue;

import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

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
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(CompanionUtil.PATH_DIGILOGUE_WEATHER_DATA)) {
            // TODO: get weather from service (use async task?)

            // TODO: get weather config data from DataMap
            // TODO: set defaults

            boolean autoLocation = true;
            String manualLocation = "Cape Town, ZA";
            String location;
            String xmlString = "";

            if (autoLocation) {
                String latitude = "0";
                String longitude = "0";

                if (mLastLocation != null) {
                    latitude = String.valueOf(mLastLocation.getLatitude());
                    longitude = String.valueOf(mLastLocation.getLongitude());
                }

                location = latitude + "," + longitude;
            } else {
                location = manualLocation;
            }

            // Call API endpoint with apiURL
            HttpURLConnection con = null;
            InputStream is = null;

            // TODO: cache result
            try {
                String apiURL = "http://api.worldweatheronline.com/free/v2/weather.ashx?key=30566152af44998e55196aeacb2e1&fx=no&q=";
                con = (HttpURLConnection) ( new URL(apiURL + location)).openConnection();
                con.setRequestMethod("GET");
                con.setDoInput(true);
                con.setDoOutput(true);
                con.connect();

                // Let's read the response
                StringBuffer buffer = new StringBuffer();
                is = con.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line = null;
                while (  (line = br.readLine()) != null )
                    buffer.append(line + "\r\n");

                is.close();
                con.disconnect();
                //return buffer.toString();
                xmlString = buffer.toString();
            }
            catch(Throwable t) {
                t.printStackTrace();
            }
            finally {
                try { is.close(); } catch(Throwable t) {}
                try { con.disconnect(); } catch(Throwable t) {}
            }

            // TODO: parse xmlString and send data to message function


            sendWeatherDataMessage();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void sendWeatherDataMessage() {
        DataMap config = new DataMap();
        config.putInt(CompanionUtil.KEY_WIDGET_WEATHER_DATA_TEMPERATURE, 24);
        config.putInt(CompanionUtil.KEY_WIDGET_WEATHER_DATA_CODE, 116);
            /*config.putString(CompanionUtil.KEY_BACKGROUND_COLOUR, background.getSelectedItem().toString());
            config.putString(CompanionUtil.KEY_MIDDLE_COLOUR, middle.getSelectedItem().toString());
            config.putString(CompanionUtil.KEY_FOREGROUND_COLOUR, foreground.getSelectedItem().toString());
            config.putString(CompanionUtil.KEY_ACCENT_COLOUR, accent.getSelectedItem().toString());
            config.putString(CompanionUtil.KEY_WIDGET_WEATHER_LOCATION, widget_weather_text_location.getText().toString());
            config.putBoolean(CompanionUtil.KEY_WIDGET_SHOW_WEATHER, widget_show_weather.isChecked());
            config.putBoolean(CompanionUtil.KEY_WIDGET_WEATHER_FAHRENHEIT, widget_weather_fahrenheit.isChecked());
            config.putBoolean(CompanionUtil.KEY_WIDGET_WEATHER_AUTO_LOCATION, widget_weather_auto_location.isChecked());

            int frequency = widget_weather_frequency.getProgress();

            if (frequency < 1)
                frequency = 1;

            config.putLong(CompanionUtil.KEY_WIDGET_WEATHER_UPDATE_FREQUENCY, frequency);*/
        final byte[] rawData = config.toByteArray();

        //Wearable.MessageApi.sendMessage(mGoogleApiClient, mPeerId, CompanionUtil.PATH_DIGILOGUE_SETTINGS, rawData);

            /*final ResultCallback<MessageApi.SendMessageResult> callback = new ResultCallback<MessageApi.SendMessageResult>() {
                @Override
                public void onResult(MessageApi.SendMessageResult sendMessageResult) {

                }
            };*/

        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(
                new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(NodeApi.GetConnectedNodesResult result) {
                        for (Node node : result.getNodes()) {
                            Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), CompanionUtil.PATH_DIGILOGUE_WEATHER_DATA, rawData);
                        }
                    }
                });
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}

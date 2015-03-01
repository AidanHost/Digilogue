package com.greenman.digilogue;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.wearable.companion.WatchFaceCompanion;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.greenman.common.Utility;
import com.greenman.digilogue.view.PreviewWatchFace;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class DigilogueConfigActivity extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<DataApi.DataItemResult>, ColoursFragment.OnFragmentInteractionListener, TogglesFragment.OnFragmentInteractionListener, WeatherFragment.OnFragmentInteractionListener {
    private static final String TAG = "DigilogueConfigActivity";

    //region variables
    private DataMap config;

    private GoogleApiClient mGoogleApiClient;
    private String mPeerId;

    private PreviewWatchFace preview;
    private ViewPager pager;

    private static ColoursFragment coloursFragment;
    private static TogglesFragment togglesFragment;
    private static WeatherFragment weatherFragment;

    private String backgroundColour = Utility.COLOUR_NAME_DEFAULT_BACKGROUND;
    private String middleColour = Utility.COLOUR_NAME_DEFAULT_MIDDLE;
    private String foregroundColour = Utility.COLOUR_NAME_DEFAULT_FOREGROUND;
    private String accentColour = Utility.COLOUR_NAME_DEFAULT_ACCENT;

    private boolean toggleAmPm = Utility.CONFIG_DEFAULT_TOGGLE_AM_PM;
    private boolean toggleDayDate = Utility.CONFIG_DEFAULT_TOGGLE_DAY_DATE;
    private boolean toggleDimColour = Utility.CONFIG_DEFAULT_TOGGLE_DIM_COLOUR;
    private boolean toggleSolidText = Utility.CONFIG_DEFAULT_TOGGLE_SOLID_TEXT;
    private boolean toggleDigital = Utility.CONFIG_DEFAULT_TOGGLE_DIGITAL;
    private boolean toggleAnalogue = Utility.CONFIG_DEFAULT_TOGGLE_ANALOGUE;
    private boolean toggleBattery = Utility.CONFIG_DEFAULT_TOGGLE_BATTERY;
    private boolean toggleFixChin = Utility.CONFIG_DEFAULT_TOGGLE_FIX_CHIN;
    private boolean toggleDial = Utility.CONFIG_DEFAULT_TOGGLE_DIAL;
    private boolean toggleWeather = Utility.CONFIG_DEFAULT_TOGGLE_WEATHER;

    private boolean fahrenheit = Utility.CONFIG_DEFAULT_WIDGET_WEATHER_FAHRENHEIT;
    private boolean autoLocation = Utility.CONFIG_DEFAULT_WIDGET_WEATHER_AUTO_LOCATION;
    private String weatherData = "";
    private String manualLocation = "";
    private TabAdapter mAdapter;
    //endregion

    //region Overrides
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_digilogue_config);

        preview = (PreviewWatchFace) findViewById(R.id.preview);
        pager = (ViewPager) findViewById(R.id.pager);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mPeerId = getIntent().getStringExtra(WatchFaceCompanion.EXTRA_PEER_ID);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();

                return true;
            case R.id.button_update:
                sendConfigUpdateMessage();
                return true;
            case R.id.button_reset:
                init(null);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override // GoogleApiClient.ConnectionCallbacks
    public void onConnected(Bundle connectionHint) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnected: " + connectionHint);
        }

        if (mPeerId != null) {
            Uri.Builder builder = new Uri.Builder();
            Uri uri = builder.scheme("wear").path(Utility.PATH_DIGILOGUE_SETTINGS).authority(mPeerId).build();
            Wearable.DataApi.getDataItem(mGoogleApiClient, uri).setResultCallback(this);
        } else {
            displayNoConnectedDeviceDialog();
        }
    }

    @Override // ResultCallback<DataApi.DataItemResult>
    public void onResult(DataApi.DataItemResult dataItemResult) {
        if (dataItemResult.getStatus().isSuccess() && dataItemResult.getDataItem() != null) {
            DataItem configDataItem = dataItemResult.getDataItem();
            DataMapItem dataMapItem = DataMapItem.fromDataItem(configDataItem);
            DataMap config = dataMapItem.getDataMap();
            this.config = config;
            init(config);
        } else {
            // If DataItem with the current config can't be retrieved, select the default items on
            // each picker.
            init(null);
        }
    }

    @Override // GoogleApiClient.ConnectionCallbacks
    public void onConnectionSuspended(int cause) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnectionSuspended: " + cause);
        }
    }

    @Override // GoogleApiClient.OnConnectionFailedListener
    public void onConnectionFailed(ConnectionResult result) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnectionFailed: " + result);
        }
    }
    //endregion

    private void displayNoConnectedDeviceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String messageText = getResources().getString(R.string.title_no_device_connected);
        String okText = getResources().getString(R.string.ok_no_device_connected);
        builder.setMessage(messageText)
                .setCancelable(false)
                .setPositiveButton(okText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void fetchToggles(DataMap config) {
        toggleAmPm = config.containsKey(Utility.KEY_TOGGLE_AM_PM) && config.getBoolean(Utility.KEY_TOGGLE_AM_PM, false);
        toggleAnalogue = config.containsKey(Utility.KEY_TOGGLE_ANALOGUE) && config.getBoolean(Utility.KEY_TOGGLE_ANALOGUE, true) || !config.containsKey(Utility.KEY_TOGGLE_ANALOGUE);
        toggleDigital = config.containsKey(Utility.KEY_TOGGLE_DIGITAL) && config.getBoolean(Utility.KEY_TOGGLE_DIGITAL, true) || !config.containsKey(Utility.KEY_TOGGLE_DIGITAL);
        toggleDayDate = config.containsKey(Utility.KEY_TOGGLE_DAY_DATE) && config.getBoolean(Utility.KEY_TOGGLE_DAY_DATE, true) || !config.containsKey(Utility.KEY_TOGGLE_DAY_DATE);
        toggleBattery = config.containsKey(Utility.KEY_TOGGLE_BATTERY) && config.getBoolean(Utility.KEY_TOGGLE_BATTERY, true) || !config.containsKey(Utility.KEY_TOGGLE_BATTERY);
        toggleDimColour = config.containsKey(Utility.KEY_TOGGLE_DIM_COLOUR) && config.getBoolean(Utility.KEY_TOGGLE_DIM_COLOUR, true) || !config.containsKey(Utility.KEY_TOGGLE_DIM_COLOUR);
        toggleSolidText = config.containsKey(Utility.KEY_TOGGLE_SOLID_TEXT) && config.getBoolean(Utility.KEY_TOGGLE_SOLID_TEXT, false);
        toggleFixChin = config.containsKey(Utility.KEY_TOGGLE_FIX_CHIN) && config.getBoolean(Utility.KEY_TOGGLE_FIX_CHIN, false);
        toggleDial = config.containsKey(Utility.KEY_TOGGLE_DRAW_DIAL) && config.getBoolean(Utility.KEY_TOGGLE_DRAW_DIAL, false);

        toggleWeather = config.containsKey(Utility.KEY_TOGGLE_WEATHER) && config.getBoolean(Utility.KEY_TOGGLE_WEATHER, false);
        autoLocation = (config.containsKey(Utility.KEY_WIDGET_WEATHER_AUTO_LOCATION) && config.getBoolean(Utility.KEY_WIDGET_WEATHER_AUTO_LOCATION, true) || !config.containsKey(Utility.KEY_WIDGET_WEATHER_AUTO_LOCATION));
        fahrenheit = config.containsKey(Utility.KEY_WIDGET_WEATHER_FAHRENHEIT) && config.getBoolean(Utility.KEY_WIDGET_WEATHER_FAHRENHEIT, false);
    }

    private void fetchWeatherData(DataMap config) {
        if (config.containsKey(Utility.KEY_WIDGET_WEATHER_DATA_DATETIME)) {
            Time lastTime = new Time();
            lastTime.set(config.getLong(Utility.KEY_WIDGET_WEATHER_DATA_DATETIME));

            weatherData += getString(R.string.last_time_updated) + lastTime.format(getString(R.string.time_format)) + "\n\n";

            Time nextTime = new Time();
            nextTime.set(lastTime.toMillis(true) + TimeUnit.HOURS.toMillis(Utility.REFRESH_WEATHER_DELAY_HOURS));
            weatherData += getString(R.string.next_update) + nextTime.format(getString(R.string.time_format)) + "\n\n";
        }

        if (config.containsKey(Utility.KEY_WIDGET_WEATHER_DATA_CODE)) {
            weatherData += getString(R.string.condition) + getString(Utility.WeatherCodes.getStringResourceByCode(config.getInt(Utility.KEY_WIDGET_WEATHER_DATA_CODE))) + "\n\n";
        }

        if (config.containsKey(Utility.KEY_WIDGET_WEATHER_DATA_ISDAYTIME)) {
            weatherData += (config.getBoolean(Utility.KEY_WIDGET_WEATHER_DATA_ISDAYTIME) ? getString(R.string.day) : getString(R.string.night)) + "\n\n";
        }

        if (config.containsKey(Utility.KEY_WIDGET_WEATHER_DATA_TEMPERATURE_C) && config.containsKey(Utility.KEY_WIDGET_WEATHER_DATA_TEMPERATURE_F)) {
            weatherData += config.getInt(Utility.KEY_WIDGET_WEATHER_DATA_TEMPERATURE_C) + getString(R.string.degrees) + "C / " +
                    config.getInt(Utility.KEY_WIDGET_WEATHER_DATA_TEMPERATURE_F) + getString(R.string.degrees) + "F\n\n";
        }

        if (config.containsKey(Utility.KEY_WIDGET_WEATHER_LOCATION)) {
            weatherData += getString(R.string.location) + config.getString(Utility.KEY_WIDGET_WEATHER_LOCATION);
        }

        if (!autoLocation) {
            if (config.containsKey(Utility.KEY_WIDGET_WEATHER_LOCATION))
                manualLocation = config.getString(Utility.KEY_WIDGET_WEATHER_LOCATION);
        }
    }

    private void fetchColours(DataMap config) {
        if (config.containsKey(Utility.KEY_BACKGROUND_COLOUR))
            backgroundColour = config.getString(Utility.KEY_BACKGROUND_COLOUR);

        if (config.containsKey(Utility.KEY_MIDDLE_COLOUR))
            middleColour = config.getString(Utility.KEY_MIDDLE_COLOUR);

        if (config.containsKey(Utility.KEY_FOREGROUND_COLOUR))
            foregroundColour = config.getString(Utility.KEY_FOREGROUND_COLOUR);

        if (config.containsKey(Utility.KEY_ACCENT_COLOUR))
            accentColour = config.getString(Utility.KEY_ACCENT_COLOUR);
    }

    private void init(DataMap config) {
        try {

            if (config != null) {
                fetchColours(config);

                fetchToggles(config);

                fetchWeatherData(config);

                preview.setConfig(config);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        setUpFragments();

        Toast.makeText(getBaseContext(), getString(R.string.preview_tap), Toast.LENGTH_LONG).show();
    }

    private void setUpFragments() {
        coloursFragment = ColoursFragment.newInstance(backgroundColour, middleColour, foregroundColour, accentColour);
        togglesFragment = TogglesFragment.newInstance(toggleAmPm, toggleDayDate, toggleDimColour, toggleSolidText, toggleDigital, toggleAnalogue, toggleBattery, toggleFixChin, toggleDial, toggleWeather);
        weatherFragment = WeatherFragment.newInstance(autoLocation, fahrenheit, manualLocation, weatherData);

        ArrayList<String> tabs = new ArrayList<>();
        tabs.add(getBaseContext().getString(R.string.colour_title));
        tabs.add(getBaseContext().getString(R.string.toggles_title));
        tabs.add(getBaseContext().getString(R.string.weather_title));


        mAdapter = new TabAdapter(getSupportFragmentManager(), tabs);

        pager.setAdapter(mAdapter);
    }

    private void sendConfigUpdateMessage() {
        if (mPeerId != null) {
            updateConfig();

            byte[] rawData = config.toByteArray();

            Wearable.MessageApi.sendMessage(mGoogleApiClient, mPeerId, Utility.PATH_DIGILOGUE_SETTINGS, rawData);
        }
    }

    private void updateConfig() {
        if (config == null)
            config = new DataMap();

        config.putString(Utility.KEY_BACKGROUND_COLOUR, backgroundColour);
        config.putString(Utility.KEY_MIDDLE_COLOUR, middleColour);
        config.putString(Utility.KEY_FOREGROUND_COLOUR, foregroundColour);
        config.putString(Utility.KEY_ACCENT_COLOUR, accentColour);

        config.putBoolean(Utility.KEY_TOGGLE_AM_PM, toggleAmPm);
        config.putBoolean(Utility.KEY_TOGGLE_DAY_DATE, toggleDayDate);
        config.putBoolean(Utility.KEY_TOGGLE_DIM_COLOUR, toggleDimColour);
        config.putBoolean(Utility.KEY_TOGGLE_SOLID_TEXT, toggleSolidText);
        config.putBoolean(Utility.KEY_TOGGLE_DIGITAL, toggleDigital);
        config.putBoolean(Utility.KEY_TOGGLE_ANALOGUE, toggleAnalogue);
        config.putBoolean(Utility.KEY_TOGGLE_BATTERY, toggleBattery);
        config.putBoolean(Utility.KEY_TOGGLE_FIX_CHIN, toggleFixChin);
        config.putBoolean(Utility.KEY_TOGGLE_DRAW_DIAL, toggleDial);
        config.putBoolean(Utility.KEY_TOGGLE_WEATHER, toggleWeather);

        config.putBoolean(Utility.KEY_WIDGET_WEATHER_FAHRENHEIT, fahrenheit);
        config.putBoolean(Utility.KEY_WIDGET_WEATHER_AUTO_LOCATION, autoLocation);

        if (manualLocation.length() > 0 && !autoLocation)
            config.putString(Utility.KEY_WIDGET_WEATHER_LOCATION, manualLocation);

        preview.setConfig(config);
    }

    @Override
    public void onColourSelected(Bundle colours) {
        if (colours == null)
            return;

        if (colours.containsKey(ColoursFragment.ARG_BACKGROUND))
            backgroundColour = colours.getString(ColoursFragment.ARG_BACKGROUND);

        if (colours.containsKey(ColoursFragment.ARG_BACKGROUND))
            middleColour = colours.getString(ColoursFragment.ARG_MIDDLE);

        if (colours.containsKey(ColoursFragment.ARG_BACKGROUND))
            foregroundColour = colours.getString(ColoursFragment.ARG_FOREGROUND);

        if (colours.containsKey(ColoursFragment.ARG_BACKGROUND))
            accentColour = colours.getString(ColoursFragment.ARG_ACCENT);

        updateConfig();
    }

    @Override
    public void onToggleChanged(Bundle toggles) {
        if (toggles == null)
            return;

        //mAdapter.notifyDataSetChanged();

        if (toggles.containsKey(TogglesFragment.ARG_AM_PM))
            toggleAmPm = toggles.getBoolean(TogglesFragment.ARG_AM_PM);

        if (toggles.containsKey(TogglesFragment.ARG_DAY_DATE))
            toggleDayDate = toggles.getBoolean(TogglesFragment.ARG_DAY_DATE);

        if (toggles.containsKey(TogglesFragment.ARG_DIM_COLOUR))
            toggleDimColour = toggles.getBoolean(TogglesFragment.ARG_DIM_COLOUR);

        if (toggles.containsKey(TogglesFragment.ARG_SOLID_TEXT))
            toggleSolidText = toggles.getBoolean(TogglesFragment.ARG_SOLID_TEXT);

        if (toggles.containsKey(TogglesFragment.ARG_DIGITAL))
            toggleDigital = toggles.getBoolean(TogglesFragment.ARG_DIGITAL);

        if (toggles.containsKey(TogglesFragment.ARG_ANALOGUE))
            toggleAnalogue = toggles.getBoolean(TogglesFragment.ARG_ANALOGUE);

        if (toggles.containsKey(TogglesFragment.ARG_BATTERY))
            toggleBattery = toggles.getBoolean(TogglesFragment.ARG_BATTERY);

        if (toggles.containsKey(TogglesFragment.ARG_FIX_CHIN))
            toggleFixChin = toggles.getBoolean(TogglesFragment.ARG_FIX_CHIN);

        if (toggles.containsKey(TogglesFragment.ARG_DIAL))
            toggleDial = toggles.getBoolean(TogglesFragment.ARG_DIAL);

        if (toggles.containsKey(TogglesFragment.ARG_WEATHER))
            toggleWeather = toggles.getBoolean(TogglesFragment.ARG_WEATHER);

        updateConfig();
    }

    @Override
    public void onWeatherChanged(Bundle weather) {
        if (weather == null)
            return;

        if (weather.containsKey(WeatherFragment.ARG_LOCATION))
            manualLocation = weather.getString(WeatherFragment.ARG_LOCATION);

        if (weather.containsKey(WeatherFragment.ARG_AUTO_LOCATION))
            autoLocation = weather.getBoolean(WeatherFragment.ARG_AUTO_LOCATION);

        if (weather.containsKey(WeatherFragment.ARG_FAHRENHEIT))
            fahrenheit = weather.getBoolean(WeatherFragment.ARG_FAHRENHEIT);

        updateConfig();
    }

    public static class TabAdapter extends FragmentPagerAdapter {
        private ArrayList<String> tabs;
        public TabAdapter(FragmentManager fm, ArrayList<String> tabs) {
            super(fm);

            this.tabs = tabs;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return tabs.get(position);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return coloursFragment;
                case 1:
                    return togglesFragment;
                case 2:
                    return weatherFragment;

                default:
                    return  new Fragment();
            }
        }
    }

}

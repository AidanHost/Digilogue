package com.greenman.digilogue;

import android.app.AlertDialog;
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
import android.view.View;

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

import java.util.concurrent.TimeUnit;

public class DigilogueConfigActivity extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<DataApi.DataItemResult>, ColoursFragment.OnFragmentInteractionListener, TogglesFragment.OnFragmentInteractionListener, WeatherFragment.OnFragmentInteractionListener {
    private static final String TAG = "DigilogueConfigActivity";

    //region variables
    private static final String KEY_CONFIG = "CONFIG";

    private static ColoursFragment coloursFragment;
    private static TogglesFragment togglesFragment;
    private static WeatherFragment weatherFragment;
    private static String[] tabs = new String[0];

    private DataMap config;

    private GoogleApiClient mGoogleApiClient;
    private String mPeerId;

    private PreviewWatchFace preview;
    private ViewPager pager;

    public String backgroundColour = Utility.COLOUR_NAME_DEFAULT_BACKGROUND;
    public String middleColour = Utility.COLOUR_NAME_DEFAULT_MIDDLE;
    public String foregroundColour = Utility.COLOUR_NAME_DEFAULT_FOREGROUND;
    public String accentColour = Utility.COLOUR_NAME_DEFAULT_ACCENT;

    public boolean toggleAnalogue = Utility.CONFIG_DEFAULT_TOGGLE_ANALOGUE;
    public boolean toggleDigital = Utility.CONFIG_DEFAULT_TOGGLE_DIGITAL;
    public boolean toggleBattery = Utility.CONFIG_DEFAULT_TOGGLE_BATTERY;
    public boolean toggleDayDate = Utility.CONFIG_DEFAULT_TOGGLE_DAY_DATE;
    public boolean toggleAmPm = Utility.CONFIG_DEFAULT_TOGGLE_AM_PM;
    public boolean toggleDial = Utility.CONFIG_DEFAULT_TOGGLE_DIAL;
    public boolean toggleFixChin = Utility.CONFIG_DEFAULT_TOGGLE_FIX_CHIN;
    public int analogueElementSize = 100;
    public int digitalElementSize = 100;
    public boolean toggleDimColour = Utility.CONFIG_DEFAULT_TOGGLE_DIM_COLOUR;
    public boolean toggleSolidText = Utility.CONFIG_DEFAULT_TOGGLE_SOLID_TEXT;

    public boolean toggleWeather = Utility.CONFIG_DEFAULT_TOGGLE_WEATHER;
    public boolean fahrenheit = Utility.CONFIG_DEFAULT_WIDGET_WEATHER_FAHRENHEIT;
    public boolean autoLocation = Utility.CONFIG_DEFAULT_WIDGET_WEATHER_AUTO_LOCATION;
    public String weatherData = "";
    public String manualLocation = "";

    //endregion

    //region Overrides
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_digilogue_config);

        preview = (PreviewWatchFace) findViewById(R.id.preview);
        preview.setHintText(getString(R.string.preview_tap), true);
        pager = (ViewPager) findViewById(R.id.pager);
        pager.setOffscreenPageLimit(tabs.length - 1);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mPeerId = getIntent().getStringExtra(WatchFaceCompanion.EXTRA_PEER_ID);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        tabs = getResources().getStringArray(R.array.tab_array);

        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_CONFIG))
            config = DataMap.fromByteArray(savedInstanceState.getByteArray(KEY_CONFIG));

        coloursFragment = new ColoursFragment();
        togglesFragment = new TogglesFragment();
        weatherFragment = new WeatherFragment();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (config != null)
            outState.putByteArray(KEY_CONFIG, config.toByteArray());
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
                resetConfig();
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

        if (config == null) {
            if (mPeerId != null) {
                Uri.Builder builder = new Uri.Builder();
                Uri uri = builder.scheme("wear").path(Utility.PATH_DIGILOGUE_SETTINGS).authority(mPeerId).build();
                Wearable.DataApi.getDataItem(mGoogleApiClient, uri).setResultCallback(this);
            } else {
                displayNoConnectedDeviceDialog();
            }
        } else {
            init();
        }
    }

    @Override // ResultCallback<DataApi.DataItemResult>
    public void onResult(DataApi.DataItemResult dataItemResult) {
        if (dataItemResult.getStatus().isSuccess() && dataItemResult.getDataItem() != null) {
            DataItem configDataItem = dataItemResult.getDataItem();
            DataMapItem dataMapItem = DataMapItem.fromDataItem(configDataItem);
            this.config = dataMapItem.getDataMap();
        } else {
            // If DataItem with the current config can't be retrieved, select the default items on
            // each picker.
            config = defaultDataMap();
        }

        init();
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

    @Override
    public void onColourSelected(Bundle colours) {
        if (colours == null)
            return;

        if (colours.containsKey(ColoursFragment.ARG_BACKGROUND))
            backgroundColour = colours.getString(ColoursFragment.ARG_BACKGROUND);

        if (colours.containsKey(ColoursFragment.ARG_MIDDLE))
            middleColour = colours.getString(ColoursFragment.ARG_MIDDLE);

        if (colours.containsKey(ColoursFragment.ARG_FOREGROUND))
            foregroundColour = colours.getString(ColoursFragment.ARG_FOREGROUND);

        if (colours.containsKey(ColoursFragment.ARG_ACCENT))
            accentColour = colours.getString(ColoursFragment.ARG_ACCENT);

        try {
            config.putString(Utility.KEY_BACKGROUND_COLOUR, backgroundColour);
            config.putString(Utility.KEY_MIDDLE_COLOUR, middleColour);
            config.putString(Utility.KEY_FOREGROUND_COLOUR, foregroundColour);
            config.putString(Utility.KEY_ACCENT_COLOUR, accentColour);
            preview.setConfig(config);
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public void onToggleChanged(Bundle toggles) {
        if (toggles == null)
            return;

        if (toggles.containsKey(TogglesFragment.ARG_ANALOGUE))
            toggleAnalogue = toggles.getBoolean(TogglesFragment.ARG_ANALOGUE);

        if (toggles.containsKey(TogglesFragment.ARG_DIGITAL))
            toggleDigital = toggles.getBoolean(TogglesFragment.ARG_DIGITAL);

        if (toggles.containsKey(TogglesFragment.ARG_BATTERY))
            toggleBattery = toggles.getBoolean(TogglesFragment.ARG_BATTERY);

        if (toggles.containsKey(TogglesFragment.ARG_DAY_DATE))
            toggleDayDate = toggles.getBoolean(TogglesFragment.ARG_DAY_DATE);

        if (toggles.containsKey(TogglesFragment.ARG_AM_PM))
            toggleAmPm = toggles.getBoolean(TogglesFragment.ARG_AM_PM);

        if (toggles.containsKey(TogglesFragment.ARG_DIAL))
            toggleDial = toggles.getBoolean(TogglesFragment.ARG_DIAL);

        if (toggles.containsKey(TogglesFragment.ARG_FIX_CHIN))
            toggleFixChin = toggles.getBoolean(TogglesFragment.ARG_FIX_CHIN);

        if (toggles.containsKey(TogglesFragment.ARG_ANALOGUE_ELEMENT_SIZE))
            analogueElementSize = toggles.getInt(TogglesFragment.ARG_ANALOGUE_ELEMENT_SIZE);

        if (toggles.containsKey(TogglesFragment.ARG_DIGITAL_ELEMENT_SIZE))
            digitalElementSize = toggles.getInt(TogglesFragment.ARG_DIGITAL_ELEMENT_SIZE);

        if (toggles.containsKey(TogglesFragment.ARG_DIM_COLOUR))
            toggleDimColour = toggles.getBoolean(TogglesFragment.ARG_DIM_COLOUR);

        if (toggles.containsKey(TogglesFragment.ARG_SOLID_TEXT))
            toggleSolidText = toggles.getBoolean(TogglesFragment.ARG_SOLID_TEXT);

        try {
            config.putBoolean(Utility.KEY_TOGGLE_ANALOGUE, toggleAnalogue);
            config.putBoolean(Utility.KEY_TOGGLE_DIGITAL, toggleDigital);
            config.putBoolean(Utility.KEY_TOGGLE_BATTERY, toggleBattery);
            config.putBoolean(Utility.KEY_TOGGLE_DAY_DATE, toggleDayDate);
            config.putBoolean(Utility.KEY_TOGGLE_AM_PM, toggleAmPm);
            config.putBoolean(Utility.KEY_TOGGLE_DRAW_DIAL, toggleDial);
            config.putBoolean(Utility.KEY_TOGGLE_FIX_CHIN, toggleFixChin);
            config.putInt(Utility.KEY_ANALOGUE_ELEMENT_SIZE, analogueElementSize);
            config.putInt(Utility.KEY_DIGITAL_ELEMENT_SIZE, digitalElementSize);
            config.putBoolean(Utility.KEY_TOGGLE_DIM_COLOUR, toggleDimColour);
            config.putBoolean(Utility.KEY_TOGGLE_SOLID_TEXT, toggleSolidText);
            preview.setConfig(config);
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public void onWeatherChanged(Bundle weather) {
        if (weather == null)
            return;

        if (weather.containsKey(WeatherFragment.ARG_TOGGLE_WEATHER))
            toggleWeather = weather.getBoolean(WeatherFragment.ARG_TOGGLE_WEATHER);

        if (weather.containsKey(WeatherFragment.ARG_LOCATION))
            manualLocation = weather.getString(WeatherFragment.ARG_LOCATION);

        if (weather.containsKey(WeatherFragment.ARG_AUTO_LOCATION))
            autoLocation = weather.getBoolean(WeatherFragment.ARG_AUTO_LOCATION);

        if (weather.containsKey(WeatherFragment.ARG_FAHRENHEIT))
            fahrenheit = weather.getBoolean(WeatherFragment.ARG_FAHRENHEIT);

        try {
            config.putBoolean(Utility.KEY_TOGGLE_WEATHER, toggleWeather);
            config.putBoolean(Utility.KEY_WIDGET_WEATHER_FAHRENHEIT, fahrenheit);
            config.putBoolean(Utility.KEY_WIDGET_WEATHER_AUTO_LOCATION, autoLocation);

            if (manualLocation.length() > 0 && !autoLocation)
                config.putString(Utility.KEY_WIDGET_WEATHER_LOCATION, manualLocation);

            preview.setConfig(config);
        } catch (Exception e) {
            // ignore
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
                        finish();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void fetchToggles() {
        toggleAnalogue = config.containsKey(Utility.KEY_TOGGLE_ANALOGUE) && config.getBoolean(Utility.KEY_TOGGLE_ANALOGUE, true) || !config.containsKey(Utility.KEY_TOGGLE_ANALOGUE);
        toggleDigital = config.containsKey(Utility.KEY_TOGGLE_DIGITAL) && config.getBoolean(Utility.KEY_TOGGLE_DIGITAL, true) || !config.containsKey(Utility.KEY_TOGGLE_DIGITAL);
        toggleBattery = config.containsKey(Utility.KEY_TOGGLE_BATTERY) && config.getBoolean(Utility.KEY_TOGGLE_BATTERY, true) || !config.containsKey(Utility.KEY_TOGGLE_BATTERY);
        toggleDayDate = config.containsKey(Utility.KEY_TOGGLE_DAY_DATE) && config.getBoolean(Utility.KEY_TOGGLE_DAY_DATE, true) || !config.containsKey(Utility.KEY_TOGGLE_DAY_DATE);
        toggleAmPm = config.containsKey(Utility.KEY_TOGGLE_AM_PM) && config.getBoolean(Utility.KEY_TOGGLE_AM_PM, false);
        toggleDial = config.containsKey(Utility.KEY_TOGGLE_DRAW_DIAL) && config.getBoolean(Utility.KEY_TOGGLE_DRAW_DIAL, false);
        toggleFixChin = config.containsKey(Utility.KEY_TOGGLE_FIX_CHIN) && config.getBoolean(Utility.KEY_TOGGLE_FIX_CHIN, false);
        analogueElementSize = config.containsKey(Utility.KEY_ANALOGUE_ELEMENT_SIZE) ? config.getInt(Utility.KEY_ANALOGUE_ELEMENT_SIZE) : 100;
        digitalElementSize = config.containsKey(Utility.KEY_DIGITAL_ELEMENT_SIZE) ? config.getInt(Utility.KEY_DIGITAL_ELEMENT_SIZE) : 100;
        toggleDimColour = config.containsKey(Utility.KEY_TOGGLE_DIM_COLOUR) && config.getBoolean(Utility.KEY_TOGGLE_DIM_COLOUR, true) || !config.containsKey(Utility.KEY_TOGGLE_DIM_COLOUR);
        toggleSolidText = config.containsKey(Utility.KEY_TOGGLE_SOLID_TEXT) && config.getBoolean(Utility.KEY_TOGGLE_SOLID_TEXT, false);

        try {
            togglesFragment = (TogglesFragment) getSupportFragmentManager().getFragments().get(1);
            togglesFragment.setAnalogue(toggleAnalogue);
            togglesFragment.setDigital(toggleDigital);
            togglesFragment.setBattery(toggleBattery);
            togglesFragment.setDayDate(toggleDayDate);
            togglesFragment.setAmPm(toggleAmPm);
            togglesFragment.setDial(toggleDial);
            togglesFragment.setFixChin(toggleFixChin);
            togglesFragment.setAnalogueElementSize(analogueElementSize);
            togglesFragment.setDigitalElementSize(digitalElementSize);
            togglesFragment.setDimColour(toggleDimColour);
            togglesFragment.setSolidText(toggleSolidText);
        } catch (Exception e) {
            // ignore
        }
    }

    private void fetchWeatherData() {
        toggleWeather = config.containsKey(Utility.KEY_TOGGLE_WEATHER) && config.getBoolean(Utility.KEY_TOGGLE_WEATHER, false);
        autoLocation = (config.containsKey(Utility.KEY_WIDGET_WEATHER_AUTO_LOCATION) && config.getBoolean(Utility.KEY_WIDGET_WEATHER_AUTO_LOCATION, true) || !config.containsKey(Utility.KEY_WIDGET_WEATHER_AUTO_LOCATION));
        fahrenheit = config.containsKey(Utility.KEY_WIDGET_WEATHER_FAHRENHEIT) && config.getBoolean(Utility.KEY_WIDGET_WEATHER_FAHRENHEIT, false);

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

        try {
            weatherFragment = (WeatherFragment) getSupportFragmentManager().getFragments().get(2);
            weatherFragment.setWeather(toggleWeather);
            weatherFragment.setAutoLocation(autoLocation);
            weatherFragment.setFahrenheit(fahrenheit);
            weatherFragment.setManualLocation(manualLocation);
            weatherFragment.setWeatherData(weatherData);
        } catch (Exception e) {
            // ignore
        }
    }

    private void fetchColours() {
        if (config.containsKey(Utility.KEY_BACKGROUND_COLOUR))
            backgroundColour = config.getString(Utility.KEY_BACKGROUND_COLOUR);

        if (config.containsKey(Utility.KEY_MIDDLE_COLOUR))
            middleColour = config.getString(Utility.KEY_MIDDLE_COLOUR);

        if (config.containsKey(Utility.KEY_FOREGROUND_COLOUR))
            foregroundColour = config.getString(Utility.KEY_FOREGROUND_COLOUR);

        if (config.containsKey(Utility.KEY_ACCENT_COLOUR))
            accentColour = config.getString(Utility.KEY_ACCENT_COLOUR);

        try {
            coloursFragment = (ColoursFragment) getSupportFragmentManager().getFragments().get(0);
            coloursFragment.setBackground(backgroundColour);
            coloursFragment.setMiddle(middleColour);
            coloursFragment.setForeground(foregroundColour);
            coloursFragment.setAccent(accentColour);
        } catch (Exception e) {
            // ignore
        }
    }

    private void assignMissingValues() {
        if (!config.containsKey(Utility.KEY_BACKGROUND_COLOUR))
            config.putString(Utility.KEY_BACKGROUND_COLOUR, Utility.COLOUR_NAME_DEFAULT_BACKGROUND);

        if (!config.containsKey(Utility.KEY_MIDDLE_COLOUR))
            config.putString(Utility.KEY_MIDDLE_COLOUR, Utility.COLOUR_NAME_DEFAULT_MIDDLE);

        if (!config.containsKey(Utility.KEY_FOREGROUND_COLOUR))
            config.putString(Utility.KEY_FOREGROUND_COLOUR, Utility.COLOUR_NAME_DEFAULT_FOREGROUND);

        if (!config.containsKey(Utility.KEY_ACCENT_COLOUR))
            config.putString(Utility.KEY_ACCENT_COLOUR, Utility.COLOUR_NAME_DEFAULT_ACCENT);


        if (!config.containsKey(Utility.KEY_TOGGLE_ANALOGUE))
            config.putBoolean(Utility.KEY_TOGGLE_ANALOGUE, Utility.CONFIG_DEFAULT_TOGGLE_ANALOGUE);

        if (!config.containsKey(Utility.KEY_TOGGLE_DIGITAL))
            config.putBoolean(Utility.KEY_TOGGLE_DIGITAL, Utility.CONFIG_DEFAULT_TOGGLE_DIGITAL);

        if (!config.containsKey(Utility.KEY_TOGGLE_BATTERY))
            config.putBoolean(Utility.KEY_TOGGLE_BATTERY, Utility.CONFIG_DEFAULT_TOGGLE_BATTERY);

        if (!config.containsKey(Utility.KEY_TOGGLE_DAY_DATE))
            config.putBoolean(Utility.KEY_TOGGLE_DAY_DATE, Utility.CONFIG_DEFAULT_TOGGLE_DAY_DATE);

        if (!config.containsKey(Utility.KEY_TOGGLE_AM_PM))
            config.putBoolean(Utility.KEY_TOGGLE_AM_PM, Utility.CONFIG_DEFAULT_TOGGLE_AM_PM);

        if (!config.containsKey(Utility.KEY_TOGGLE_DRAW_DIAL))
            config.putBoolean(Utility.KEY_TOGGLE_DRAW_DIAL, Utility.CONFIG_DEFAULT_TOGGLE_DIAL);

        if (!config.containsKey(Utility.KEY_TOGGLE_FIX_CHIN))
            config.putBoolean(Utility.KEY_TOGGLE_FIX_CHIN, Utility.CONFIG_DEFAULT_TOGGLE_FIX_CHIN);

        if (!config.containsKey(Utility.KEY_ANALOGUE_ELEMENT_SIZE))
            config.putInt(Utility.KEY_ANALOGUE_ELEMENT_SIZE, 100);

        if (!config.containsKey(Utility.KEY_DIGITAL_ELEMENT_SIZE))
            config.putInt(Utility.KEY_DIGITAL_ELEMENT_SIZE, 100);

        if (!config.containsKey(Utility.KEY_TOGGLE_DIM_COLOUR))
            config.putBoolean(Utility.KEY_TOGGLE_DIM_COLOUR, Utility.CONFIG_DEFAULT_TOGGLE_DIM_COLOUR);

        if (!config.containsKey(Utility.KEY_TOGGLE_SOLID_TEXT))
            config.putBoolean(Utility.KEY_TOGGLE_SOLID_TEXT, Utility.CONFIG_DEFAULT_TOGGLE_SOLID_TEXT);


        if (!config.containsKey(Utility.KEY_TOGGLE_WEATHER))
            config.putBoolean(Utility.KEY_TOGGLE_WEATHER, Utility.CONFIG_DEFAULT_TOGGLE_WEATHER);

        if (!config.containsKey(Utility.KEY_WIDGET_WEATHER_FAHRENHEIT))
            config.putBoolean(Utility.KEY_WIDGET_WEATHER_FAHRENHEIT, Utility.CONFIG_DEFAULT_WIDGET_WEATHER_FAHRENHEIT);

        if (!config.containsKey(Utility.KEY_WIDGET_WEATHER_AUTO_LOCATION))
            config.putBoolean(Utility.KEY_WIDGET_WEATHER_AUTO_LOCATION, Utility.CONFIG_DEFAULT_WIDGET_WEATHER_AUTO_LOCATION);

        if (!config.containsKey(Utility.KEY_WIDGET_WEATHER_LOCATION))
            config.putString(Utility.KEY_WIDGET_WEATHER_LOCATION, Utility.CONFIG_DEFAULT_WIDGET_WEATHER_LOCATION);
    }

    private void init() {
        assignMissingValues();

        fetchColours();
        fetchToggles();
        fetchWeatherData();
        preview.setConfig(config);
        preview.setVisibility(View.VISIBLE);

        pager.setAdapter(new TabAdapter(getSupportFragmentManager()));
    }

    private void sendConfigUpdateMessage() {
        if (mPeerId != null) {
            config.putString(Utility.KEY_BACKGROUND_COLOUR, backgroundColour);
            config.putString(Utility.KEY_MIDDLE_COLOUR, middleColour);
            config.putString(Utility.KEY_FOREGROUND_COLOUR, foregroundColour);
            config.putString(Utility.KEY_ACCENT_COLOUR, accentColour);

            config.putBoolean(Utility.KEY_TOGGLE_ANALOGUE, toggleAnalogue);
            config.putBoolean(Utility.KEY_TOGGLE_DIGITAL, toggleDigital);
            config.putBoolean(Utility.KEY_TOGGLE_BATTERY, toggleBattery);
            config.putBoolean(Utility.KEY_TOGGLE_DAY_DATE, toggleDayDate);
            config.putBoolean(Utility.KEY_TOGGLE_AM_PM, toggleAmPm);
            config.putBoolean(Utility.KEY_TOGGLE_DRAW_DIAL, toggleDial);
            config.putBoolean(Utility.KEY_TOGGLE_FIX_CHIN, toggleFixChin);

            config.putInt(Utility.KEY_ANALOGUE_ELEMENT_SIZE, analogueElementSize);
            config.putInt(Utility.KEY_DIGITAL_ELEMENT_SIZE, digitalElementSize);
            config.putBoolean(Utility.KEY_TOGGLE_DIM_COLOUR, toggleDimColour);
            config.putBoolean(Utility.KEY_TOGGLE_SOLID_TEXT, toggleSolidText);

            config.putBoolean(Utility.KEY_TOGGLE_WEATHER, toggleWeather);
            config.putBoolean(Utility.KEY_WIDGET_WEATHER_FAHRENHEIT, fahrenheit);
            config.putBoolean(Utility.KEY_WIDGET_WEATHER_AUTO_LOCATION, autoLocation);

            if (manualLocation.length() > 0 && !autoLocation)
                config.putString(Utility.KEY_WIDGET_WEATHER_LOCATION, manualLocation);

            byte[] rawData = config.toByteArray();

            Wearable.MessageApi.sendMessage(mGoogleApiClient, mPeerId, Utility.PATH_DIGILOGUE_SETTINGS, rawData);
        }
    }

    private DataMap defaultDataMap() {
        DataMap defaults = new DataMap();

        defaults.putString(Utility.KEY_BACKGROUND_COLOUR, Utility.COLOUR_NAME_DEFAULT_BACKGROUND);
        defaults.putString(Utility.KEY_MIDDLE_COLOUR, Utility.COLOUR_NAME_DEFAULT_MIDDLE);
        defaults.putString(Utility.KEY_FOREGROUND_COLOUR, Utility.COLOUR_NAME_DEFAULT_FOREGROUND);
        defaults.putString(Utility.KEY_ACCENT_COLOUR, Utility.COLOUR_NAME_DEFAULT_ACCENT);

        defaults.putBoolean(Utility.KEY_TOGGLE_ANALOGUE, Utility.CONFIG_DEFAULT_TOGGLE_ANALOGUE);
        defaults.putBoolean(Utility.KEY_TOGGLE_DIGITAL, Utility.CONFIG_DEFAULT_TOGGLE_DIGITAL);
        defaults.putBoolean(Utility.KEY_TOGGLE_BATTERY, Utility.CONFIG_DEFAULT_TOGGLE_BATTERY);
        defaults.putBoolean(Utility.KEY_TOGGLE_DAY_DATE, Utility.CONFIG_DEFAULT_TOGGLE_DAY_DATE);
        defaults.putBoolean(Utility.KEY_TOGGLE_AM_PM, Utility.CONFIG_DEFAULT_TOGGLE_AM_PM);
        defaults.putBoolean(Utility.KEY_TOGGLE_DRAW_DIAL, Utility.CONFIG_DEFAULT_TOGGLE_DIAL);
        defaults.putBoolean(Utility.KEY_TOGGLE_FIX_CHIN, Utility.CONFIG_DEFAULT_TOGGLE_FIX_CHIN);

        defaults.putInt(Utility.KEY_ANALOGUE_ELEMENT_SIZE, 100);
        defaults.putInt(Utility.KEY_DIGITAL_ELEMENT_SIZE, 100);
        defaults.putBoolean(Utility.KEY_TOGGLE_DIM_COLOUR, Utility.CONFIG_DEFAULT_TOGGLE_DIM_COLOUR);
        defaults.putBoolean(Utility.KEY_TOGGLE_SOLID_TEXT, Utility.CONFIG_DEFAULT_TOGGLE_SOLID_TEXT);

        defaults.putBoolean(Utility.KEY_TOGGLE_WEATHER, Utility.CONFIG_DEFAULT_TOGGLE_WEATHER);
        defaults.putBoolean(Utility.KEY_WIDGET_WEATHER_FAHRENHEIT, Utility.CONFIG_DEFAULT_WIDGET_WEATHER_FAHRENHEIT);
        defaults.putBoolean(Utility.KEY_WIDGET_WEATHER_AUTO_LOCATION, Utility.CONFIG_DEFAULT_WIDGET_WEATHER_AUTO_LOCATION);

        if (manualLocation.length() > 0 && !autoLocation)
            defaults.putString(Utility.KEY_WIDGET_WEATHER_LOCATION, Utility.CONFIG_DEFAULT_WIDGET_WEATHER_LOCATION);

        return defaults;
    }

    private void resetConfig() {
        backgroundColour = Utility.COLOUR_NAME_DEFAULT_BACKGROUND;
        middleColour = Utility.COLOUR_NAME_DEFAULT_MIDDLE;
        foregroundColour = Utility.COLOUR_NAME_DEFAULT_FOREGROUND;
        accentColour = Utility.COLOUR_NAME_DEFAULT_ACCENT;

        toggleAnalogue = Utility.CONFIG_DEFAULT_TOGGLE_ANALOGUE;
        toggleDigital = Utility.CONFIG_DEFAULT_TOGGLE_DIGITAL;
        toggleBattery = Utility.CONFIG_DEFAULT_TOGGLE_BATTERY;
        toggleDayDate = Utility.CONFIG_DEFAULT_TOGGLE_DAY_DATE;
        toggleAmPm = Utility.CONFIG_DEFAULT_TOGGLE_AM_PM;
        toggleDial = Utility.CONFIG_DEFAULT_TOGGLE_DIAL;
        toggleFixChin = Utility.CONFIG_DEFAULT_TOGGLE_FIX_CHIN;
        analogueElementSize = 100;
        digitalElementSize = 100;
        toggleDimColour = Utility.CONFIG_DEFAULT_TOGGLE_DIM_COLOUR;
        toggleSolidText = Utility.CONFIG_DEFAULT_TOGGLE_SOLID_TEXT;

        toggleWeather = Utility.CONFIG_DEFAULT_TOGGLE_WEATHER;
        fahrenheit = Utility.CONFIG_DEFAULT_WIDGET_WEATHER_FAHRENHEIT;
        autoLocation = Utility.CONFIG_DEFAULT_WIDGET_WEATHER_AUTO_LOCATION;
        manualLocation = "";

        config.putString(Utility.KEY_BACKGROUND_COLOUR, Utility.COLOUR_NAME_DEFAULT_BACKGROUND);
        config.putString(Utility.KEY_MIDDLE_COLOUR, Utility.COLOUR_NAME_DEFAULT_MIDDLE);
        config.putString(Utility.KEY_FOREGROUND_COLOUR, Utility.COLOUR_NAME_DEFAULT_FOREGROUND);
        config.putString(Utility.KEY_ACCENT_COLOUR, Utility.COLOUR_NAME_DEFAULT_ACCENT);

        config.putBoolean(Utility.KEY_TOGGLE_ANALOGUE, Utility.CONFIG_DEFAULT_TOGGLE_ANALOGUE);
        config.putBoolean(Utility.KEY_TOGGLE_DIGITAL, Utility.CONFIG_DEFAULT_TOGGLE_DIGITAL);
        config.putBoolean(Utility.KEY_TOGGLE_BATTERY, Utility.CONFIG_DEFAULT_TOGGLE_BATTERY);
        config.putBoolean(Utility.KEY_TOGGLE_DAY_DATE, Utility.CONFIG_DEFAULT_TOGGLE_DAY_DATE);
        config.putBoolean(Utility.KEY_TOGGLE_AM_PM, Utility.CONFIG_DEFAULT_TOGGLE_AM_PM);
        config.putBoolean(Utility.KEY_TOGGLE_DRAW_DIAL, Utility.CONFIG_DEFAULT_TOGGLE_DIAL);
        config.putBoolean(Utility.KEY_TOGGLE_FIX_CHIN, Utility.CONFIG_DEFAULT_TOGGLE_FIX_CHIN);
        config.putInt(Utility.KEY_ANALOGUE_ELEMENT_SIZE, analogueElementSize);
        config.putInt(Utility.KEY_DIGITAL_ELEMENT_SIZE, digitalElementSize);
        config.putBoolean(Utility.KEY_TOGGLE_DIM_COLOUR, Utility.CONFIG_DEFAULT_TOGGLE_DIM_COLOUR);
        config.putBoolean(Utility.KEY_TOGGLE_SOLID_TEXT, Utility.CONFIG_DEFAULT_TOGGLE_SOLID_TEXT);

        config.putBoolean(Utility.KEY_TOGGLE_WEATHER, Utility.CONFIG_DEFAULT_TOGGLE_WEATHER);
        config.putBoolean(Utility.KEY_WIDGET_WEATHER_FAHRENHEIT, Utility.CONFIG_DEFAULT_WIDGET_WEATHER_FAHRENHEIT);
        config.putBoolean(Utility.KEY_WIDGET_WEATHER_AUTO_LOCATION, Utility.CONFIG_DEFAULT_WIDGET_WEATHER_AUTO_LOCATION);

        if (manualLocation.length() > 0 && !autoLocation)
            config.putString(Utility.KEY_WIDGET_WEATHER_LOCATION, Utility.CONFIG_DEFAULT_WIDGET_WEATHER_LOCATION);

        preview.setConfig(config);

        try {
            coloursFragment = (ColoursFragment) getSupportFragmentManager().getFragments().get(0);
            coloursFragment.setBackground(backgroundColour);
            coloursFragment.setMiddle(middleColour);
            coloursFragment.setForeground(foregroundColour);
            coloursFragment.setAccent(accentColour);
        } catch (Exception e) {
            // ignore
        }

        try {
            togglesFragment = (TogglesFragment) getSupportFragmentManager().getFragments().get(1);
            togglesFragment.setAnalogue(toggleAnalogue);
            togglesFragment.setDigital(toggleDigital);
            togglesFragment.setBattery(toggleBattery);
            togglesFragment.setDayDate(toggleDayDate);
            togglesFragment.setAmPm(toggleAmPm);
            togglesFragment.setFixChin(toggleFixChin);
            togglesFragment.setDial(toggleDial);
            togglesFragment.setAnalogueElementSize(analogueElementSize);
            togglesFragment.setDigitalElementSize(digitalElementSize);
            togglesFragment.setDimColour(toggleDimColour);
            togglesFragment.setSolidText(toggleSolidText);
        } catch (Exception e) {
            // ignore
        }

        try {
            weatherFragment = (WeatherFragment) getSupportFragmentManager().getFragments().get(2);
            weatherFragment.setWeather(toggleWeather);
            weatherFragment.setAutoLocation(autoLocation);
            weatherFragment.setFahrenheit(fahrenheit);
            weatherFragment.setManualLocation(manualLocation);
            weatherFragment.setWeatherData(weatherData);
        } catch (Exception e) {
            // ignore
        }
    }

    public static class TabAdapter extends FragmentPagerAdapter {
        public TabAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return tabs.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (tabs.length <= 0 || position > tabs.length)
                return "";

            return tabs[position];
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
                    return new Fragment();
            }
        }
    }

}

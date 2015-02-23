package com.greenman.digilogue;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.wearable.companion.WatchFaceCompanion;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.greenman.common.Utility;

import java.util.concurrent.TimeUnit;

public class DigilogueConfigActivity extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<DataApi.DataItemResult> {
    private static final String TAG = "DigilogueConfigActivity";

    private DataMap config;

    private GoogleApiClient mGoogleApiClient;
    private String mPeerId;

    private Spinner background;
    private Spinner middle;
    private Spinner foreground;
    private Spinner accent;

    private EditText widget_weather_text_location;

    private CheckBox digital_format;
    private CheckBox widget_show_weather;
    private CheckBox widget_weather_fahrenheit;
    private CheckBox widget_weather_auto_location;

    private CheckBox toggle_day_date;
    private CheckBox toggle_dim_colour;
    private CheckBox toggle_solid_text;
    private CheckBox toggle_digital;
    private CheckBox toggle_analogue;
    private CheckBox toggle_battery;
    private CheckBox toggle_fix_chin;
    private CheckBox toggle_dial;

    private LinearLayout weather_data;
    private TextView widget_weather_text_data;
    private LinearLayout widget_weather_group;
    private LinearLayout location;

    private String backgroundColour = Utility.COLOUR_NAME_DEFAULT_BACKGROUND;
    private String middleColour = Utility.COLOUR_NAME_DEFAULT_MIDDLE;
    private String foregroundColour = Utility.COLOUR_NAME_DEFAULT_FOREGROUND;
    private String accentColour = Utility.COLOUR_NAME_DEFAULT_ACCENT;

    private boolean showWeather = Utility.CONFIG_DEFAULT_TOGGLE_WEATHER;
    private boolean fahrenheit = Utility.CONFIG_DEFAULT_WIDGET_WEATHER_FAHRENHEIT;
    private boolean autoLocation = Utility.CONFIG_DEFAULT_WIDGET_WEATHER_AUTO_LOCATION;
    private boolean toggleAmPm = Utility.CONFIG_DEFAULT_TOGGLE_AM_PM;
    private boolean toggleAnalogue = Utility.CONFIG_DEFAULT_TOGGLE_ANALOGUE;
    private boolean toggleDigital = Utility.CONFIG_DEFAULT_TOGGLE_DIGITAL;
    private boolean toggleDayDate = Utility.CONFIG_DEFAULT_TOGGLE_DAY_DATE;
    private boolean toggleBattery = Utility.CONFIG_DEFAULT_TOGGLE_BATTERY;
    private boolean toggleDimColour = Utility.CONFIG_DEFAULT_TOGGLE_DIM_COLOUR;
    private boolean toggleSolidText = Utility.CONFIG_DEFAULT_TOGGLE_SOLID_TEXT;
    private boolean toggleFixChin = Utility.CONFIG_DEFAULT_TOGGLE_FIX_CHIN;
    private boolean toggleDial = Utility.CONFIG_DEFAULT_TOGGLE_DRAW_DIAL;

    private String weatherData = "";
    private String locationText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_digilogue_config);

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

    private void fetchControls() {
        background = (Spinner) findViewById(R.id.background);
        middle = (Spinner) findViewById(R.id.middle);
        foreground = (Spinner) findViewById(R.id.foreground);
        accent = (Spinner) findViewById(R.id.accent);

        weather_data = (LinearLayout) findViewById(R.id.weather_data);
        widget_weather_text_data = (TextView) findViewById(R.id.widget_weather_text_data);
        digital_format = (CheckBox) findViewById(R.id.digital_format);
        widget_show_weather = (CheckBox) findViewById(R.id.widget_show_weather);
        widget_weather_fahrenheit = (CheckBox) findViewById(R.id.widget_weather_fahrenheit);
        widget_weather_auto_location = (CheckBox) findViewById(R.id.widget_weather_auto_location);

        toggle_analogue = (CheckBox) findViewById(R.id.toggle_analogue);
        toggle_digital = (CheckBox) findViewById(R.id.toggle_digital);
        toggle_day_date = (CheckBox) findViewById(R.id.toggle_date_day);
        toggle_battery = (CheckBox) findViewById(R.id.toggle_battery);
        toggle_dim_colour = (CheckBox) findViewById(R.id.toggle_dim);
        toggle_solid_text = (CheckBox) findViewById(R.id.toggle_solid_number);
        toggle_fix_chin = (CheckBox) findViewById(R.id.toggle_fix_chin);
        toggle_dial = (CheckBox) findViewById(R.id.toggle_dial);

        widget_weather_text_location = (EditText) findViewById(R.id.widget_weather_text_location);

        widget_weather_group = (LinearLayout) findViewById(R.id.widget_weather_group);
        location = (LinearLayout) findViewById(R.id.location);
    }

    private void setUpUIChangeListeners() {
        widget_show_weather.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    widget_weather_group.setVisibility(View.VISIBLE);
                else
                    widget_weather_group.setVisibility(View.GONE);
            }
        });

        widget_weather_auto_location.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    location.setVisibility(View.GONE);
                else
                    location.setVisibility(View.VISIBLE);
            }
        });
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

        showWeather = config.containsKey(Utility.KEY_TOGGLE_WEATHER) && config.getBoolean(Utility.KEY_TOGGLE_WEATHER, false);
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
                locationText = config.getString(Utility.KEY_WIDGET_WEATHER_LOCATION);
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

    private void setChecked() {
        widget_weather_auto_location.setChecked(autoLocation);
        widget_show_weather.setChecked(showWeather);
        widget_weather_fahrenheit.setChecked(fahrenheit);
        digital_format.setChecked(toggleAmPm);
        toggle_analogue.setChecked(toggleAnalogue);
        toggle_digital.setChecked(toggleDigital);
        toggle_day_date.setChecked(toggleDayDate);
        toggle_battery.setChecked(toggleBattery);
        toggle_dim_colour.setChecked(toggleDimColour);
        toggle_solid_text.setChecked(toggleSolidText);
        toggle_fix_chin.setChecked(toggleFixChin);
        toggle_dial.setChecked(toggleDial);
    }

    private void setText() {
        if (!weatherData.isEmpty())
            widget_weather_text_data.setText(weatherData);
        else
            widget_weather_text_data.setText(getString(R.string.weather_data_info));

        if (!locationText.isEmpty())
            widget_weather_text_location.setText(locationText);
    }

    private void setColours(DataMap config) {
        setUpColorPickerSelection(R.id.background, Utility.KEY_BACKGROUND_COLOUR, config, backgroundColour);
        setUpColorPickerSelection(R.id.middle, Utility.KEY_MIDDLE_COLOUR, config, middleColour);
        setUpColorPickerSelection(R.id.foreground, Utility.KEY_FOREGROUND_COLOUR, config, foregroundColour);
        setUpColorPickerSelection(R.id.accent, Utility.KEY_ACCENT_COLOUR, config, accentColour);
    }

    private void init(DataMap config) {
        fetchControls();

        setUpUIChangeListeners();

        try {

            if (config != null) {
                fetchToggles(config);

                fetchWeatherData(config);

                fetchColours(config);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (showWeather) {
            widget_weather_group.setVisibility(View.VISIBLE);
            weather_data.setVisibility(View.VISIBLE);

            if (!autoLocation)
                location.setVisibility(View.VISIBLE);
        }

        setChecked();

        setText();

        setColours(config);
    }

    private void setUpColorPickerSelection(int spinnerId, final String configKey, DataMap config, String defaultColorName) {
        String color;
        if (config != null) {
            color = config.getString(configKey, defaultColorName);
        } else {
            color = defaultColorName;
        }
        Spinner spinner = (Spinner) findViewById(spinnerId);
        String[] colorNames = getResources().getStringArray(R.array.color_array);
        for (int i = 0; i < colorNames.length; i++) {
            if (colorNames[i].toLowerCase().equals(color.toLowerCase())) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void sendConfigUpdateMessage() {
        if (mPeerId != null) {
            if (config == null)
                config = new DataMap();

            config.putBoolean(Utility.KEY_TOGGLE_AM_PM, digital_format.isChecked());
            config.putString(Utility.KEY_BACKGROUND_COLOUR, background.getSelectedItem().toString());
            config.putString(Utility.KEY_MIDDLE_COLOUR, middle.getSelectedItem().toString());
            config.putString(Utility.KEY_FOREGROUND_COLOUR, foreground.getSelectedItem().toString());
            config.putString(Utility.KEY_ACCENT_COLOUR, accent.getSelectedItem().toString());

            String manualLocation = widget_weather_text_location.getText().toString();
            int length = manualLocation.length();
            if (length > 0 && !widget_weather_auto_location.isChecked())
                config.putString(Utility.KEY_WIDGET_WEATHER_LOCATION, manualLocation);

            config.putBoolean(Utility.KEY_TOGGLE_WEATHER, widget_show_weather.isChecked());
            config.putBoolean(Utility.KEY_WIDGET_WEATHER_FAHRENHEIT, widget_weather_fahrenheit.isChecked());
            config.putBoolean(Utility.KEY_WIDGET_WEATHER_AUTO_LOCATION, widget_weather_auto_location.isChecked());

            config.putBoolean(Utility.KEY_TOGGLE_ANALOGUE, toggle_analogue.isChecked());
            config.putBoolean(Utility.KEY_TOGGLE_DIGITAL, toggle_digital.isChecked());
            config.putBoolean(Utility.KEY_TOGGLE_DAY_DATE, toggle_day_date.isChecked());
            config.putBoolean(Utility.KEY_TOGGLE_BATTERY, toggle_battery.isChecked());
            config.putBoolean(Utility.KEY_TOGGLE_DIM_COLOUR, toggle_dim_colour.isChecked());
            config.putBoolean(Utility.KEY_TOGGLE_SOLID_TEXT, toggle_solid_text.isChecked());
            config.putBoolean(Utility.KEY_TOGGLE_FIX_CHIN, toggle_fix_chin.isChecked());
            config.putBoolean(Utility.KEY_TOGGLE_DRAW_DIAL, toggle_dial.isChecked());

            byte[] rawData = config.toByteArray();

            Wearable.MessageApi.sendMessage(mGoogleApiClient, mPeerId, Utility.PATH_DIGILOGUE_SETTINGS, rawData);
        }
    }
}

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

    DataMap config;

    private GoogleApiClient mGoogleApiClient;
    private String mPeerId;

    Spinner background;
    Spinner middle;
    Spinner foreground;
    Spinner accent;

    EditText widget_weather_text_location;

    CheckBox digital_format;
    CheckBox widget_show_weather;
    CheckBox widget_weather_fahrenheit;
    CheckBox widget_weather_auto_location;

    CheckBox toggle_day_date;
    CheckBox toggle_dim_colour;
    CheckBox toggle_solid_text;
    CheckBox toggle_digital;
    CheckBox toggle_analogue;
    CheckBox toggle_battery;
    CheckBox toggle_fix_chin;

    LinearLayout weather_data;
    TextView widget_weather_text_data;

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
            setUpAllPickers(config);
        } else {
            // If DataItem with the current config can't be retrieved, select the default items on
            // each picker.
            setUpAllPickers(null);
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

    private void setUpAllPickers(DataMap config) {
        String backgroundColour = getString(R.string.color_black);
        String middleColour = getString(R.string.color_gray);
        String foregroundColour = getString(R.string.color_white);
        String accentColour = getString(R.string.color_red);

        background = (Spinner) findViewById(R.id.background);
        middle = (Spinner) findViewById(R.id.middle);
        foreground = (Spinner) findViewById(R.id.foreground);
        accent = (Spinner) findViewById(R.id.accent);

        weather_data = (LinearLayout) findViewById(R.id.weather_data);
        widget_weather_text_data = (TextView) findViewById(R.id.widget_weather_text_data);
        digital_format = (CheckBox) findViewById(R.id.digital_format);
        widget_show_weather = (CheckBox)findViewById(R.id.widget_show_weather);
        widget_weather_fahrenheit = (CheckBox) findViewById(R.id.widget_weather_fahrenheit);
        widget_weather_auto_location = (CheckBox) findViewById(R.id.widget_weather_auto_location);

        toggle_analogue = (CheckBox) findViewById(R.id.toggle_analogue);
        toggle_digital = (CheckBox) findViewById(R.id.toggle_digital);
        toggle_day_date = (CheckBox) findViewById(R.id.toggle_date_day);
        toggle_battery = (CheckBox) findViewById(R.id.toggle_battery);
        toggle_dim_colour = (CheckBox) findViewById(R.id.toggle_dim);
        toggle_solid_text = (CheckBox) findViewById(R.id.toggle_solid_number);
        toggle_fix_chin = (CheckBox) findViewById(R.id.toggle_fix_chin);

        widget_weather_text_location = (EditText) findViewById(R.id.widget_weather_text_location);

        final LinearLayout widget_weather_group = (LinearLayout) findViewById(R.id.widget_weather_group);
        final LinearLayout location = (LinearLayout) findViewById(R.id.location);

        boolean autoLocation = true;

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

        if (config != null) {
            // Toggles
            digital_format.setChecked(config.containsKey(Utility.KEY_TOGGLE_AM_PM) && config.getBoolean(Utility.KEY_TOGGLE_AM_PM, false));
            toggle_analogue.setChecked(config.containsKey(Utility.KEY_TOGGLE_ANALOGUE) && config.getBoolean(Utility.KEY_TOGGLE_ANALOGUE, true) || !config.containsKey(Utility.KEY_TOGGLE_ANALOGUE));
            toggle_digital.setChecked(config.containsKey(Utility.KEY_TOGGLE_DIGITAL) && config.getBoolean(Utility.KEY_TOGGLE_DIGITAL, true) || !config.containsKey(Utility.KEY_TOGGLE_DIGITAL));
            toggle_day_date.setChecked(config.containsKey(Utility.KEY_TOGGLE_DAY_DATE) && config.getBoolean(Utility.KEY_TOGGLE_DAY_DATE, true) || !config.containsKey(Utility.KEY_TOGGLE_DAY_DATE));
            toggle_battery.setChecked(config.containsKey(Utility.KEY_TOGGLE_BATTERY) && config.getBoolean(Utility.KEY_TOGGLE_BATTERY, true) || !config.containsKey(Utility.KEY_TOGGLE_BATTERY));
            toggle_dim_colour.setChecked(config.containsKey(Utility.KEY_TOGGLE_DIM_COLOUR) && config.getBoolean(Utility.KEY_TOGGLE_DIM_COLOUR, true) || !config.containsKey(Utility.KEY_TOGGLE_DIM_COLOUR));
            toggle_solid_text.setChecked(config.containsKey(Utility.KEY_TOGGLE_SOLID_TEXT) && config.getBoolean(Utility.KEY_TOGGLE_SOLID_TEXT, false));
            toggle_fix_chin.setChecked(config.containsKey(Utility.KEY_TOGGLE_FIX_CHIN) && config.getBoolean(Utility.KEY_TOGGLE_FIX_CHIN, false));

            boolean showWeather = config.containsKey(Utility.KEY_TOGGLE_WEATHER) && config.getBoolean(Utility.KEY_TOGGLE_WEATHER, false);
            autoLocation = (config.containsKey(Utility.KEY_WIDGET_WEATHER_AUTO_LOCATION) && config.getBoolean(Utility.KEY_WIDGET_WEATHER_AUTO_LOCATION, true) || !config.containsKey(Utility.KEY_WIDGET_WEATHER_AUTO_LOCATION));

            widget_show_weather.setChecked(showWeather);
            if (showWeather) {
                widget_weather_group.setVisibility(View.VISIBLE);
                weather_data.setVisibility(View.VISIBLE);

                String weatherData = "";

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

                if (!weatherData.isEmpty())
                    widget_weather_text_data.setText(weatherData);
                else
                    widget_weather_text_data.setText(getString(R.string.weather_data_info));
            }

            widget_weather_fahrenheit.setChecked(config.containsKey(Utility.KEY_WIDGET_WEATHER_FAHRENHEIT) && config.getBoolean(Utility.KEY_WIDGET_WEATHER_FAHRENHEIT, false));

            if (!autoLocation) {
                location.setVisibility(View.VISIBLE);

                if (config.containsKey(Utility.KEY_WIDGET_WEATHER_LOCATION))
                    widget_weather_text_location.setText(config.getString(Utility.KEY_WIDGET_WEATHER_LOCATION));
            }

            // Colours
            if (config.containsKey(Utility.KEY_BACKGROUND_COLOUR))
                backgroundColour = config.getString(Utility.KEY_BACKGROUND_COLOUR);

            if (config.containsKey(Utility.KEY_MIDDLE_COLOUR))
                middleColour = config.getString(Utility.KEY_MIDDLE_COLOUR);

            if (config.containsKey(Utility.KEY_FOREGROUND_COLOUR))
                foregroundColour = config.getString(Utility.KEY_FOREGROUND_COLOUR);

            if (config.containsKey(Utility.KEY_ACCENT_COLOUR))
                accentColour = config.getString(Utility.KEY_ACCENT_COLOUR);
        }

        widget_weather_auto_location.setChecked(autoLocation);

        setUpColorPickerSelection(R.id.background, Utility.KEY_BACKGROUND_COLOUR, config, backgroundColour);
        setUpColorPickerSelection(R.id.middle, Utility.KEY_MIDDLE_COLOUR, config, middleColour);
        setUpColorPickerSelection(R.id.foreground, Utility.KEY_FOREGROUND_COLOUR, config, foregroundColour);
        setUpColorPickerSelection(R.id.accent, Utility.KEY_ACCENT_COLOUR, config, accentColour);
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

            byte[] rawData = config.toByteArray();

            Wearable.MessageApi.sendMessage(mGoogleApiClient, mPeerId, Utility.PATH_DIGILOGUE_SETTINGS, rawData);
        }
    }
}

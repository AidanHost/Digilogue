package com.greenman.digilogue;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.wearable.companion.WatchFaceCompanion;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
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

public class DigilogueConfigActivity extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<DataApi.DataItemResult> {
    private static final String TAG = "DigilogueConfigActivity";

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

    SeekBar widget_weather_frequency;

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

        // TODO: Bind message listener service
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
            Uri uri = builder.scheme("wear").path(CompanionUtil.PATH_DIGILOGUE_SETTINGS).authority(mPeerId).build();
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

        digital_format = (CheckBox) findViewById(R.id.digital_format);
        widget_show_weather = (CheckBox)findViewById(R.id.widget_show_weather);
        widget_weather_fahrenheit = (CheckBox) findViewById(R.id.widget_weather_fahrenheit);
        widget_weather_auto_location = (CheckBox) findViewById(R.id.widget_weather_auto_location);

        widget_weather_text_location = (EditText) findViewById(R.id.widget_weather_text_location);

        widget_weather_frequency = (SeekBar) findViewById(R.id.widget_weather_frequency);

        final LinearLayout widget_weather_group = (LinearLayout) findViewById(R.id.widget_weather_group);
        final LinearLayout location = (LinearLayout) findViewById(R.id.location);
        final TextView text_frequency = (TextView) findViewById(R.id.text_frequency);
        boolean autoLocation = true;

        widget_show_weather.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    widget_weather_group.setVisibility(View.VISIBLE);
                } else {
                    widget_weather_group.setVisibility(View.GONE);
                }
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

        widget_weather_frequency.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 1) {
                    progress = 1;
                    seekBar.setProgress(progress);
                }

                text_frequency.setText(String.format(getString(R.string.widget_weather_frequency), progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        if (config != null) {
            // Toggles
            digital_format.setChecked(config.containsKey(CompanionUtil.KEY_12HOUR_FORMAT) && config.getBoolean(CompanionUtil.KEY_12HOUR_FORMAT, false));

            boolean showWeather = config.containsKey(CompanionUtil.KEY_WIDGET_SHOW_WEATHER) && config.getBoolean(CompanionUtil.KEY_WIDGET_SHOW_WEATHER, false);
            autoLocation = (config.containsKey(CompanionUtil.KEY_WIDGET_WEATHER_AUTO_LOCATION) && config.getBoolean(CompanionUtil.KEY_WIDGET_WEATHER_AUTO_LOCATION, true) || !config.containsKey(CompanionUtil.KEY_WIDGET_WEATHER_AUTO_LOCATION));

            widget_show_weather.setChecked(showWeather);
            if (showWeather)
                widget_weather_group.setVisibility(View.VISIBLE);

            widget_weather_fahrenheit.setChecked(config.containsKey(CompanionUtil.KEY_WIDGET_WEATHER_FAHRENHEIT) && config.getBoolean(CompanionUtil.KEY_WIDGET_WEATHER_FAHRENHEIT, false));

            if (config.containsKey(CompanionUtil.KEY_WIDGET_WEATHER_UPDATE_FREQUENCY))
                widget_weather_frequency.setProgress((int)config.getLong(CompanionUtil.KEY_WIDGET_WEATHER_UPDATE_FREQUENCY));
            else {
                widget_weather_frequency.setProgress(3);
                text_frequency.setText(String.format(getString(R.string.widget_weather_frequency), 3));
            }

            if (!autoLocation) {
                location.setVisibility(View.VISIBLE);

                if (config.containsKey(CompanionUtil.KEY_WIDGET_WEATHER_LOCATION))
                    widget_weather_text_location.setText(config.getString(CompanionUtil.KEY_WIDGET_WEATHER_LOCATION));
            }

            // Colours
            if (config.containsKey(CompanionUtil.KEY_BACKGROUND_COLOUR))
                backgroundColour = config.getString(CompanionUtil.KEY_BACKGROUND_COLOUR);

            if (config.containsKey(CompanionUtil.KEY_MIDDLE_COLOUR))
                middleColour = config.getString(CompanionUtil.KEY_MIDDLE_COLOUR);

            if (config.containsKey(CompanionUtil.KEY_FOREGROUND_COLOUR))
                foregroundColour = config.getString(CompanionUtil.KEY_FOREGROUND_COLOUR);

            if (config.containsKey(CompanionUtil.KEY_ACCENT_COLOUR))
                accentColour = config.getString(CompanionUtil.KEY_ACCENT_COLOUR);
        }

        widget_weather_auto_location.setChecked(autoLocation);

        setUpColorPickerSelection(R.id.background, CompanionUtil.KEY_BACKGROUND_COLOUR, config, backgroundColour);
        setUpColorPickerSelection(R.id.middle, CompanionUtil.KEY_MIDDLE_COLOUR, config, middleColour);
        setUpColorPickerSelection(R.id.foreground, CompanionUtil.KEY_FOREGROUND_COLOUR, config, foregroundColour);
        setUpColorPickerSelection(R.id.accent, CompanionUtil.KEY_ACCENT_COLOUR, config, accentColour);
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
            DataMap config = new DataMap();
            config.putBoolean(CompanionUtil.KEY_12HOUR_FORMAT, digital_format.isChecked());
            config.putString(CompanionUtil.KEY_BACKGROUND_COLOUR, background.getSelectedItem().toString());
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

            config.putLong(CompanionUtil.KEY_WIDGET_WEATHER_UPDATE_FREQUENCY, frequency);
            byte[] rawData = config.toByteArray();

            Wearable.MessageApi.sendMessage(mGoogleApiClient, mPeerId, CompanionUtil.PATH_DIGILOGUE_SETTINGS, rawData);
        }
    }
}

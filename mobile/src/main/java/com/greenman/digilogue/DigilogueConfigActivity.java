package com.greenman.digilogue;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.wearable.companion.WatchFaceCompanion;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;

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

    public static final String KEY_12HOUR_FORMAT = "com.greenman.digilogue.12HOUR_FORMAT";
    public static final String KEY_BACKGROUND_COLOUR = "com.greenman.digilogue.BACKGROUND_COLOUR";
    public static final String KEY_MIDDLE_COLOUR = "com.greenman.digilogue.MIDDLE_COLOUR";
    public static final String KEY_FOREGROUND_COLOUR = "com.greenman.digilogue.FOREGROUND_COLOUR";
    public static final String KEY_ACCENT_COLOUR = "com.greenman.digilogue.ACCENT_COLOUR";

    private static final String PATH_DIGILOGUE_COLOURS = "/digilogue/colours";

    private GoogleApiClient mGoogleApiClient;
    private String mPeerId;

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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();

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
            Uri uri = builder.scheme("wear").path(PATH_DIGILOGUE_COLOURS).authority(mPeerId).build();
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

        if (config != null) {
            CheckBox checkBox = (CheckBox) findViewById(R.id.digital_format);
            checkBox.setChecked(config.containsKey(KEY_12HOUR_FORMAT) && config.getBoolean(KEY_12HOUR_FORMAT, false));

            if (config.containsKey(KEY_BACKGROUND_COLOUR))
                backgroundColour = config.getString(KEY_BACKGROUND_COLOUR);

            if (config.containsKey(KEY_MIDDLE_COLOUR))
                middleColour = config.getString(KEY_MIDDLE_COLOUR);

            if (config.containsKey(KEY_FOREGROUND_COLOUR))
                foregroundColour = config.getString(KEY_FOREGROUND_COLOUR);

            if (config.containsKey(KEY_ACCENT_COLOUR))
                accentColour = config.getString(KEY_ACCENT_COLOUR);
        }

        setUpColorPickerSelection(R.id.background, KEY_BACKGROUND_COLOUR, config, backgroundColour);
        setUpColorPickerSelection(R.id.middle, KEY_MIDDLE_COLOUR, config, middleColour);
        setUpColorPickerSelection(R.id.foreground, KEY_FOREGROUND_COLOUR, config, foregroundColour);
        setUpColorPickerSelection(R.id.accent, KEY_ACCENT_COLOUR, config, accentColour);

        Button buttonUpdate = (Button) findViewById(R.id.button_update);
        buttonUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendConfigUpdateMessage();
            }
        });
    }

    private void setUpColorPickerSelection(int spinnerId, final String configKey, DataMap config, String defaultColorName) {
        int color;
        if (config != null) {
            color = Color.parseColor(config.getString(configKey, defaultColorName));
        } else {
            color = Color.parseColor(defaultColorName);
        }
        Spinner spinner = (Spinner) findViewById(spinnerId);
        String[] colorNames = getResources().getStringArray(R.array.color_array);
        for (int i = 0; i < colorNames.length; i++) {
            if (Color.parseColor(colorNames[i]) == color) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void sendConfigUpdateMessage() {
        if (mPeerId != null) {
            CheckBox checkBox = (CheckBox) findViewById(R.id.digital_format);

            Spinner backgroundSpinner = (Spinner) findViewById(R.id.background);
            Spinner middleSpinner = (Spinner) findViewById(R.id.middle);
            Spinner foregroundSpinner = (Spinner) findViewById(R.id.foreground);
            Spinner accentSpinner = (Spinner) findViewById(R.id.accent);

            String backgroundColour = backgroundSpinner.getSelectedItem().toString();

            DataMap config = new DataMap();
            config.putBoolean(KEY_12HOUR_FORMAT, checkBox.isChecked());
            config.putString(KEY_BACKGROUND_COLOUR, backgroundColour);
            config.putString(KEY_MIDDLE_COLOUR, middleSpinner.getSelectedItem().toString());
            config.putString(KEY_FOREGROUND_COLOUR, foregroundSpinner.getSelectedItem().toString());
            config.putString(KEY_ACCENT_COLOUR, accentSpinner.getSelectedItem().toString());
            byte[] rawData = config.toByteArray();

            Wearable.MessageApi.sendMessage(mGoogleApiClient, mPeerId, PATH_DIGILOGUE_COLOURS, rawData);
        }
    }
}

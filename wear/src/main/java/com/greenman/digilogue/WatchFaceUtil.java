package com.greenman.digilogue;

import android.net.Uri;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.TimeUnit;

public class WatchFaceUtil {
    private static final String TAG = "WatchFaceUtil";

    public static final String PATH_DIGILOGUE_SETTINGS = "/digilogue/colours";

    // boolean keys
    public static final String KEY_12HOUR_FORMAT = "com.greenman.digilogue.12HOUR_FORMAT";
    public static final String KEY_WIDGET_SHOW_WEATHER = "com.greenman.digilogue.SHOW_WEATHER";
    public static final String KEY_WIDGET_WEATHER_FAHRENHEIT = "com.greenman.digilogue.FAHRENHEIT";
    public static final String KEY_WIDGET_WEATHER_AUTO_LOCATION = "com.greenman.digilogue.AUTO_LOCATION";

    // string keys
    public static final String KEY_BACKGROUND_COLOUR = "com.greenman.digilogue.BACKGROUND_COLOUR";
    public static final String KEY_MIDDLE_COLOUR = "com.greenman.digilogue.MIDDLE_COLOUR";
    public static final String KEY_FOREGROUND_COLOUR = "com.greenman.digilogue.FOREGROUND_COLOUR";
    public static final String KEY_ACCENT_COLOUR = "com.greenman.digilogue.ACCENT_COLOUR";
    public static final String KEY_WIDGET_WEATHER_LOCATION = "com.greenman.digilogue.LOCATION";

    // int keys
    public static final String KEY_WIDGET_WEATHER_DATA_TEMPERATURE_C = "com.greenman.digilogue.WEATHER_TEMPERATURE_C";
    public static final String KEY_WIDGET_WEATHER_DATA_TEMPERATURE_F = "com.greenman.digilogue.WEATHER_TEMPERATURE_F";
    public static final String KEY_WIDGET_WEATHER_DATA_CODE = "com.greenman.digilogue.WEATHER_CODE";

    // long keys
    public static final String KEY_WIDGET_WEATHER_DATA_DATETIME = "com.greenman.digilogue.WEATHER_DATETIME";


    // boolean defaults
    public static final boolean CONFIG_12HOUR_DEFAULT = false;
    public static final boolean CONFIG_WIDGET_SHOW_WEATHER_DEFAULT = false;
    public static final boolean CONFIG_WIDGET_FAHRENHEIT_DEFAULT = false;
    public static final boolean CONFIG_AUTO_LOCATION_DEFAULT = true;

    // string defaults
    public static final String COLOUR_NAME_DEFAULT_AND_AMBIENT_BACKGROUND = "black";
    public static final String COLOUR_NAME_DEFAULT_AND_AMBIENT_MIDDLE = "gray";
    public static final String COLOUR_NAME_DEFAULT_AND_AMBIENT_FOREGROUND = "white";
    public static final String COLOUR_NAME_DEFAULT_AND_AMBIENT_ACCENT = "red";
    public static final String CONFIG_LOCATION_DEFAULT = "";

    // int defaults
    public static int WIDGET_WEATHER_DATA_TEMPERATURE_C_DEFAULT = -999;
    public static int WIDGET_WEATHER_DATA_TEMPERATURE_F_DEFAULT = -999;
    public static int WIDGET_WEATHER_DATA_CODE_DEFAULT = WeatherCodes.UNKNOWN;

    // Weather codes for icons
    public static class WeatherCodes {
        static final int UNKNOWN = -1;
        static final int SUNNY = 113;
        static final int PARTLY_CLOUDY = 116;
    }

    private WatchFaceUtil() { }

    public interface FetchConfigDataMapCallback {
        void onConfigDataMapFetched(DataMap config);
    }

    /**
     * Asynchronously fetches the current config {@link DataMap} for {@link WatchFaceUtil}
     * and passes it to the given callback.
     * <p>
     * If the current config {@link com.google.android.gms.wearable.DataItem} doesn't exist, it isn't created and the callback
     * receives an empty DataMap.
     */
    public static void fetchConfigDataMap(final GoogleApiClient client, final FetchConfigDataMapCallback callback) {
        Wearable.NodeApi.getLocalNode(client).setResultCallback(
                new ResultCallback<NodeApi.GetLocalNodeResult>() {
                    @Override
                    public void onResult(NodeApi.GetLocalNodeResult getLocalNodeResult) {
                        String localNode = getLocalNodeResult.getNode().getId();
                        Uri uri = new Uri.Builder()
                                .scheme("wear")
                                .path(PATH_DIGILOGUE_SETTINGS)
                                .authority(localNode)
                                .build();
                        Wearable.DataApi.getDataItem(client, uri)
                                .setResultCallback(new DataItemResultCallback(callback));
                    }
                }
        );
    }

    /*public static void fetchConnectedConfigDataMap(final GoogleApiClient client, final FetchConfigDataMapCallback callback) {
        Wearable.NodeApi.getConnectedNodes(client).setResultCallback(
                new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(NodeApi.GetConnectedNodesResult getLocalNodeResult) {
                        for (Node node : getLocalNodeResult.getNodes()) {
                            String localNode = node.getId();
                            Uri uri = new Uri.Builder()
                                    .scheme("wear")
                                    .path(WatchFaceUtil.PATH_DIGILOGUE_SETTINGS)
                                    .authority(localNode)
                                    .build();
                            Wearable.DataApi.getDataItem(client, uri)
                                    .setResultCallback(new DataItemResultCallback(callback));
                        }
                    }
                }
        );
    }*/

    /**
     * Overwrites (or sets, if not present) the keys in the current config {@link com.google.android.gms.wearable.DataItem} with
     * the ones appearing in the given {@link DataMap}. If the config DataItem doesn't exist,
     * it's created.
     * <p>
     * It is allowed that only some of the keys used in the config DataItem appear in
     * {@code configKeysToOverwrite}. The rest of the keys remains unmodified in this case.
     */
    public static void overwriteKeysInConfigDataMap(final GoogleApiClient googleApiClient, final DataMap configKeysToOverwrite) {
        WatchFaceUtil.fetchConfigDataMap(googleApiClient,
                new FetchConfigDataMapCallback() {
                    @Override
                    public void onConfigDataMapFetched(DataMap currentConfig) {
                        DataMap overwrittenConfig = new DataMap();
                        overwrittenConfig.putAll(currentConfig);
                        overwrittenConfig.putAll(configKeysToOverwrite);
                        WatchFaceUtil.putConfigDataItem(googleApiClient, overwrittenConfig);
                    }
                }
        );
    }

    /**
     * Overwrites the current config {@link com.google.android.gms.wearable.DataItem}'s {@link DataMap} with {@code newConfig}.
     * If the config DataItem doesn't exist, it's created.
     */
    public static void putConfigDataItem(GoogleApiClient googleApiClient, DataMap newConfig) {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH_DIGILOGUE_SETTINGS);
        DataMap configToPut = putDataMapRequest.getDataMap();
        configToPut.putAll(newConfig);
        Wearable.DataApi.putDataItem(googleApiClient, putDataMapRequest.asPutDataRequest())
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "putDataItem result status: " + dataItemResult.getStatus());
                        }
                    }
                });
    }

    private static class DataItemResultCallback implements ResultCallback<DataApi.DataItemResult> {

        private final FetchConfigDataMapCallback mCallback;

        public DataItemResultCallback(FetchConfigDataMapCallback callback) {
            mCallback = callback;
        }

        @Override
        public void onResult(DataApi.DataItemResult dataItemResult) {
            if (dataItemResult.getStatus().isSuccess()) {
                if (dataItemResult.getDataItem() != null) {
                    DataItem configDataItem = dataItemResult.getDataItem();
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(configDataItem);
                    DataMap config = dataMapItem.getDataMap();
                    mCallback.onConfigDataMapFetched(config);
                } else {
                    mCallback.onConfigDataMapFetched(new DataMap());
                }
            }
        }
    }
}

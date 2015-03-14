package com.greenman.digilogue;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.greenman.common.Utility;
import com.greenman.common.WatchFace;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Service to handle drawing of the watch face
 */
public class DigilogueWatchFaceService extends CanvasWatchFaceService {
    private static final String TAG = "WatchFaceService";

    /**
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Update rate in milliseconds for mute mode. We update every minute, like in ambient mode.
     */
    private static final long MUTE_UPDATE_RATE_MS = TimeUnit.MINUTES.toMillis(1);

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
        //region class member variables
        private static final int MSG_UPDATE_TIME = 0;
        private static final int MSG_REFRESH_WEATHER = 1;

        private RefreshWeatherTask mRefreshWeatherTask;

        private DataMap mConfig;

        private Time mTime;

        /**
         * How often {@link #mUpdateHandler} ticks in milliseconds.
         */
        long mInteractiveUpdateRateMs = INTERACTIVE_UPDATE_RATE_MS;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        private boolean mLowBitAmbient;
        private boolean mRegisteredTimeZoneReceiver = false;
        private boolean mRunWeather = true;

        final private GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(DigilogueWatchFaceService.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
        //endregion

        boolean mToggleWeather = Utility.CONFIG_DEFAULT_TOGGLE_WEATHER;
        long mLastTime = Utility.WIDGET_WEATHER_DATA_DEFAULT_DATETIME;

        //region Handler, Callbacks and Receivers
        /**
         * Handler to update the time once a second in interactive mode.
         */
        final Handler mUpdateHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        if (Log.isLoggable(TAG, Log.VERBOSE)) {
                            Log.v(TAG, "updating time");
                        }
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                    case MSG_REFRESH_WEATHER:
                        cancelRefreshWeatherTask();
                        mRefreshWeatherTask = new RefreshWeatherTask();
                        mRefreshWeatherTask.execute();
                        break;
                }
            }
        };


        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        private final BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent intent) {
                WatchFace.setBatteryLevel(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0));
            }
        };

        private final WatchFaceUtil.FetchConfigDataMapCallback fetchConfigCallback = new WatchFaceUtil.FetchConfigDataMapCallback() {
            @Override
            public void onConfigDataMapFetched(DataMap config) {
                DigilogueWatchFaceService.Engine.this.mConfig = config;

                if (mConfig.containsKey(Utility.KEY_TOGGLE_WEATHER))
                    mToggleWeather = mConfig.getBoolean(Utility.KEY_TOGGLE_WEATHER);

                if (mConfig.containsKey(Utility.KEY_WIDGET_WEATHER_DATA_DATETIME)) {
                    long oldTime = mLastTime;
                    mLastTime = config.getLong(Utility.KEY_WIDGET_WEATHER_DATA_DATETIME);
                    if (mLastTime != oldTime || mTime.toMillis(true) >= mLastTime + TimeUnit.HOURS.toMillis(Utility.REFRESH_WEATHER_DELAY_HOURS))
                        mRunWeather = true;

                    if (mToggleWeather && !mRunWeather && mLastTime == 0)
                        mRunWeather = true;
                }

                WatchFace.updateUI(dataMapToBundle(config), isInAmbientMode());
                invalidate();
            }
        };
        //endregion

        //region Overrides
        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(DigilogueWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setViewProtection(WatchFaceStyle.PROTECT_HOTWORD_INDICATOR | WatchFaceStyle.PROTECT_STATUS_BAR)
                    .setHotwordIndicatorGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL)
                    .setShowSystemUiTime(false)
                    .build());

            WatchFace.init();

            mTime = new Time();
        }

        @Override
        public void onDestroy() {
            mUpdateHandler.removeMessages(MSG_UPDATE_TIME);
            mUpdateHandler.removeMessages(MSG_REFRESH_WEATHER);
            cancelRefreshWeatherTask();
            super.onDestroy();
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            WatchFace.setFixChin(insets.hasSystemWindowInsets(), insets.getSystemWindowInsetBottom());
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);

            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();

            mTime.setToNow();

            try {
                WatchFaceUtil.fetchConfigDataMap(mGoogleApiClient, fetchConfigCallback);
            } catch (Exception ignored) {}

            if (mToggleWeather && mTime.toMillis(true) >= mLastTime + TimeUnit.HOURS.toMillis(Utility.REFRESH_WEATHER_DELAY_HOURS) && mRunWeather) {
                mUpdateHandler.sendEmptyMessage(MSG_REFRESH_WEATHER);
                mRunWeather = false;
            }
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            if (mLowBitAmbient)
                WatchFace.setAntiAlias(inAmbientMode);

            if (mConfig != null) {
                if (mConfig.containsKey(Utility.KEY_TOGGLE_WEATHER))
                    mToggleWeather = mConfig.getBoolean(Utility.KEY_TOGGLE_WEATHER);

                if (mConfig.containsKey(Utility.KEY_WIDGET_WEATHER_DATA_DATETIME)) {
                    long oldTime = mLastTime;
                    mLastTime = mConfig.getLong(Utility.KEY_WIDGET_WEATHER_DATA_DATETIME);
                    if (mLastTime != oldTime || mTime.toMillis(true) >= mLastTime + TimeUnit.HOURS.toMillis(Utility.REFRESH_WEATHER_DELAY_HOURS))
                        mRunWeather = true;

                    if (mToggleWeather && !mRunWeather && mLastTime == 0)
                        mRunWeather = true;
                }
                WatchFace.updateUI(dataMapToBundle(mConfig), inAmbientMode);
            }

            invalidate();

            // Whether the timer should be running depends on whether we're in ambient mode (as well
            // as whether we're visible), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);

            setInteractiveUpdateRateMs(inMuteMode ? MUTE_UPDATE_RATE_MS : INTERACTIVE_UPDATE_RATE_MS);

            WatchFace.setMuteMode(inMuteMode);
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            WatchFace.draw(canvas, bounds.width(), bounds.height(), false);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                mGoogleApiClient.connect();

                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();

                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    Wearable.DataApi.removeListener(mGoogleApiClient, this);
                    mGoogleApiClient.disconnect();
                }
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onConnected(Bundle connectionHint) {
            Wearable.DataApi.addListener(mGoogleApiClient, Engine.this);

            WatchFaceUtil.fetchConfigDataMap(mGoogleApiClient, fetchConfigCallback);
        }

        @Override
        public void onConnectionSuspended(int cause) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }

        @Override
        public void onConnectionFailed(ConnectionResult result) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }

        @Override
        public void onDataChanged(DataEventBuffer dataEvents) {
            try {
                for (DataEvent dataEvent : dataEvents) {
                    if (dataEvent.getType() != DataEvent.TYPE_CHANGED) {
                        continue;
                    }

                    DataItem dataItem = dataEvent.getDataItem();
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                    DataMap config = dataMapItem.getDataMap();
                    this.mConfig = config;

                    if (mConfig.containsKey(Utility.KEY_TOGGLE_WEATHER))
                        mToggleWeather = mConfig.getBoolean(Utility.KEY_TOGGLE_WEATHER);

                    if (mConfig.containsKey(Utility.KEY_WIDGET_WEATHER_DATA_DATETIME)) {
                        long oldTime = mLastTime;
                        mLastTime = config.getLong(Utility.KEY_WIDGET_WEATHER_DATA_DATETIME);
                        if (mLastTime != oldTime || mTime.toMillis(true) >= mLastTime + TimeUnit.HOURS.toMillis(Utility.REFRESH_WEATHER_DELAY_HOURS))
                            mRunWeather = true;

                        if (mToggleWeather && !mRunWeather && mLastTime == 0)
                            mRunWeather = true;
                    }

                    WatchFace.updateUI(dataMapToBundle(config), isInAmbientMode());
                    invalidate();
                }
            } finally {
                dataEvents.close();
            }
        }
        //endregion

        //region custom methods
        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            DigilogueWatchFaceService.this.registerReceiver(mTimeZoneReceiver, new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED));
            DigilogueWatchFaceService.this.registerReceiver(mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            DigilogueWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
            DigilogueWatchFaceService.this.unregisterReceiver(mBatInfoReceiver);
        }

        private void cancelRefreshWeatherTask() {
            if (mRefreshWeatherTask != null) {
                mRefreshWeatherTask.cancel(true);
            }
        }

        private Bundle dataMapToBundle(DataMap config) {
            Bundle bundle = new Bundle();

            for (int i = 0; i < config.keySet().size(); i++) {
                String key = config.keySet().toArray()[i].toString();
                Object value = config.get(key);
                if (value instanceof Integer) {
                    bundle.putInt(key, (int) value);
                } else if (value instanceof Boolean) {
                    bundle.putBoolean(key, (boolean) value);
                } else if (value instanceof String) {
                    bundle.putString(key, (String) value);
                } else if (value instanceof Long) {
                    bundle.putLong(key, (long) value);
                }
            }

            return bundle;
        }

        private DataMap bundleToDataMap(Bundle bundle) {
            DataMap map = new DataMap();

            for (int i = 0; i < bundle.keySet().size(); i++) {
                String key = bundle.keySet().toArray()[i].toString();
                Object value = bundle.get(key);
                if (value instanceof Integer) {
                    map.putInt(key, (int) value);
                } else if (value instanceof Boolean) {
                    map.putBoolean(key, (boolean) value);
                } else if (value instanceof String) {
                    map.putString(key, (String) value);
                } else if (value instanceof Long) {
                    map.putLong(key, (long) value);
                }
            }

            return map;
        }
        //endregion

        //region Timer methods

        /**
         * Starts the {@link #mUpdateHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        public void setInteractiveUpdateRateMs(long updateRateMs) {
            if (updateRateMs == mInteractiveUpdateRateMs) {
                return;
            }
            mInteractiveUpdateRateMs = updateRateMs;

            // Stop and restart the timer so the new update rate takes effect immediately.
            if (shouldTimerBeRunning()) {
                updateTimer();
            }
        }
        //endregion

        private class RefreshWeatherTask extends AsyncTask<Void, Void, Void> {
            private PowerManager.WakeLock mWakeLock;

            @Override
            protected Void doInBackground(Void... voids) {
                PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DigilogueWakeLock");
                mWakeLock.acquire();

                // get weather data
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(
                        new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                            @Override
                            public void onResult(NodeApi.GetConnectedNodesResult result) {
                                if (mConfig != null) {
                                    byte[] rawData = mConfig.toByteArray();
                                    for (Node node : result.getNodes()) {
                                        String nodeId = node.getId();
                                        Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, Utility.PATH_DIGILOGUE_SETTINGS, rawData);
                                    }
                                }
                            }
                        });

                return null;
            }

            @Override
            protected void onPostExecute(Void temp) {
                releaseWakeLock();
            }

            @Override
            protected void onCancelled() {
                releaseWakeLock();
            }

            private void releaseWakeLock() {
                if (mWakeLock != null) {
                    mWakeLock.release();
                    mWakeLock = null;
                }
            }
        }
    }
}
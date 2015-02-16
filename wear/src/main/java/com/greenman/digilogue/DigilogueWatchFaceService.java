package com.greenman.digilogue;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Service to handle drawing of the watch face
 */
public class DigilogueWatchFaceService extends CanvasWatchFaceService {
    private static final String TAG = "WatchFaceService";

    private static final Typeface NORMAL_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

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

    private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener { //, MessageApi.MessageListener {
        static final int MSG_UPDATE_TIME = 0;
        static final int MSG_REFRESH_WEATHER = 1;

        static final String COLON_STRING = ":";
        int REFRESH_WEATHER_DELAY_HOURS = 3;

        private RefreshWeatherTask mRefreshWeatherTask;

        private DataMap config;

        /** How often {@link #mUpdateHandler} ticks in milliseconds. */
        long mInteractiveUpdateRateMs = INTERACTIVE_UPDATE_RATE_MS;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;
        boolean mMute;
        boolean mRegisteredTimeZoneReceiver = false;
        boolean m12Hour = WatchFaceUtil.CONFIG_12HOUR_DEFAULT;
        boolean mShowWeather = WatchFaceUtil.CONFIG_WIDGET_SHOW_WEATHER_DEFAULT;
        boolean mFahrenheit = WatchFaceUtil.CONFIG_WIDGET_FAHRENHEIT_DEFAULT;

        Time mTime;

        float mXOffset;
        float mYOffset;
        float smallTextOffset;
        float mColonWidth;

        int batteryLevel = 100;
        int foregroundOpacityLevel;
        int accentOpacityLevel;

        int temperatureC = WatchFaceUtil.WIDGET_WEATHER_DATA_TEMPERATURE_C_DEFAULT;
        int temperatureF = WatchFaceUtil.WIDGET_WEATHER_DATA_TEMPERATURE_F_DEFAULT;
        int code = WatchFaceUtil.WIDGET_WEATHER_DATA_CODE_DEFAULT;

        String mAmString;
        String mPmString;

        String mBackgroundColor = WatchFaceUtil.COLOUR_NAME_DEFAULT_AND_AMBIENT_BACKGROUND;
        String mMiddleColor = WatchFaceUtil.COLOUR_NAME_DEFAULT_AND_AMBIENT_MIDDLE;
        String mForegroundColor = WatchFaceUtil.COLOUR_NAME_DEFAULT_AND_AMBIENT_FOREGROUND;
        String mAccentColor = WatchFaceUtil.COLOUR_NAME_DEFAULT_AND_AMBIENT_ACCENT;

        // Face
        Paint mBackgroundPaint;
        Paint mHourTickPaint;
        Paint mMinuteTickPaint;
        Paint mBatteryFullPaint;
        Paint mBatteryPaint;
        Paint mTextElementPaint;
        Paint mWidgetWeatherPaint;

        // Analogue
        Paint mHourPaint;
        Paint mMinutePaint;
        Paint mSecondPaint;

        // Digital
        Paint mDigitalHourPaint;
        Paint mDigitalMinutePaint;
        Paint mDigitalAmPmPaint;
        Paint mColonPaint;

        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(DigilogueWatchFaceService.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

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

        private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent intent) {
                batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            }
        };

        private WatchFaceUtil.FetchConfigDataMapCallback fetchConfigCallback = new WatchFaceUtil.FetchConfigDataMapCallback() {
            @Override
            public void onConfigDataMapFetched(DataMap config) {
                DigilogueWatchFaceService.Engine.this.config = config;
                updateUI(config);
            }
        };

        private void onWeatherRefreshed() {
            mUpdateHandler.sendEmptyMessageDelayed(MSG_REFRESH_WEATHER, TimeUnit.HOURS.toMillis(REFRESH_WEATHER_DELAY_HOURS));
        }
        //endregion

        //region Overrides
        @Override
        public void onCreate(SurfaceHolder holder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCreate");
            }
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(DigilogueWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setViewProtection(WatchFaceStyle.PROTECT_HOTWORD_INDICATOR | WatchFaceStyle.PROTECT_STATUS_BAR)
                    .setHotwordIndicatorGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL)
                    .setShowSystemUiTime(false)
                    .build());

            Resources resources = DigilogueWatchFaceService.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);
            mAmString = resources.getString(R.string.digital_am);
            mPmString = resources.getString(R.string.digital_pm);

            mHourPaint = new Paint();
            mHourPaint.setColor(Color.parseColor(mForegroundColor));
            mHourPaint.setStrokeWidth(3f);
            mHourPaint.setAntiAlias(true);
            mHourPaint.setStrokeCap(Paint.Cap.ROUND);

            mMinutePaint = new Paint();
            mMinutePaint.setColor(Color.parseColor(mForegroundColor));
            mMinutePaint.setStrokeWidth(3f);
            mMinutePaint.setAntiAlias(true);
            mMinutePaint.setStrokeCap(Paint.Cap.ROUND);

            mSecondPaint = new Paint();
            mSecondPaint.setColor(Color.parseColor(mAccentColor));
            mSecondPaint.setStrokeWidth(2f);
            mSecondPaint.setAntiAlias(true);
            mSecondPaint.setStrokeCap(Paint.Cap.ROUND);

            mHourTickPaint = new Paint();
            mHourTickPaint.setColor(Color.parseColor(mForegroundColor));
            mHourTickPaint.setAlpha(100);
            mHourTickPaint.setStrokeWidth(2f);
            mHourTickPaint.setAntiAlias(true);

            mMinuteTickPaint = new Paint();
            mMinuteTickPaint.setColor(Color.parseColor(mForegroundColor));
            mMinuteTickPaint.setAlpha(100);
            mMinuteTickPaint.setStrokeWidth(1f);
            mMinuteTickPaint.setAntiAlias(true);

            mBatteryFullPaint = new Paint();
            mBatteryFullPaint.setColor(Color.parseColor(mMiddleColor));
            mBatteryFullPaint.setStrokeWidth(1f);

            mWidgetWeatherPaint = new Paint();
            mWidgetWeatherPaint.setColor(Color.parseColor(mForegroundColor));
            mWidgetWeatherPaint.setStrokeWidth(1f);

            mBatteryPaint = new Paint();
            mBatteryPaint.setColor(Color.parseColor(mForegroundColor));
            mBatteryPaint.setStrokeWidth(1f);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(Color.parseColor(mBackgroundColor));

            mDigitalHourPaint = createTextPaint(Color.parseColor(mForegroundColor));
            mDigitalMinutePaint = createTextPaint(Color.parseColor(mForegroundColor));
            mDigitalAmPmPaint = createTextPaint(Color.parseColor(mForegroundColor));
            mTextElementPaint = createTextPaint(Color.parseColor(mForegroundColor));
            mColonPaint = createTextPaint(Color.parseColor(mMiddleColor));

            mTime = new Time();

            foregroundOpacityLevel = mMute || isInAmbientMode() ? 125 : 255;
            accentOpacityLevel = mMute || isInAmbientMode() ? 100 : 255;
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
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onApplyWindowInsets: " + (insets.isRound() ? "round" : "square"));
            }
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = DigilogueWatchFaceService.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension(isRound ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            float textSize = resources.getDimension(isRound ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            mDigitalHourPaint.setTextSize(textSize);
            mDigitalMinutePaint.setTextSize(textSize);
            mTextElementPaint.setTextSize(textSize / 2f);
            mColonPaint.setTextSize(textSize);

            mColonWidth = mColonPaint.measureText(COLON_STRING);

            smallTextOffset = textSize / 4f;
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);

            boolean burnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onPropertiesChanged: burn-in protection = " + burnInProtection
                        + ", low-bit ambient = " + mLowBitAmbient);
            }
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onTimeTick: ambient = " + isInAmbientMode());
            }
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);
            }

            if (mLowBitAmbient) {
                boolean antiAlias = !inAmbientMode;
                mHourPaint.setAntiAlias(antiAlias);
                mMinutePaint.setAntiAlias(antiAlias);
                mSecondPaint.setAntiAlias(antiAlias);
                mHourTickPaint.setAntiAlias(antiAlias);
            }

            // Dim all elements on screen
            foregroundOpacityLevel = mMute || isInAmbientMode() ? 125 : 255;
            accentOpacityLevel = mMute || isInAmbientMode() ? 100 : 255;

            if (isInAmbientMode()) {
                setBackgroundColor(WatchFaceUtil.COLOUR_NAME_DEFAULT_AND_AMBIENT_BACKGROUND);
                setMiddleColor(WatchFaceUtil.COLOUR_NAME_DEFAULT_AND_AMBIENT_MIDDLE);
                setForegroundColor(WatchFaceUtil.COLOUR_NAME_DEFAULT_AND_AMBIENT_FOREGROUND);
                setAccentColor(WatchFaceUtil.COLOUR_NAME_DEFAULT_AND_AMBIENT_ACCENT);
                invalidate();
            } else {
                WatchFaceUtil.fetchConfigDataMap(mGoogleApiClient, fetchConfigCallback);
            }

            // Whether the timer should be running depends on whether we're in ambient mode (as well
            // as whether we're visible), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);

            setInteractiveUpdateRateMs(inMuteMode ? MUTE_UPDATE_RATE_MS : INTERACTIVE_UPDATE_RATE_MS);

            // Dim all elements on screen
            foregroundOpacityLevel = inMuteMode || isInAmbientMode() ? 125 : 255;
            accentOpacityLevel = inMuteMode || isInAmbientMode() ? 100 : 255;

            if (mMute != inMuteMode) {
                mMute = inMuteMode;
                mHourPaint.setAlpha(foregroundOpacityLevel);
                mMinutePaint.setAlpha(foregroundOpacityLevel);
                mSecondPaint.setAlpha(accentOpacityLevel);

                mDigitalHourPaint.setAlpha(foregroundOpacityLevel);
                mDigitalMinutePaint.setAlpha(foregroundOpacityLevel);

                mTextElementPaint.setAlpha(foregroundOpacityLevel);
                mBatteryFullPaint.setAlpha(foregroundOpacityLevel);
                mBatteryPaint.setAlpha(foregroundOpacityLevel);

                invalidate();
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();

            int width = bounds.width();
            int height = bounds.height();

            canvas.drawRect(0, 0, width, height, mBackgroundPaint);

            // Find the center. Ignore the window insets so that, on round watches with a
            // "chin", the watch face is centered on the entire screen, not just the usable
            // portion.
            float centerX = width / 2f;
            float centerY = height / 2f;

            // Analogue
            // Draw the ticks.
            float innerTickRadius = centerX - 10;
            float outerTickRadius = centerX;
            float innerShortTickRadius = centerX - 13;
            float outerShortTickRadius = centerX - 23;
            for (int tickIndex = 0; tickIndex < 12; tickIndex++) {
                float tickRot = (float) (tickIndex * Math.PI * 2 / 12);
                float innerX = (float) Math.sin(tickRot) * innerTickRadius;
                float innerY = (float) -Math.cos(tickRot) * innerTickRadius;
                float outerX = (float) Math.sin(tickRot) * outerTickRadius;
                float outerY = (float) -Math.cos(tickRot) * outerTickRadius;

                if (!isInAmbientMode())
                    canvas.drawLine(centerX + innerX, centerY + innerY, centerX + outerX, centerY + outerY, mHourTickPaint);

                float innerShortX = (float) Math.sin(tickRot) * innerShortTickRadius;
                float innerShortY = (float) -Math.cos(tickRot) * innerShortTickRadius;
                float outerShortX = (float) Math.sin(tickRot) * outerShortTickRadius;
                float outerShortY = (float) -Math.cos(tickRot) * outerShortTickRadius;
                canvas.drawLine(centerX + innerShortX, centerY + innerShortY, centerX + outerShortX, centerY + outerShortY, mHourTickPaint);
            }

            // Draw the minute ticks.
            if (!isInAmbientMode()) {
                float innerMinuteTickRadius = centerX - 7;
                float outerMinuteTickRadius = centerX;
                for (int tickIndex = 0; tickIndex < 60; tickIndex++) {
                    float tickRot = (float) (tickIndex * Math.PI * 2 / 60);
                    float innerX = (float) Math.sin(tickRot) * innerMinuteTickRadius;
                    float innerY = (float) -Math.cos(tickRot) * innerMinuteTickRadius;
                    float outerX = (float) Math.sin(tickRot) * outerMinuteTickRadius;
                    float outerY = (float) -Math.cos(tickRot) * outerMinuteTickRadius;
                    canvas.drawLine(centerX + innerX, centerY + innerY, centerX + outerX, centerY + outerY, mMinuteTickPaint);
                }
            }

            float secRot = mTime.second / 30f * (float) Math.PI;
            int minutes = mTime.minute;
            float minRot = minutes / 30f * (float) Math.PI;
            float hrRot = ((mTime.hour + (minutes / 60f)) / 6f) * (float) Math.PI;

            float secLength = centerX - 20;
            float minLength = centerX - 35;
            float hrLength = centerX - 75;
            float offset = centerX / 4;

            if (!isInAmbientMode()) {
                float secX = (float) Math.sin(secRot) * secLength;
                float secY = (float) -Math.cos(secRot) * secLength;
                float secStartX = (float) Math.sin(secRot) * offset;
                float secStartY = (float) -Math.cos(secRot) * offset;

                mSecondPaint.setStyle(Paint.Style.STROKE);
                mSecondPaint.setColor(Color.parseColor(mBackgroundColor));
                mSecondPaint.setAlpha(255);
                canvas.drawLine(centerX + secStartX, centerY + secStartY, centerX + secX, centerY + secY, mSecondPaint);

                mSecondPaint.setStyle(Paint.Style.FILL);
                mSecondPaint.setColor(Color.parseColor(mAccentColor));
                mSecondPaint.setAlpha(foregroundOpacityLevel);
                canvas.drawLine(centerX + secStartX, centerY + secStartY, centerX + secX, centerY + secY, mSecondPaint);
            }

            float minX = (float) Math.sin(minRot) * minLength;
            float minY = (float) -Math.cos(minRot) * minLength;
            float minStartX = (float) Math.sin(minRot) * offset;
            float minStartY = (float) -Math.cos(minRot) * offset;

            mMinutePaint.setStyle(Paint.Style.STROKE);
            mMinutePaint.setColor(Color.parseColor(mBackgroundColor));
            mMinutePaint.setAlpha(255);
            canvas.drawLine(centerX + minStartX, centerY + minStartY, centerX + minX, centerY + minY, mMinutePaint);

            mMinutePaint.setStyle(Paint.Style.FILL);
            mMinutePaint.setColor(Color.parseColor(mForegroundColor));
            mMinutePaint.setAlpha(foregroundOpacityLevel);
            canvas.drawLine(centerX + minStartX, centerY + minStartY, centerX + minX, centerY + minY, mMinutePaint);

            float hrX = (float) Math.sin(hrRot) * hrLength;
            float hrY = (float) -Math.cos(hrRot) * hrLength;
            float hrStartX = (float) Math.sin(hrRot) * offset;
            float hrStartY = (float) -Math.cos(hrRot) * offset;

            mHourPaint.setStyle(Paint.Style.STROKE);
            mHourPaint.setColor(Color.parseColor(mBackgroundColor));
            mHourPaint.setAlpha(255);
            canvas.drawLine(centerX + hrStartX, centerY + hrStartY, centerX + hrX, centerY + hrY, mHourPaint);

            mHourPaint.setStyle(Paint.Style.FILL);
            mHourPaint.setColor(Color.parseColor(mForegroundColor));
            mHourPaint.setAlpha(foregroundOpacityLevel);
            canvas.drawLine(centerX + hrStartX, centerY + hrStartY, centerX + hrX, centerY + hrY, mHourPaint);

            // Digital
            // Draw the hours.
            float x = centerX - mXOffset;
            String hourString = formatTwoDigitHourNumber(mTime.hour);

            mDigitalHourPaint.setStyle(Paint.Style.STROKE);
            mDigitalHourPaint.setColor(isInAmbientMode() ? Color.parseColor(mForegroundColor) : Color.parseColor(mBackgroundColor));
            mDigitalHourPaint.setAlpha(isInAmbientMode() ? foregroundOpacityLevel : 255);
            canvas.drawText(hourString, x, centerY + mYOffset, mDigitalHourPaint);

            mDigitalHourPaint.setStyle(Paint.Style.FILL);
            mDigitalHourPaint.setColor(isInAmbientMode() ? Color.parseColor(mBackgroundColor) : Color.parseColor(mForegroundColor));
            mDigitalHourPaint.setAlpha(isInAmbientMode() ? 255 : foregroundOpacityLevel);
            canvas.drawText(hourString, x, centerY + mYOffset, mDigitalHourPaint);

            x += mDigitalHourPaint.measureText(hourString);

            mColonPaint.setStyle(Paint.Style.STROKE);
            mColonPaint.setColor(isInAmbientMode() ? Color.parseColor(mMiddleColor) : Color.parseColor(mBackgroundColor));
            mColonPaint.setAlpha(isInAmbientMode() ? foregroundOpacityLevel : 255);
            canvas.drawText(COLON_STRING, x, centerY + mYOffset, mColonPaint);

            mColonPaint.setStyle(Paint.Style.FILL);
            mColonPaint.setColor(isInAmbientMode() ? Color.parseColor(mBackgroundColor) : Color.parseColor(mMiddleColor));
            mColonPaint.setAlpha(isInAmbientMode() ? 255 : foregroundOpacityLevel);
            canvas.drawText(COLON_STRING, x, centerY + mYOffset, mColonPaint);

            x += mColonWidth;

            // Draw the minutes.
            String minuteString = formatTwoDigitNumber(mTime.minute);

            mDigitalMinutePaint.setStyle(Paint.Style.STROKE);
            mDigitalMinutePaint.setColor(isInAmbientMode() ? Color.parseColor(mForegroundColor) : Color.parseColor(mBackgroundColor));
            mDigitalMinutePaint.setAlpha(isInAmbientMode() ? foregroundOpacityLevel : 255);
            canvas.drawText(minuteString, x, centerY + mYOffset, mDigitalMinutePaint);

            mDigitalMinutePaint.setStyle(Paint.Style.FILL);
            mDigitalMinutePaint.setColor(isInAmbientMode() ? Color.parseColor(mBackgroundColor) : Color.parseColor(mForegroundColor));
            mDigitalMinutePaint.setAlpha(isInAmbientMode() ? 255 : foregroundOpacityLevel);
            canvas.drawText(minuteString, x, centerY + mYOffset, mDigitalMinutePaint);

            // Draw AM/PM indicator
            if (m12Hour) {
                x += mDigitalMinutePaint.measureText(minuteString);

                mDigitalAmPmPaint.setStyle(Paint.Style.STROKE);
                mDigitalAmPmPaint.setColor(Color.parseColor(mBackgroundColor));
                mDigitalAmPmPaint.setAlpha(255);
                canvas.drawText(getAmPmString(mTime.hour), x, centerY + mYOffset, mDigitalAmPmPaint);

                mDigitalAmPmPaint.setStyle(Paint.Style.FILL);
                mDigitalAmPmPaint.setColor(Color.parseColor(mForegroundColor));
                mDigitalAmPmPaint.setAlpha(foregroundOpacityLevel);
                canvas.drawText(getAmPmString(mTime.hour), x, centerY + mYOffset, mDigitalAmPmPaint);
            }

            // Draw the Day, Date.
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, d");
            String dayString = sdf.format(new Date(mTime.toMillis(true)));

            mTextElementPaint.setStyle(Paint.Style.STROKE);
            mTextElementPaint.setColor(Color.parseColor(mBackgroundColor));
            mTextElementPaint.setAlpha(255);
            canvas.drawText(dayString, (centerX * 1.5f) - 5, centerY + smallTextOffset, mTextElementPaint);

            mTextElementPaint.setStyle(Paint.Style.FILL);
            mTextElementPaint.setColor(Color.parseColor(mForegroundColor));
            mTextElementPaint.setAlpha(foregroundOpacityLevel);
            canvas.drawText(dayString, (centerX * 1.5f) - 5, centerY + smallTextOffset, mTextElementPaint);

            // Draw Battery icon
            Path batteryIcon = new Path();
            batteryIcon.moveTo((centerX / 2f) - 35f, centerY + smallTextOffset);
            batteryIcon.rLineTo(0, -13);
            batteryIcon.rLineTo(2, 0);
            batteryIcon.rLineTo(0, -2);
            batteryIcon.rLineTo(5, 0);
            batteryIcon.rLineTo(0, 2);
            batteryIcon.rLineTo(2, 0);
            batteryIcon.rLineTo(0, 13);
            batteryIcon.close();

            mBatteryFullPaint.setColor(Color.parseColor(mBackgroundColor));
            mBatteryFullPaint.setAlpha(255);
            canvas.drawPath(batteryIcon, mBatteryFullPaint);

            mBatteryFullPaint.setColor(Color.parseColor(mMiddleColor));
            mBatteryFullPaint.setAlpha(foregroundOpacityLevel);
            canvas.drawPath(batteryIcon, mBatteryFullPaint);

            float batteryHeight = (float)Math.ceil(15f * batteryLevel / 100f);

            Path batteryIconLevel = new Path();
            batteryIconLevel.moveTo((centerX / 2f) - 35f, centerY + smallTextOffset);

            if (batteryHeight >= 13) {
                batteryIconLevel.rLineTo(0, -13);
                batteryIconLevel.rLineTo(2, 0);
                batteryIconLevel.rLineTo(0, -(batteryHeight - 13));
                batteryIconLevel.rLineTo(5, 0);
                batteryIconLevel.rLineTo(0, (batteryHeight - 13));
                batteryIconLevel.rLineTo(2, 0);
                batteryIconLevel.rLineTo(0, 13);
            } else {
                batteryIconLevel.rLineTo(0, -batteryHeight);
                batteryIconLevel.rLineTo(9, 0);
                batteryIconLevel.rLineTo(0, batteryHeight);
            }

            batteryIconLevel.close();

            canvas.drawPath(batteryIconLevel, mBatteryPaint);

            // Battery level
            mTextElementPaint.setStyle(Paint.Style.STROKE);
            mTextElementPaint.setColor(Color.parseColor(mBackgroundColor));
            mTextElementPaint.setAlpha(255);
            canvas.drawText(String.valueOf(batteryLevel), (centerX / 2f) - 20f, centerY + smallTextOffset, mTextElementPaint);

            mTextElementPaint.setStyle(Paint.Style.FILL);
            mTextElementPaint.setColor(Color.parseColor(mForegroundColor));
            mTextElementPaint.setAlpha(foregroundOpacityLevel);
            canvas.drawText(String.valueOf(batteryLevel), (centerX / 2f) - 20f, centerY + smallTextOffset, mTextElementPaint);

            // Widgets
            // weather widget
            if (mShowWeather) {
                if (temperatureC != -999 && temperatureF != -999 && code != WatchFaceUtil.WeatherCodes.UNKNOWN) {
                    // Draw temperature
                    mTextElementPaint.setStyle(Paint.Style.STROKE);
                    mTextElementPaint.setColor(Color.parseColor(mBackgroundColor));
                    mTextElementPaint.setAlpha(255);
                    canvas.drawText(String.valueOf(mFahrenheit ? temperatureF : temperatureC) + getString(R.string.degrees), centerX + 3f, centerY * 0.6f, mTextElementPaint);

                    mTextElementPaint.setStyle(Paint.Style.FILL);
                    mTextElementPaint.setColor(Color.parseColor(mForegroundColor));
                    mTextElementPaint.setAlpha(foregroundOpacityLevel);
                    canvas.drawText(String.valueOf(mFahrenheit ? temperatureF : temperatureC) + getString(R.string.degrees), centerX + 3f, centerY * 0.6f, mTextElementPaint);

                    // Draw icon based on conditions
                    switch (code) {
                        case WatchFaceUtil.WeatherCodes.SUNNY:
                            mWidgetWeatherPaint.setColor(Color.parseColor(mBackgroundColor));
                            mWidgetWeatherPaint.setAlpha(255);
                            mWidgetWeatherPaint.setStyle(Paint.Style.STROKE);
                            mWidgetWeatherPaint.setStrokeWidth(2f);
                            canvas.drawCircle(centerX - 15f, (centerY * 0.6f) - 8, 10, mWidgetWeatherPaint);

                            mWidgetWeatherPaint.setColor(Color.parseColor(mForegroundColor));
                            mWidgetWeatherPaint.setAlpha(foregroundOpacityLevel);
                            mWidgetWeatherPaint.setStyle(Paint.Style.STROKE);
                            mWidgetWeatherPaint.setStrokeWidth(1f);
                            canvas.drawCircle(centerX - 15f, (centerY * 0.6f) - 8, 10, mWidgetWeatherPaint);
                            break;
                        case WatchFaceUtil.WeatherCodes.PARTLY_CLOUDY:
                            mWidgetWeatherPaint.setColor(Color.parseColor(mBackgroundColor));
                            mWidgetWeatherPaint.setAlpha(255);
                            mWidgetWeatherPaint.setStyle(Paint.Style.STROKE);
                            mWidgetWeatherPaint.setStrokeWidth(2f);
                            canvas.drawCircle(centerX - 15f, (centerY * 0.6f) - 8, 10, mWidgetWeatherPaint);

                            mWidgetWeatherPaint.setColor(Color.parseColor(mForegroundColor));
                            mWidgetWeatherPaint.setAlpha(foregroundOpacityLevel);
                            mWidgetWeatherPaint.setStyle(Paint.Style.STROKE);
                            mWidgetWeatherPaint.setStrokeWidth(1f);
                            canvas.drawCircle(centerX - 15f, (centerY * 0.6f) - 8, 10, mWidgetWeatherPaint);

                            mWidgetWeatherPaint.setColor(Color.parseColor(mBackgroundColor));
                            mWidgetWeatherPaint.setAlpha(255);
                            mWidgetWeatherPaint.setStyle(Paint.Style.STROKE);
                            mWidgetWeatherPaint.setStrokeWidth(2f);
                            canvas.drawLine(centerX - 25f, (centerY * 0.6f) - 8, centerX - 5f, (centerY * 0.6f) - 8, mWidgetWeatherPaint);

                            mWidgetWeatherPaint.setColor(Color.parseColor(mForegroundColor));
                            mWidgetWeatherPaint.setAlpha(foregroundOpacityLevel);
                            mWidgetWeatherPaint.setStyle(Paint.Style.STROKE);
                            mWidgetWeatherPaint.setStrokeWidth(1f);
                            canvas.drawLine(centerX - 25f, (centerY * 0.6f) - 8, centerX - 5f, (centerY * 0.6f) - 8, mWidgetWeatherPaint);
                            break;

                        // TODO: other condition icons

                        default:
                            mWidgetWeatherPaint.setColor(Color.parseColor(mBackgroundColor));
                            mWidgetWeatherPaint.setAlpha(255);
                            mWidgetWeatherPaint.setStyle(Paint.Style.STROKE);
                            mWidgetWeatherPaint.setStrokeWidth(2f);
                            canvas.drawLine(centerX - 5f, (centerY * 0.6f) - 8, centerX + 5f, (centerY * 0.6f) - 8, mWidgetWeatherPaint);

                            mWidgetWeatherPaint.setColor(Color.parseColor(mForegroundColor));
                            mWidgetWeatherPaint.setAlpha(foregroundOpacityLevel);
                            mWidgetWeatherPaint.setStyle(Paint.Style.STROKE);
                            mWidgetWeatherPaint.setStrokeWidth(1f);
                            canvas.drawLine(centerX - 5f, (centerY * 0.6f) - 8, centerX + 5f, (centerY * 0.6f) - 8, mWidgetWeatherPaint);
                            break;
                    }
                } else {
                    // No weather data to display
                    mWidgetWeatherPaint.setColor(Color.parseColor(mBackgroundColor));
                    mWidgetWeatherPaint.setAlpha(255);
                    mWidgetWeatherPaint.setStyle(Paint.Style.STROKE);
                    mWidgetWeatherPaint.setStrokeWidth(2f);
                    canvas.drawLine(centerX - 5f, (centerY * 0.6f) - 8, centerX + 5f, (centerY * 0.6f) - 8, mWidgetWeatherPaint);

                    mWidgetWeatherPaint.setColor(Color.parseColor(mForegroundColor));
                    mWidgetWeatherPaint.setAlpha(foregroundOpacityLevel);
                    mWidgetWeatherPaint.setStyle(Paint.Style.FILL);
                    mWidgetWeatherPaint.setStrokeWidth(1f);
                    canvas.drawLine(centerX - 5f, (centerY * 0.6f) - 8, centerX + 5f, (centerY * 0.6f) - 8, mWidgetWeatherPaint);
                }
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onVisibilityChanged: " + visible);
            }

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
                    //Wearable.MessageApi.removeListener(mGoogleApiClient, this);
                    mGoogleApiClient.disconnect();
                }
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override  // GoogleApiClient.ConnectionCallbacks
        public void onConnected(Bundle connectionHint) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onConnected: " + connectionHint);
            }

            Wearable.DataApi.addListener(mGoogleApiClient, Engine.this);
            //Wearable.MessageApi.addListener(mGoogleApiClient, Engine.this);

            WatchFaceUtil.fetchConfigDataMap(mGoogleApiClient, fetchConfigCallback);

            mUpdateHandler.sendEmptyMessage(MSG_REFRESH_WEATHER);
        }

        @Override  // GoogleApiClient.ConnectionCallbacks
        public void onConnectionSuspended(int cause) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onConnectionSuspended: " + cause);
            }

            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            //Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }

        @Override  // GoogleApiClient.OnConnectionFailedListener
        public void onConnectionFailed(ConnectionResult result) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onConnectionFailed: " + result);
            }

            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            //Wearable.MessageApi.removeListener(mGoogleApiClient, this);
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
                    this.config = config;

                    if (config.getBoolean(WatchFaceUtil.KEY_WIDGET_SHOW_WEATHER) && config.getInt(WatchFaceUtil.KEY_WIDGET_WEATHER_DATA_CODE) == WatchFaceUtil.WeatherCodes.UNKNOWN)
                        mUpdateHandler.sendEmptyMessage(MSG_REFRESH_WEATHER);

                    updateUI(config);

                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Config DataItem updated:" + config);
                    }
                }
            } finally {
                dataEvents.close();
            }
        }
        //endregion

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

        private void updateUI(DataMap config) {
            setDefaultValuesForMissingConfigKeys(config);

            m12Hour = config.getBoolean(WatchFaceUtil.KEY_12HOUR_FORMAT);
            mShowWeather = config.getBoolean(WatchFaceUtil.KEY_WIDGET_SHOW_WEATHER);
            mFahrenheit = config.getBoolean(WatchFaceUtil.KEY_WIDGET_WEATHER_FAHRENHEIT);

            temperatureC = config.getInt(WatchFaceUtil.KEY_WIDGET_WEATHER_DATA_TEMPERATURE_C);
            temperatureF = config.getInt(WatchFaceUtil.KEY_WIDGET_WEATHER_DATA_TEMPERATURE_F);
            code = config.getInt(WatchFaceUtil.KEY_WIDGET_WEATHER_DATA_CODE);

            if (!isInAmbientMode()) {
                setBackgroundColor(config.getString(WatchFaceUtil.KEY_BACKGROUND_COLOUR));
                setMiddleColor(config.getString(WatchFaceUtil.KEY_MIDDLE_COLOUR));
                setForegroundColor(config.getString(WatchFaceUtil.KEY_FOREGROUND_COLOUR));
                setAccentColor(config.getString(WatchFaceUtil.KEY_ACCENT_COLOUR));

                // TODO: change preview image??
            }

            invalidate();
        }

        private void cancelRefreshWeatherTask() {
            if (mRefreshWeatherTask != null) {
                mRefreshWeatherTask.cancel(true);
            }
        }

        //region Timer methods
        /**
         * Starts the {@link #mUpdateHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "updateTimer");
            }
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

        //region Colour methods
        private void updatePaint(Paint paint, String colour, int opacityLevel) {
            paint.setColor(Color.parseColor(colour));
            paint.setAlpha(opacityLevel);
        }
        private void updatePaint(Paint paint, String colour, int opacityLevel, float strokeWidth) {
            updatePaint(paint, colour, opacityLevel);
            paint.setStrokeWidth(isInAmbientMode() ? 2f : strokeWidth);
        }

        private void setBackgroundColor(String color) {
            mBackgroundColor = color;
            updatePaint(mBackgroundPaint, color, 255);
        }

        private void setForegroundColor(String color) {
            mForegroundColor = color;

            updatePaint(mHourPaint, color, foregroundOpacityLevel, 3f);
            updatePaint(mMinutePaint, color, foregroundOpacityLevel, 3f);

            updatePaint(mDigitalHourPaint, color, foregroundOpacityLevel);
            updatePaint(mDigitalMinutePaint, color, foregroundOpacityLevel);

            updatePaint(mTextElementPaint, color, foregroundOpacityLevel);
            updatePaint(mBatteryPaint, color, foregroundOpacityLevel);

            updatePaint(mHourTickPaint, color, 100);
            updatePaint(mMinuteTickPaint, color, 100);
        }

        private void setAccentColor(String color) {
            mAccentColor = color;
            updatePaint(mSecondPaint, color, accentOpacityLevel, 2f);
        }

        private void setMiddleColor(String color) {
            mMiddleColor = color;
            updatePaint(mColonPaint, color, foregroundOpacityLevel);
            updatePaint(mBatteryFullPaint, color, foregroundOpacityLevel);
        }

        private Paint createTextPaint(int defaultInteractiveColour) {
            Paint paint = new Paint();
            paint.setColor(defaultInteractiveColour);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            paint.setStrokeJoin(Paint.Join.BEVEL);
            paint.setStrokeWidth(2f);
            return paint;
        }
        //endregion

        //region Config Data methods
        private void setDefaultValuesForMissingConfigKeys(DataMap config) {
            addBooleanKeyIfMissing(config, WatchFaceUtil.KEY_12HOUR_FORMAT, WatchFaceUtil.CONFIG_12HOUR_DEFAULT);
            addBooleanKeyIfMissing(config, WatchFaceUtil.KEY_WIDGET_SHOW_WEATHER, WatchFaceUtil.CONFIG_WIDGET_SHOW_WEATHER_DEFAULT);
            addBooleanKeyIfMissing(config, WatchFaceUtil.KEY_WIDGET_WEATHER_FAHRENHEIT, WatchFaceUtil.CONFIG_WIDGET_FAHRENHEIT_DEFAULT);
            addBooleanKeyIfMissing(config, WatchFaceUtil.KEY_WIDGET_WEATHER_AUTO_LOCATION, WatchFaceUtil.CONFIG_AUTO_LOCATION_DEFAULT);

            addStringKeyIfMissing(config, WatchFaceUtil.KEY_BACKGROUND_COLOUR, WatchFaceUtil.COLOUR_NAME_DEFAULT_AND_AMBIENT_BACKGROUND);
            addStringKeyIfMissing(config, WatchFaceUtil.KEY_MIDDLE_COLOUR, WatchFaceUtil.COLOUR_NAME_DEFAULT_AND_AMBIENT_MIDDLE);
            addStringKeyIfMissing(config, WatchFaceUtil.KEY_FOREGROUND_COLOUR, WatchFaceUtil.COLOUR_NAME_DEFAULT_AND_AMBIENT_FOREGROUND);
            addStringKeyIfMissing(config, WatchFaceUtil.KEY_ACCENT_COLOUR, WatchFaceUtil.COLOUR_NAME_DEFAULT_AND_AMBIENT_ACCENT);
            addStringKeyIfMissing(config, WatchFaceUtil.KEY_WIDGET_WEATHER_LOCATION, WatchFaceUtil.CONFIG_LOCATION_DEFAULT);

            addIntKeyIfMissing(config, WatchFaceUtil.KEY_WIDGET_WEATHER_DATA_TEMPERATURE_C, WatchFaceUtil.WIDGET_WEATHER_DATA_TEMPERATURE_C_DEFAULT);
            addIntKeyIfMissing(config, WatchFaceUtil.KEY_WIDGET_WEATHER_DATA_TEMPERATURE_F, WatchFaceUtil.WIDGET_WEATHER_DATA_TEMPERATURE_F_DEFAULT);
            addIntKeyIfMissing(config, WatchFaceUtil.KEY_WIDGET_WEATHER_DATA_CODE, WatchFaceUtil.WIDGET_WEATHER_DATA_CODE_DEFAULT);

            addLongKeyIfMissing(config, WatchFaceUtil.KEY_WIDGET_WEATHER_DATA_DATETIME, 0);
        }

        private void addBooleanKeyIfMissing(DataMap config, String key, boolean value) {
            if (!config.containsKey(key)) {
                config.putBoolean(key, value);
            }
        }

        private void addStringKeyIfMissing(DataMap config, String key, String value) {
            if (!config.containsKey(key)) {
                config.putString(key, value);
            }
        }

        private void addLongKeyIfMissing(DataMap config, String key, long value) {
            if (!config.containsKey(key)) {
                config.putLong(key, value);
            }
        }

        private void addIntKeyIfMissing(DataMap config, String key, int value) {
            if (!config.containsKey(key)) {
                config.putInt(key, value);
            }
        }
        //endregion

        //region String methods
        private String formatTwoDigitNumber(int number) {
            return String.format("%02d", number);
        }

        private String formatTwoDigitHourNumber(int hour) {
            if (m12Hour)
                return String.format("%02d", convertTo12Hour(hour));
            else
                return String.format("%02d", hour);
        }

        private int convertTo12Hour(int hour) {
            int result = hour % 12;
            return (result == 0) ? 12 : result;
        }

        private String getAmPmString(int hour) {
            return (hour < 12) ? mAmString : mPmString;
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
                                byte[] rawData = config.toByteArray();
                                for (Node node : result.getNodes()) {
                                    String nodeId = node.getId();
                                    Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, WatchFaceUtil.PATH_DIGILOGUE_SETTINGS, rawData);
                                }
                            }
                        });

                return null;
            }

            @Override
            protected void onPostExecute(Void temp) {
                releaseWakeLock();
                onWeatherRefreshed();
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
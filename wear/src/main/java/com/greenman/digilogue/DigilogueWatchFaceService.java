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
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
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

    private static final Typeface BOLD_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
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
        long REFRESH_WEATHER_DELAY_HOURS = WatchFaceUtil.CONFIG_WIDGET_WEATHER_UPDATE_FREQUENCY_DEFAULT;

        private AsyncTask<Void, Void, Void> mRefreshWeatherTask;


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

        Time mTime;

        float mXOffset;
        float mYOffset;
        float smallTextOffset;
        float mColonWidth;

        int batteryLevel = 100;
        int foregroundOpacityLevel;
        int accentOpacityLevel;

        int temperature = 23;

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
                updateUI(config);
            }
        };

        private void onWeatherRefreshed() {
            temperature = mTime.second;

            invalidate();

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

            mDigitalHourPaint = createTextPaint(Color.parseColor(mForegroundColor), BOLD_TYPEFACE);
            mDigitalMinutePaint = createTextPaint(Color.parseColor(mForegroundColor));
            mDigitalAmPmPaint = createTextPaint(Color.parseColor(mForegroundColor));
            mTextElementPaint = createTextPaint(Color.parseColor(mForegroundColor));
            mColonPaint = createTextPaint(Color.parseColor(mMiddleColor));

            mTime = new Time();

            foregroundOpacityLevel = mMute ? 100 : 255;
            accentOpacityLevel = mMute ? 80 : 255;
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
            mDigitalHourPaint.setTypeface(burnInProtection ? NORMAL_TYPEFACE : BOLD_TYPEFACE);

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
            foregroundOpacityLevel = inMuteMode ? 100 : 255;
            accentOpacityLevel = inMuteMode ? 80 : 255;

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
                canvas.drawLine(centerX + innerX, centerY + innerY, centerX + outerX, centerY + outerY, mHourTickPaint);

                float innerShortX = (float) Math.sin(tickRot) * innerShortTickRadius;
                float innerShortY = (float) -Math.cos(tickRot) * innerShortTickRadius;
                float outerShortX = (float) Math.sin(tickRot) * outerShortTickRadius;
                float outerShortY = (float) -Math.cos(tickRot) * outerShortTickRadius;
                canvas.drawLine(centerX + innerShortX, centerY + innerShortY, centerX + outerShortX, centerY + outerShortY, mHourTickPaint);
            }

            // Draw the minute ticks.
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

            float secRot = mTime.second / 30f * (float) Math.PI;
            int minutes = mTime.minute;
            float minRot = minutes / 30f * (float) Math.PI;
            float hrRot = ((mTime.hour + (minutes / 60f)) / 6f) * (float) Math.PI;

            float secLength = centerX - 20;
            float minLength = centerX - 40;
            float hrLength = centerX - 80;
            float offset = centerX / 4;

            if (!isInAmbientMode()) {
                float secX = (float) Math.sin(secRot) * secLength;
                float secY = (float) -Math.cos(secRot) * secLength;
                float secStartX = (float) Math.sin(secRot) * offset;
                float secStartY = (float) -Math.cos(secRot) * offset;
                canvas.drawLine(centerX + secStartX, centerY + secStartY, centerX + secX, centerY + secY, mSecondPaint);
            }

            float minX = (float) Math.sin(minRot) * minLength;
            float minY = (float) -Math.cos(minRot) * minLength;
            float minStartX = (float) Math.sin(minRot) * offset;
            float minStartY = (float) -Math.cos(minRot) * offset;
            canvas.drawLine(centerX + minStartX, centerY + minStartY, centerX + minX, centerY + minY, mMinutePaint);

            float hrX = (float) Math.sin(hrRot) * hrLength;
            float hrY = (float) -Math.cos(hrRot) * hrLength;
            float hrStartX = (float) Math.sin(hrRot) * offset;
            float hrStartY = (float) -Math.cos(hrRot) * offset;
            canvas.drawLine(centerX + hrStartX, centerY + hrStartY, centerX + hrX, centerY + hrY, mHourPaint);

            // Digital
            // Draw the hours.
            float x = centerX - mXOffset;
            String hourString = formatTwoDigitNumber(mTime.hour);

            mDigitalHourPaint.setStyle(Paint.Style.STROKE);
            mDigitalHourPaint.setColor(Color.parseColor(mBackgroundColor));
            mDigitalHourPaint.setAlpha(255);
            canvas.drawText(hourString, x, centerY + mYOffset, mDigitalHourPaint);

            mDigitalHourPaint.setStyle(Paint.Style.FILL);
            mDigitalHourPaint.setColor(Color.parseColor(mForegroundColor));
            mDigitalHourPaint.setAlpha(foregroundOpacityLevel);
            canvas.drawText(hourString, x, centerY + mYOffset, mDigitalHourPaint);

            x += mDigitalHourPaint.measureText(hourString);

            mColonPaint.setStyle(Paint.Style.STROKE);
            mColonPaint.setColor(Color.parseColor(mBackgroundColor));
            mColonPaint.setAlpha(255);
            canvas.drawText(COLON_STRING, x, centerY + mYOffset, mColonPaint);

            mColonPaint.setStyle(Paint.Style.FILL);
            mColonPaint.setColor(Color.parseColor(mMiddleColor));
            mColonPaint.setAlpha(foregroundOpacityLevel);
            canvas.drawText(COLON_STRING, x, centerY + mYOffset, mColonPaint);

            x += mColonWidth;

            // Draw the minutes.
            String minuteString = formatTwoDigitNumber(mTime.minute);

            mDigitalMinutePaint.setStyle(Paint.Style.STROKE);
            mDigitalMinutePaint.setColor(Color.parseColor(mBackgroundColor));
            mDigitalMinutePaint.setAlpha(255);
            canvas.drawText(minuteString, x, centerY + mYOffset, mDigitalMinutePaint);

            mDigitalMinutePaint.setStyle(Paint.Style.FILL);
            mDigitalMinutePaint.setColor(Color.parseColor(mForegroundColor));
            mDigitalMinutePaint.setAlpha(foregroundOpacityLevel);
            canvas.drawText(minuteString, x, centerY + mYOffset, mDigitalMinutePaint);

            // Draw AM/PM indicator
            if (m12Hour) {
                x += mDigitalHourPaint.measureText(minuteString);

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
                // Draw temperature
                mTextElementPaint.setStyle(Paint.Style.STROKE);
                mTextElementPaint.setColor(Color.parseColor(mBackgroundColor));
                mTextElementPaint.setAlpha(255);
                canvas.drawText(String.valueOf(temperature) + getString(R.string.degrees), centerX + 3f, centerY * 0.6f, mTextElementPaint);

                mTextElementPaint.setStyle(Paint.Style.FILL);
                mTextElementPaint.setColor(Color.parseColor(mForegroundColor));
                mTextElementPaint.setAlpha(foregroundOpacityLevel);
                canvas.drawText(String.valueOf(temperature) + getString(R.string.degrees), centerX + 3f, centerY * 0.6f, mTextElementPaint);

                // TODO: Draw icon based on conditions
                mWidgetWeatherPaint.setColor(Color.parseColor(mBackgroundColor));
                mWidgetWeatherPaint.setAlpha(255);
                canvas.drawCircle(centerX - 15f, (centerY * 0.6f) - 8, 10, mWidgetWeatherPaint);

                mWidgetWeatherPaint.setColor(Color.parseColor(mForegroundColor));
                mWidgetWeatherPaint.setAlpha(foregroundOpacityLevel);
                canvas.drawCircle(centerX - 15f, (centerY * 0.6f) - 8, 10, mWidgetWeatherPaint);
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

            // TODO: check this
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
                    if (!dataItem.getUri().getPath().equals(WatchFaceUtil.PATH_DIGILOGUE_SETTINGS)) {
                        continue;
                    }

                    DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                    DataMap config = dataMapItem.getDataMap();
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Config DataItem updated:" + config);
                    }
                    updateUI(config);

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
            REFRESH_WEATHER_DELAY_HOURS = config.getLong(WatchFaceUtil.KEY_WIDGET_WEATHER_UPDATE_FREQUENCY);

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

        private void setBackgroundColor(String color) {
            mBackgroundColor = color;
            updatePaint(mBackgroundPaint, color, 255);
        }

        private void setForegroundColor(String color) {
            mForegroundColor = color;

            updatePaint(mHourPaint, color, foregroundOpacityLevel);
            updatePaint(mMinutePaint, color, foregroundOpacityLevel);

            updatePaint(mDigitalHourPaint, color, foregroundOpacityLevel);
            updatePaint(mDigitalMinutePaint, color, foregroundOpacityLevel);

            updatePaint(mTextElementPaint, color, foregroundOpacityLevel);
            updatePaint(mBatteryPaint, color, foregroundOpacityLevel);

            updatePaint(mHourTickPaint, color, 100);
            updatePaint(mMinuteTickPaint, color, 100);
        }

        private void setAccentColor(String color) {
            mAccentColor = color;
            updatePaint(mSecondPaint, color, accentOpacityLevel);
        }

        private void setMiddleColor(String color) {
            mMiddleColor = color;
            updatePaint(mColonPaint, color, 255);
            updatePaint(mBatteryFullPaint, color, 255);
        }

        private Paint createTextPaint(int defaultInteractiveColor) {
            return createTextPaint(defaultInteractiveColor, NORMAL_TYPEFACE);
        }

        private Paint createTextPaint(int defaultInteractiveColour, Typeface typeface) {
            Paint paint = new Paint();
            paint.setColor(defaultInteractiveColour);
            paint.setTypeface(typeface);
            paint.setAntiAlias(true);
            paint.setStrokeJoin(Paint.Join.BEVEL);
            paint.setStrokeWidth(3);
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

            addLongKeyIfMissing(config, WatchFaceUtil.KEY_WIDGET_WEATHER_UPDATE_FREQUENCY, WatchFaceUtil.CONFIG_WIDGET_WEATHER_UPDATE_FREQUENCY_DEFAULT);
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
        //endregion

        //region String methods
        private String formatTwoDigitNumber(int hour) {
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

                // TODO: get weather data

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
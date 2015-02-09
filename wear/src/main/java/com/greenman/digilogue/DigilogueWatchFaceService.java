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
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
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

    private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, MessageApi.MessageListener {
        static final int MSG_UPDATE_TIME = 0;
        static final String COLON_STRING = ":";

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;
        boolean mMute;
        boolean mRegisteredTimeZoneReceiver = false;
        Time mTime;
        float mXOffset;
        float mYOffset;
        private int batteryLevel = 100;
        private float smallTextOffset;

        /** How often {@link #mUpdateTimeHandler} ticks in milliseconds. */
        long mInteractiveUpdateRateMs = INTERACTIVE_UPDATE_RATE_MS;

        private int foregroundOpacityLevel;
        private int accentOpacityLevel;

        /*int mBackgroundColor = WatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_BACKGROUND;
        int mMiddleColor = Color.parseColor("gray");
        int mForegroundColor = WatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_FOREGROUND;
        int mAccentColor = Color.parseColor("red");*/

        String mBackgroundColor = WatchFaceUtil.COLOR_NAME_DEFAULT_AND_AMBIENT_BACKGROUND;
        String mMiddleColor = "gray";
        String mForegroundColor = WatchFaceUtil.COLOR_NAME_DEFAULT_AND_AMBIENT_FOREGROUND;
        String mAccentColor = "red";

        // Face
        Paint mBackgroundPaint;
        Paint mHourTickPaint;
        Paint mMinuteTickPaint;
        Paint mBatteryFullPaint;
        Paint mBatteryPaint;
        Paint mElementPaint;

        // Analogue
        Paint mHourPaint;
        Paint mMinutePaint;
        Paint mSecondPaint;

        // Digital
        Paint mDigitalHourPaint;
        Paint mDigitalMinutePaint;
        Paint mColonPaint;
        float mColonWidth;

        /**
         * Handler to update the time once a second in interactive mode.
         */
        final Handler mUpdateTimeHandler = new Handler() {
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
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(DigilogueWatchFaceService.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

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

        @Override
        public void onCreate(SurfaceHolder holder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCreate");
            }
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(DigilogueWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            Resources resources = DigilogueWatchFaceService.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);

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

            mBatteryPaint = new Paint();
            mBatteryPaint.setColor(Color.parseColor(mForegroundColor));
            mBatteryPaint.setStrokeWidth(1f);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(Color.parseColor(mBackgroundColor));

            mDigitalHourPaint = createTextPaint(Color.parseColor(mForegroundColor), BOLD_TYPEFACE);
            mDigitalMinutePaint = createTextPaint(Color.parseColor(mForegroundColor));
            mElementPaint = createTextPaint(Color.parseColor(mForegroundColor));
            mColonPaint = createTextPaint(Color.parseColor(mMiddleColor));

            mTime = new Time();

            foregroundOpacityLevel = mMute ? 100 : 255;
            accentOpacityLevel = mMute ? 80 : 255;
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
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
            mElementPaint.setTextSize(textSize / 2f);
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

            /*if (isInAmbientMode()) {
                // TODO: possibly remove dimming on ambient or make it a user defined option
                setBackgroundColor(WatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_BACKGROUND);
                setForegroundColor(WatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_FOREGROUND);
                setAccentColor(WatchFaceUtil.COLOR_VALUE_DEFAULT_ACCENT);
            } else {
                WatchFaceUtil.fetchConfigDataMap(mGoogleApiClient,
                        new WatchFaceUtil.FetchConfigDataMapCallback() {
                            @Override
                            public void onConfigDataMapFetched(DataMap config) {
                                // If the DataItem hasn't been created yet or some keys are missing,
                                // use the default values.
                                setDefaultValuesForMissingConfigKeys(config);
                                WatchFaceUtil.putConfigDataItem(mGoogleApiClient, config);

                                updateUiForConfigDataMap(config);

                                setBackgroundColor(config.getInt(WatchFaceUtil.KEY_BACKGROUND_COLOR));
                                setForegroundColor(config.getInt(WatchFaceUtil.KEY_FOREGROUND_COLOR));
                                setAccentColor(Color.parseColor("red"));
                            }
                        }
                );
            }*/

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

            foregroundOpacityLevel = inMuteMode ? 100 : 255;
            accentOpacityLevel = inMuteMode ? 80 : 255;

            if (mMute != inMuteMode) {
                mMute = inMuteMode;
                mHourPaint.setAlpha(foregroundOpacityLevel);
                mMinutePaint.setAlpha(foregroundOpacityLevel);
                mSecondPaint.setAlpha(accentOpacityLevel);

                mDigitalHourPaint.setAlpha(foregroundOpacityLevel);
                mDigitalMinutePaint.setAlpha(foregroundOpacityLevel);

                mElementPaint.setAlpha(foregroundOpacityLevel);
                mBatteryFullPaint.setAlpha(foregroundOpacityLevel);
                mBatteryPaint.setAlpha(foregroundOpacityLevel);

                invalidate();
            }
        }

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

            updatePaint(mElementPaint, color, foregroundOpacityLevel);
            updatePaint(mBatteryPaint, color, foregroundOpacityLevel);

            updatePaint(mHourTickPaint, color, 100);
            updatePaint(mMinuteTickPaint, color, 100);
        }

        private void setAccentColor(String color) {
            mAccentColor = color;
            updatePaint(mSecondPaint, color, accentOpacityLevel);
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

            // Draw the Day, Date.
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, d");
            String dayString = sdf.format(new Date(mTime.toMillis(true)));

            mElementPaint.setStyle(Paint.Style.STROKE);
            mElementPaint.setColor(Color.parseColor(mBackgroundColor));
            mElementPaint.setAlpha(255);
            canvas.drawText(dayString, (centerX * 1.5f) - 5, centerY + smallTextOffset, mElementPaint);

            mElementPaint.setStyle(Paint.Style.FILL);
            mElementPaint.setColor(Color.parseColor(mForegroundColor));
            mElementPaint.setAlpha(foregroundOpacityLevel);
            canvas.drawText(dayString, (centerX * 1.5f) - 5, centerY + smallTextOffset, mElementPaint);

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
            mElementPaint.setStyle(Paint.Style.STROKE);
            mElementPaint.setColor(Color.parseColor(mBackgroundColor));
            mElementPaint.setAlpha(255);
            canvas.drawText(String.valueOf(batteryLevel), (centerX / 2f) - 20f, centerY + smallTextOffset, mElementPaint);

            mElementPaint.setStyle(Paint.Style.FILL);
            mElementPaint.setColor(Color.parseColor(mForegroundColor));
            mElementPaint.setAlpha(foregroundOpacityLevel);
            canvas.drawText(String.valueOf(batteryLevel), (centerX / 2f) - 20f, centerY + smallTextOffset, mElementPaint);
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
                    mGoogleApiClient.disconnect();
                }
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            DigilogueWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
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

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "updateTimer");
            }
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        private Paint createTextPaint(int defaultInteractiveColor) {
            return createTextPaint(defaultInteractiveColor, NORMAL_TYPEFACE);
        }

        private Paint createTextPaint(int defaultInteractiveColor, Typeface typeface) {
            Paint paint = new Paint();
            paint.setColor(defaultInteractiveColor);
            paint.setTypeface(typeface);
            paint.setAntiAlias(true);
            paint.setStrokeJoin(Paint.Join.BEVEL);
            paint.setStrokeWidth(3);
            return paint;
        }

        private void updateConfigDataItemAndUiOnStartup() {
            WatchFaceUtil.fetchConfigDataMap(mGoogleApiClient,
                    new WatchFaceUtil.FetchConfigDataMapCallback() {
                        @Override
                        public void onConfigDataMapFetched(DataMap startupConfig) {
                            // If the DataItem hasn't been created yet or some keys are missing,
                            // use the default values.
                            setDefaultValuesForMissingConfigKeys(startupConfig);
                            WatchFaceUtil.putConfigDataItem(mGoogleApiClient, startupConfig);

                            updateUiForConfigDataMap(startupConfig);

                            // TODO: change preview image
                        }
                    }
            );
        }

        private void setDefaultValuesForMissingConfigKeys(DataMap config) {
            addIntKeyIfMissing(config, WatchFaceUtil.KEY_BACKGROUND_COLOR, WatchFaceUtil.COLOR_NAME_DEFAULT_AND_AMBIENT_BACKGROUND);
            addIntKeyIfMissing(config, WatchFaceUtil.KEY_FOREGROUND_COLOR, WatchFaceUtil.COLOR_NAME_DEFAULT_AND_AMBIENT_FOREGROUND);
        }

        private void addIntKeyIfMissing(DataMap config, String key, String color) {
            if (!config.containsKey(key)) {
                config.putString(key, color);
            }
        }

        @Override // DataApi.DataListener
        public void onDataChanged(DataEventBuffer dataEvents) {
            try {
                for (DataEvent dataEvent : dataEvents) {
                    if (dataEvent.getType() != DataEvent.TYPE_CHANGED) {
                        continue;
                    }

                    DataItem dataItem = dataEvent.getDataItem();
                    if (!dataItem.getUri().getPath().equals(WatchFaceUtil.DIGILOGUE_COLOURS)) {
                        continue;
                    }

                    DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                    DataMap config = dataMapItem.getDataMap();
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Config DataItem updated:" + config);
                    }
                    updateUiForConfigDataMap(config);
                }
            } finally {
                dataEvents.close();
            }
        }

        private void updateUiForConfigDataMap(final DataMap config) {
            boolean uiUpdated = false;
            for (String configKey : config.keySet()) {
                if (!config.containsKey(configKey)) {
                    continue;
                }
                String colourName = config.getString(configKey);

                if (colourName == null || colourName.equals(""))
                    colourName = getString(R.string.color_black).toLowerCase();

                if (colourName.contains(" & ")) {
                    int index = colourName.indexOf(" & ");
                    mBackgroundColor = colourName.substring(0, index);
                    mForegroundColor = colourName.substring(index + 4, colourName.length() - (index + 4));
                    colourName = mBackgroundColor;
                }

                int color = Color.parseColor(colourName);
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Found watch face config key: " + configKey + " -> "
                            + Integer.toHexString(color));
                }
                if (updateUiForKey(WatchFaceUtil.KEY_FOREGROUND_COLOR, mBackgroundColor)) {
                    uiUpdated = true;
                }
                if (updateUiForKey(WatchFaceUtil.KEY_FOREGROUND_COLOR, mForegroundColor)) {
                    uiUpdated = true;
                }
            }
            if (uiUpdated) {
                invalidate();
            }
        }

        /**
         * Updates the color of a UI item according to the given {@code configKey}. Does nothing if
         * {@code configKey} isn't recognized.
         *
         * @return whether UI has been updated
         */
        private boolean updateUiForKey(String configKey, String color) {
            if (configKey.equals(WatchFaceUtil.KEY_BACKGROUND_COLOR)) {
                if (color == null || color.equals(""))
                    color = getString(R.string.color_black).toLowerCase();

                mBackgroundColor = color;
                setBackgroundColor(color);
            } else if (configKey.equals(WatchFaceUtil.KEY_FOREGROUND_COLOR)) {
                if (color == null || color.equals(""))
                    color = getString(R.string.color_black).toLowerCase();

                mForegroundColor = color;
                setForegroundColor(color);
            } else {
                Log.w(TAG, "Ignoring unknown config key: " + configKey);
                return false;
            }
            return true;
        }

        @Override  // GoogleApiClient.ConnectionCallbacks
        public void onConnected(Bundle connectionHint) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onConnected: " + connectionHint);
            }
            Wearable.DataApi.addListener(mGoogleApiClient, Engine.this);
            Wearable.MessageApi.addListener(mGoogleApiClient, Engine.this);
            updateConfigDataItemAndUiOnStartup();
        }

        @Override  // GoogleApiClient.ConnectionCallbacks
        public void onConnectionSuspended(int cause) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onConnectionSuspended: " + cause);

                Wearable.DataApi.removeListener(mGoogleApiClient, this);
                Wearable.MessageApi.removeListener(mGoogleApiClient, this);
                mGoogleApiClient.disconnect();
            }
        }

        @Override  // GoogleApiClient.OnConnectionFailedListener
        public void onConnectionFailed(ConnectionResult result) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onConnectionFailed: " + result);

                Wearable.DataApi.removeListener(mGoogleApiClient, this);
                Wearable.MessageApi.removeListener(mGoogleApiClient, this);
                mGoogleApiClient.disconnect();
            }
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

        private String formatTwoDigitNumber(int hour) {
            return String.format("%02d", hour);
        }

        @Override
        public void onMessageReceived(MessageEvent messageEvent) {
            if (messageEvent.getPath().equals(WatchFaceUtil.DIGILOGUE_COLOURS)) {
                DataMap config = DataMap.fromByteArray(messageEvent.getData());
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Message received:" + config);
                }
                updateUiForConfigDataMap(config);
            }

        }
    }
}

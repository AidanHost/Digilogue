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

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        static final int MSG_UPDATE_TIME = 0;
        static final String COLON_STRING = ":";

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;
        boolean mMute;
        boolean mRegisteredTimeZoneReceiver = false;
        boolean mShouldDrawColons;
        Time mTime;
        float mXOffset;
        float mYOffset;
        private int batteryLevel = 100;
        private float smallTextOffset;

        int mBackgroundColor = Color.parseColor("black");
        int mMiddleColor = Color.parseColor("gray");
        int mForegroundColor = Color.parseColor("white");
        int mAccentColor = Color.parseColor("red");

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
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                    - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
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

            mHourPaint = new Paint();
            mHourPaint.setColor(mForegroundColor);
            mHourPaint.setStrokeWidth(3f);
            mHourPaint.setAntiAlias(true);
            mHourPaint.setStrokeCap(Paint.Cap.ROUND);

            mMinutePaint = new Paint();
            mMinutePaint.setColor(mForegroundColor);
            mMinutePaint.setStrokeWidth(3f);
            mMinutePaint.setAntiAlias(true);
            mMinutePaint.setStrokeCap(Paint.Cap.ROUND);

            mSecondPaint = new Paint();
            mSecondPaint.setColor(mAccentColor);
            mSecondPaint.setStrokeWidth(2f);
            mSecondPaint.setAntiAlias(true);
            mSecondPaint.setStrokeCap(Paint.Cap.ROUND);

            mHourTickPaint = new Paint();
            mHourTickPaint.setColor(mForegroundColor);
            mHourTickPaint.setAlpha(100);
            mHourTickPaint.setStrokeWidth(2f);
            mHourTickPaint.setAntiAlias(true);

            mMinuteTickPaint = new Paint();
            mMinuteTickPaint.setColor(mForegroundColor);
            mMinuteTickPaint.setAlpha(100);
            mMinuteTickPaint.setStrokeWidth(1f);
            mMinuteTickPaint.setAntiAlias(true);

            mBatteryFullPaint = new Paint();
            mBatteryFullPaint.setColor(mMiddleColor);
            mBatteryFullPaint.setStrokeWidth(1f);

            mBatteryPaint = new Paint();
            mBatteryPaint.setColor(mForegroundColor);
            mBatteryPaint.setStrokeWidth(1f);

            mYOffset = resources.getDimension(R.dimen.digital_y_offset);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(mBackgroundColor);

            mDigitalHourPaint = createTextPaint(mForegroundColor, BOLD_TYPEFACE);
            mDigitalMinutePaint = createTextPaint(mForegroundColor);
            mElementPaint = createTextPaint(mForegroundColor);
            mColonPaint = createTextPaint(resources.getColor(R.color.digital_colons));

            mTime = new Time();
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
            invalidate();

            // Whether the timer should be running depends on whether we're in ambient mode (as well
            // as whether we're visible), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);
            if (mMute != inMuteMode) {
                mMute = inMuteMode;
                mHourPaint.setAlpha(inMuteMode ? 100 : 255);
                mMinutePaint.setAlpha(inMuteMode ? 100 : 255);
                mSecondPaint.setAlpha(inMuteMode ? 80 : 255);

                mDigitalHourPaint.setAlpha(inMuteMode ? 100 : 255);
                mDigitalMinutePaint.setAlpha(inMuteMode ? 100 : 255);

                mElementPaint.setAlpha(inMuteMode ? 100 : 255);
                mBatteryFullPaint.setAlpha(inMuteMode ? 100 : 255);
                mBatteryPaint.setAlpha(inMuteMode ? 100 : 255);

                invalidate();
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();

            // Show colons for the first half of each second so the colons blink on when the time
            // updates.
            mShouldDrawColons = (System.currentTimeMillis() % 1000) < 500;

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
            String hourString = String.format("%02d", mTime.hour);

            mDigitalHourPaint.setStyle(Paint.Style.STROKE);
            mDigitalHourPaint.setColor(mBackgroundColor);
            mDigitalHourPaint.setAlpha(255);
            canvas.drawText(hourString, x, centerY + mYOffset, mDigitalHourPaint);

            mDigitalHourPaint.setStyle(Paint.Style.FILL);
            mDigitalHourPaint.setColor(mForegroundColor);
            mDigitalHourPaint.setAlpha(mMute ? 100 : 255);
            canvas.drawText(hourString, x, centerY + mYOffset, mDigitalHourPaint);

            x += mDigitalHourPaint.measureText(hourString);

            // In ambient and mute modes, always draw the first colon. Otherwise, draw the
            // first colon for the first half of each second.
            if (isInAmbientMode() || mMute || mShouldDrawColons) {
                mColonPaint.setStyle(Paint.Style.STROKE);
                mColonPaint.setColor(mBackgroundColor);
                mColonPaint.setAlpha(255);
                canvas.drawText(COLON_STRING, x, centerY + mYOffset, mColonPaint);

                mColonPaint.setStyle(Paint.Style.FILL);
                mColonPaint.setColor(mMiddleColor);
                mColonPaint.setAlpha(mMute ? 100 : 255);
                canvas.drawText(COLON_STRING, x, centerY + mYOffset, mColonPaint);
            }
            x += mColonWidth;

            // Draw the minutes.
            String minuteString = formatTwoDigitNumber(mTime.minute);

            mDigitalMinutePaint.setStyle(Paint.Style.STROKE);
            mDigitalMinutePaint.setColor(mBackgroundColor);
            mDigitalMinutePaint.setAlpha(255);
            canvas.drawText(minuteString, x, centerY + mYOffset, mDigitalMinutePaint);

            mDigitalMinutePaint.setStyle(Paint.Style.FILL);
            mDigitalMinutePaint.setColor(mForegroundColor);
            mDigitalMinutePaint.setAlpha(mMute ? 100 : 255);
            canvas.drawText(minuteString, x, centerY + mYOffset, mDigitalMinutePaint);

            // Draw the Day, Date.
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, d");
            String dayString = sdf.format(new Date(mTime.toMillis(true)));

            mElementPaint.setStyle(Paint.Style.STROKE);
            mElementPaint.setColor(mBackgroundColor);
            mElementPaint.setAlpha(255);
            canvas.drawText(dayString, (centerX * 1.5f) - 5, centerY + smallTextOffset, mElementPaint);

            mElementPaint.setStyle(Paint.Style.FILL);
            mElementPaint.setColor(mForegroundColor);
            mElementPaint.setAlpha(mMute ? 100 : 255);
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

            mBatteryFullPaint.setColor(mBackgroundColor);
            mBatteryFullPaint.setAlpha(255);
            canvas.drawPath(batteryIcon, mBatteryFullPaint);

            mBatteryFullPaint.setColor(mMiddleColor);
            mBatteryFullPaint.setAlpha(mMute ? 100 : 255);
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
            mElementPaint.setColor(mBackgroundColor);
            mElementPaint.setAlpha(255);
            canvas.drawText(String.valueOf(batteryLevel), (centerX / 2f) - 20f, centerY + smallTextOffset, mElementPaint);

            mElementPaint.setStyle(Paint.Style.FILL);
            mElementPaint.setColor(mForegroundColor);
            mElementPaint.setAlpha(mMute ? 100 : 255);
            canvas.drawText(String.valueOf(batteryLevel), (centerX / 2f) - 20f, centerY + smallTextOffset, mElementPaint);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onVisibilityChanged: " + visible);
            }

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
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

        private String formatTwoDigitNumber(int hour) {
            return String.format("%02d", hour);
        }
    }
}

package com.greenman.digilogue.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.gms.wearable.DataMap;
import com.greenman.common.Utility;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

// TODO: extract drawing code to Utility and implement for watch face drawing
public class PreviewWatchFace extends View {
    //region variables
    private static final Typeface NORMAL_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
    private static final float HOUR_TICK_LENGTH = 10;
    private static final float HOUR_TICK_GAP = 3;
    private static final float MINUTE_TICK_LENGTH = 7;

    private static final String DEGREE_STRING = "°";

    private static final String COLON_STRING = ":";

    private boolean mIsInAmbientMode = false;
    private Resources r;

    private DataMap mConfig;

    private Time mTime;

    private float mXOffset;
    private float mYOffset;
    private float mSmallTextYOffset;
    private float mSmallTextXOffset;
    private float mColonWidth;

    private int mBatteryLevel = 100;
    private int mForegroundOpacityLevel;
    private int mAccentOpacityLevel;

    private int mChinHeight = 0;
    private boolean mGotChin = false;
    private String mAmString;
    private String mPmString;

    // Face
    private Paint mBackgroundPaint;
    private Paint mHourTickPaint;
    private Paint mMinuteTickPaint;
    private Paint mBatteryFullPaint;
    private Paint mBatteryPaint;
    private Paint mTextElementPaint;
    private Paint mWidgetWeatherPaint;
    private Paint mDialPaint;

    // Analogue
    private Paint mHourPaint;
    private Paint mMinutePaint;
    private Paint mSecondPaint;

    // Digital
    private Paint mDigitalHourPaint;
    private Paint mDigitalMinutePaint;
    private Paint mDigitalAmPmPaint;
    private Paint mColonPaint;

    // Paths
    private final Path batteryIcon = new Path();
    private final Path batteryIconLevel = new Path();
    private final Path moonPath = new Path();
    private final Path cloudPath = new Path();
    private final Path linePath = new Path();
    private final Path flakePath = new Path();
    private final Path lightningPath = new Path();

    // draw variables
    private int width;
    private int height;
    private float centerX;
    private float centerY;
    private float modifier;
    private float innerTickRadius;
    private float innerShortTickRadius;
    private float outerShortTickRadius;
    private float dialRadius;
    private float tickRot;
    private float dialX;
    private float dialY;
    private float innerX;
    private float innerY;
    private float outerX;
    private float outerY;
    private float difference;
    private float innerShortX;
    private float innerShortY;
    private float outerShortX;
    private float outerShortY;
    private float secRot;
    private float minRot;
    private float hrRot;
    private float secLength;
    private float minLength;
    private float hrLength;
    private float analogueHandOffset;
    private float x;
    private float secX;
    private float secY;
    private float secStartX;
    private float secStartY;
    private float minX;
    private float minY;
    private float minStartX;
    private float minStartY;
    private float hrX;
    private float hrY;
    private float hrStartX;
    private float hrStartY;
    private String hourString;
    private String backgroundColour;
    private String foregroundColour;
    private String middleBackgroundColour;
    private String middleForegroundColour;
    private String minuteString;
    private String dayString;
    private float batteryHeight;
    private float weatherIconCenterX;
    private float weatherIconCenterY;

    private final ArrayList<Integer> seconds = new ArrayList<>();
    private final SimpleDateFormat sdf = new SimpleDateFormat("EEE, d", Resources.getSystem().getConfiguration().locale);
    //endregion

    //region config data defaults
    boolean mToggleAmPm = Utility.CONFIG_DEFAULT_TOGGLE_AM_PM;
    boolean mToggleWeather = Utility.CONFIG_DEFAULT_TOGGLE_WEATHER;
    boolean mFahrenheit = Utility.CONFIG_DEFAULT_WIDGET_WEATHER_FAHRENHEIT;
    boolean mIsDayTime = Utility.CONFIG_DEFAULT_WIDGET_WEATHER_DAYTIME;
    boolean mToggleAnalogue = Utility.CONFIG_DEFAULT_TOGGLE_ANALOGUE;
    boolean mToggleDrawDial = Utility.CONFIG_DEFAULT_TOGGLE_DIAL;
    boolean mToggleDigital = Utility.CONFIG_DEFAULT_TOGGLE_DIGITAL;
    boolean mToggleBattery = Utility.CONFIG_DEFAULT_TOGGLE_BATTERY;
    boolean mToggleDayDate = Utility.CONFIG_DEFAULT_TOGGLE_DAY_DATE;
    boolean mToggleDimColour = Utility.CONFIG_DEFAULT_TOGGLE_DIM_COLOUR;
    boolean mToggleSolidText = Utility.CONFIG_DEFAULT_TOGGLE_SOLID_TEXT;
    boolean mFixChin = Utility.CONFIG_DEFAULT_TOGGLE_FIX_CHIN;

    int mTemperatureC = Utility.WIDGET_WEATHER_DATA_DEFAULT_TEMPERATURE_C;
    int mTemperatureF = Utility.WIDGET_WEATHER_DATA_DEFAULT_TEMPERATURE_F;
    int mCode = Utility.WIDGET_WEATHER_DATA_DEFAULT_CODE;

    String mBackgroundColour = Utility.COLOUR_NAME_DEFAULT_BACKGROUND;
    String mMiddleColour = Utility.COLOUR_NAME_DEFAULT_MIDDLE;
    String mForegroundColour = Utility.COLOUR_NAME_DEFAULT_FOREGROUND;
    String mAccentColour = Utility.COLOUR_NAME_DEFAULT_ACCENT;
    //endregion

    public PreviewWatchFace(Context context) {
        super(context);
        init();
    }

    public PreviewWatchFace(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setConfig(DataMap config) {
        mConfig = config;
        updateUI(mConfig);
    }

    private float getPixelsFromDp(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()) * (206f / 320f);
    }

    private void init() {
        r = getResources();

        mXOffset = getPixelsFromDp(45);
        mYOffset = getPixelsFromDp(12);
        float textSize = getPixelsFromDp(36);

        mSmallTextXOffset = getPixelsFromDp(3);
        mSmallTextYOffset = mYOffset / 2f;

        mAmString = "AM";
        mPmString = "PM";

        mHourPaint = new Paint();
        mHourPaint.setColor(Color.parseColor(mForegroundColour));
        mHourPaint.setStrokeWidth(getPixelsFromDp(3));
        mHourPaint.setAntiAlias(true);
        mHourPaint.setStrokeCap(Paint.Cap.ROUND);

        mMinutePaint = new Paint();
        mMinutePaint.setColor(Color.parseColor(mForegroundColour));
        mMinutePaint.setStrokeWidth(getPixelsFromDp(3));
        mMinutePaint.setAntiAlias(true);
        mMinutePaint.setStrokeCap(Paint.Cap.ROUND);

        mSecondPaint = new Paint();
        mSecondPaint.setColor(Color.parseColor(mAccentColour));
        mSecondPaint.setStrokeWidth(getPixelsFromDp(2));
        mSecondPaint.setAntiAlias(true);
        mSecondPaint.setStrokeCap(Paint.Cap.ROUND);

        mHourTickPaint = new Paint();
        mHourTickPaint.setColor(Color.parseColor(mForegroundColour));
        mHourTickPaint.setAlpha(100);
        mHourTickPaint.setStrokeWidth(getPixelsFromDp(2));
        mHourTickPaint.setAntiAlias(true);

        mMinuteTickPaint = new Paint();
        mMinuteTickPaint.setColor(Color.parseColor(mForegroundColour));
        mMinuteTickPaint.setAlpha(100);
        mMinuteTickPaint.setStrokeWidth(getPixelsFromDp(1));
        mMinuteTickPaint.setAntiAlias(true);

        mBatteryFullPaint = new Paint();
        mBatteryFullPaint.setColor(Color.parseColor(mMiddleColour));
        mBatteryFullPaint.setStrokeWidth(getPixelsFromDp(1));

        mWidgetWeatherPaint = new Paint();
        mWidgetWeatherPaint.setColor(Color.parseColor(mForegroundColour));
        mWidgetWeatherPaint.setStrokeWidth(getPixelsFromDp(2));

        mBatteryPaint = new Paint();
        mBatteryPaint.setColor(Color.parseColor(mForegroundColour));
        mBatteryPaint.setStrokeWidth(getPixelsFromDp(1));

        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(Color.parseColor(mBackgroundColour));

        mDigitalHourPaint = createTextPaint(Color.parseColor(mForegroundColour));
        mDigitalMinutePaint = createTextPaint(Color.parseColor(mForegroundColour));
        mDigitalAmPmPaint = createTextPaint(Color.parseColor(mForegroundColour));
        mTextElementPaint = createTextPaint(Color.parseColor(mForegroundColour));
        mColonPaint = createTextPaint(Color.parseColor(mMiddleColour));
        mDialPaint = createTextPaint(Color.parseColor(mMiddleColour));

        mDigitalHourPaint.setTextSize(textSize);
        mDigitalMinutePaint.setTextSize(textSize);
        mColonPaint.setTextSize(textSize);
        mTextElementPaint.setTextSize(textSize / 2f);
        mDialPaint.setTextSize(textSize / 2f);
        mDigitalAmPmPaint.setTextSize(textSize / 4f);

        mColonWidth = mColonPaint.measureText(COLON_STRING);

        mTime = new Time();

        mForegroundOpacityLevel = mIsInAmbientMode ? 125 : 255;
        mAccentOpacityLevel = mIsInAmbientMode ? 100 : 255;

        if (!mToggleDimColour) {
            mForegroundOpacityLevel = 255;
            mAccentOpacityLevel = 255;
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        // for new preview picture
        mTime.set(35, 10, 10, 5, 8, 2014);
        mBatteryLevel = 100;

        width = getWidth();
        height = getHeight();

        canvas.drawRect(0, 0, width, height, mBackgroundPaint);

        // Find the center. Ignore the window insets so that, on round watches with a
        // "chin", the watch face is centered on the entire screen, not just the usable
        // portion.
        centerX = width / 2f;
        centerY = height / 2f;
        float ratio = centerX / (centerX - (float)mChinHeight);
        modifier = ratio - ((float)mChinHeight / centerX);

        drawAnalogue(canvas);
        drawDigital(canvas);

        drawIndicators(canvas);

        /*Paint redLine = new Paint();
        redLine.setStrokeWidth(getPixelsFromDp(2));
        redLine.setColor(Color.parseColor("red"));

        canvas.drawLine(0, centerY, width, centerY, redLine);
        canvas.drawLine(centerX, 0, centerX, height, redLine);*/
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            mIsInAmbientMode = !mIsInAmbientMode;
            updateUI(mConfig);
        }

        return true;
    }

    private void updateUI(DataMap config) {
        mToggleAmPm = config.getBoolean(Utility.KEY_TOGGLE_AM_PM);
        mToggleDayDate = config.getBoolean(Utility.KEY_TOGGLE_DAY_DATE);
        mToggleDimColour = config.getBoolean(Utility.KEY_TOGGLE_DIM_COLOUR);
        mToggleSolidText = config.getBoolean(Utility.KEY_TOGGLE_SOLID_TEXT);
        mToggleDigital = config.getBoolean(Utility.KEY_TOGGLE_DIGITAL);
        mToggleAnalogue = config.getBoolean(Utility.KEY_TOGGLE_ANALOGUE);
        mToggleBattery = config.getBoolean(Utility.KEY_TOGGLE_BATTERY);
        mFixChin = config.getBoolean(Utility.KEY_TOGGLE_FIX_CHIN);
        mToggleDrawDial = config.getBoolean(Utility.KEY_TOGGLE_DRAW_DIAL);
        mToggleWeather = config.getBoolean(Utility.KEY_TOGGLE_WEATHER);

        mFahrenheit = config.getBoolean(Utility.KEY_WIDGET_WEATHER_FAHRENHEIT);

        mTemperatureC = config.getInt(Utility.KEY_WIDGET_WEATHER_DATA_TEMPERATURE_C);
        mTemperatureF = config.getInt(Utility.KEY_WIDGET_WEATHER_DATA_TEMPERATURE_F);
        mCode = config.getInt(Utility.KEY_WIDGET_WEATHER_DATA_CODE);
        mIsDayTime = config.getBoolean(Utility.KEY_WIDGET_WEATHER_DATA_ISDAYTIME);

        if (!mIsInAmbientMode) {
            setBackgroundColor(config.getString(Utility.KEY_BACKGROUND_COLOUR));
            setMiddleColor(config.getString(Utility.KEY_MIDDLE_COLOUR));
            setForegroundColor(config.getString(Utility.KEY_FOREGROUND_COLOUR));
            setAccentColor(config.getString(Utility.KEY_ACCENT_COLOUR));
        } else {
            setBackgroundColor(Utility.COLOUR_NAME_DEFAULT_BACKGROUND);
            setMiddleColor(Utility.COLOUR_NAME_DEFAULT_MIDDLE);
            setForegroundColor(Utility.COLOUR_NAME_DEFAULT_FOREGROUND);
            setAccentColor(Utility.COLOUR_NAME_DEFAULT_ACCENT);
        }

        // Dim all elements on screen
        mForegroundOpacityLevel = mIsInAmbientMode ? 125 : 255;
        mAccentOpacityLevel = mIsInAmbientMode ? 100 : 255;

        if (!mToggleDimColour) {
            mForegroundOpacityLevel = 255;
            mAccentOpacityLevel = 255;
        }

        invalidate();
    }

    //region draw methods
    private void drawAnalogue(Canvas canvas) {
        if (mToggleAnalogue) {
            seconds.clear();

            drawHourTicks(canvas);
            drawMinuteTicks(canvas);
            drawDialNumbers(canvas);

            analogueHandOffset = centerX / 4f;

            drawSecondHand(canvas);
            drawMinuteHand(canvas);
            drawHourHand(canvas);
        }
    }

    private void drawHourTicks(Canvas canvas) {
        innerTickRadius = centerX - getPixelsFromDp(HOUR_TICK_LENGTH);
        innerShortTickRadius = innerTickRadius - getPixelsFromDp(HOUR_TICK_GAP);
        outerShortTickRadius = innerShortTickRadius - getPixelsFromDp(HOUR_TICK_LENGTH);

        for (int tickIndex = 0; tickIndex < 12; tickIndex++) {
            tickRot = (float) (tickIndex * Math.PI * 2f / 12f);
            innerX = (float) Math.sin(tickRot) * innerTickRadius;
            innerY = (float) -Math.cos(tickRot) * innerTickRadius;
            outerX = (float) Math.sin(tickRot) * centerX;
            outerY = (float) -Math.cos(tickRot) * centerX;

            if (mFixChin) {
                difference = centerY + outerY - (height - mChinHeight);

                if (difference > 0) {
                    innerX = (float) Math.sin(tickRot) * (innerTickRadius * modifier);
                    innerY = (float) -Math.cos(tickRot) * innerTickRadius - difference;
                    outerX = (float) Math.sin(tickRot) * (centerX * modifier);
                    outerY = (float) -Math.cos(tickRot) * centerX - difference;
                }
            }

            if (!mIsInAmbientMode)
                canvas.drawLine(centerX + innerX, centerY + innerY, centerX + outerX, centerY + outerY, mHourTickPaint);

            innerShortX = (float) Math.sin(tickRot) * innerShortTickRadius;
            innerShortY = (float) -Math.cos(tickRot) * innerShortTickRadius;
            outerShortX = (float) Math.sin(tickRot) * outerShortTickRadius;
            outerShortY = (float) -Math.cos(tickRot) * outerShortTickRadius;

            if (mFixChin) {
                if (mGotChin && centerY + (-Math.cos(tickRot) * centerX) > height - mChinHeight) {
                    innerShortX = (float) Math.sin(tickRot) * (innerShortTickRadius * modifier);
                    innerShortY = (float) -Math.cos(tickRot) * innerShortTickRadius - difference;
                    outerShortX = (float) Math.sin(tickRot) * (outerShortTickRadius * modifier);
                    outerShortY = (float) -Math.cos(tickRot) * outerShortTickRadius - difference;
                }
            }

            canvas.drawLine(centerX + innerShortX, centerY + innerShortY, centerX + outerShortX, centerY + outerShortY, mHourTickPaint);
        }
    }

    private void drawMinuteTicks(Canvas canvas) {
        if (!mIsInAmbientMode) {
            float innerMinuteTickRadius = centerX - getPixelsFromDp(MINUTE_TICK_LENGTH);
            for (int tickIndex = 0; tickIndex < 60; tickIndex++) {
                tickRot = (float) (tickIndex * Math.PI * 2f / 60f);
                innerX = (float) Math.sin(tickRot) * innerMinuteTickRadius;
                innerY = (float) -Math.cos(tickRot) * innerMinuteTickRadius;
                outerX = (float) Math.sin(tickRot) * centerX;
                outerY = (float) -Math.cos(tickRot) * centerX;

                if (mFixChin) {
                    difference = centerY + outerY - (height - mChinHeight);
                    if (difference > 0) {
                        innerX = (float) Math.sin(tickRot) * (innerMinuteTickRadius * modifier);
                        innerY = (float) -Math.cos(tickRot) * innerMinuteTickRadius - difference;
                        outerX = (float) Math.sin(tickRot) * (centerX * modifier);
                        outerY = (float) -Math.cos(tickRot) * centerX - difference;

                        seconds.add(tickIndex);
                    }
                }

                canvas.drawLine(centerX + innerX, centerY + innerY, centerX + outerX, centerY + outerY, mMinuteTickPaint);
            }
        }
    }

    private void drawDialNumbers(Canvas canvas) {
        if (mToggleDrawDial) {
            dialRadius = innerTickRadius - (mYOffset * 2f) - getPixelsFromDp(5);
            for (int tickIndex = 0; tickIndex < 12; tickIndex++) {
                if (tickIndex == 3 && mToggleDayDate)
                    continue;

                if (tickIndex == 9 && mToggleBattery)
                    continue;

                tickRot = (float) (tickIndex * Math.PI * 2f / 12f);
                dialX = (float) Math.sin(tickRot) * dialRadius;
                dialY = (float) -Math.cos(tickRot) * dialRadius;

                if (mFixChin) {
                    difference = centerY + ((float) -Math.cos(tickRot) * centerX) - (height - mChinHeight);

                    if (difference > 0) {
                        dialX = (float) Math.sin(tickRot) * (dialRadius * modifier);
                        dialY = (float) -Math.cos(tickRot) * dialRadius - difference;
                    }
                }

                dialX -= mSmallTextXOffset;

                if (tickIndex == 0 || tickIndex == 10 || tickIndex == 11) {
                    dialX -= mSmallTextXOffset;
                }

                dialY += mSmallTextYOffset;

                mTextElementPaint.setStyle(Paint.Style.STROKE);
                mTextElementPaint.setColor(Color.parseColor(mBackgroundColour));
                mTextElementPaint.setAlpha(255);
                canvas.drawText(tickIndex == 0 ? "12" : tickIndex + "", centerX + dialX, centerY + dialY, mDialPaint);

                mTextElementPaint.setStyle(Paint.Style.FILL);
                mTextElementPaint.setColor(Color.parseColor(mForegroundColour));
                mTextElementPaint.setAlpha(mForegroundOpacityLevel);
                canvas.drawText(tickIndex == 0 ? "12" : tickIndex + "", centerX + dialX, centerY + dialY, mDialPaint);
            }
        }
    }

    private void drawSecondHand(Canvas canvas) {
        if (!mIsInAmbientMode) {
            secLength = centerX - getPixelsFromDp(20);
            secRot = mTime.second / 30f * (float) Math.PI;

            secX = (float) Math.sin(secRot) * secLength;
            secY = (float) -Math.cos(secRot) * secLength;
            secStartX = (float) Math.sin(secRot) * analogueHandOffset;
            secStartY = (float) -Math.cos(secRot) * analogueHandOffset;

            if (mFixChin) {
                difference = centerY + secY - (height - mChinHeight);

                if (mGotChin && difference > 0 || seconds.contains(mTime.second)) {
                    secX = (float) Math.sin(secRot) * (secLength * modifier);
                    secY = (float) -Math.cos(secRot) * secLength - difference - getPixelsFromDp(18);
                }
            }

            mSecondPaint.setStyle(Paint.Style.STROKE);
            mSecondPaint.setColor(Color.parseColor(mBackgroundColour));
            mSecondPaint.setAlpha(255);
            canvas.drawLine(centerX + secStartX, centerY + secStartY, centerX + secX, centerY + secY, mSecondPaint);

            mSecondPaint.setStyle(Paint.Style.FILL);
            mSecondPaint.setColor(Color.parseColor(mAccentColour));
            mSecondPaint.setAlpha(mForegroundOpacityLevel);
            canvas.drawLine(centerX + secStartX, centerY + secStartY, centerX + secX, centerY + secY, mSecondPaint);
        }
    }

    private void drawMinuteHand(Canvas canvas) {
        minLength = centerX - getPixelsFromDp(35);
        minRot = mTime.minute / 30f * (float) Math.PI;

        minX = (float) Math.sin(minRot) * minLength;
        minY = (float) -Math.cos(minRot) * minLength;
        minStartX = (float) Math.sin(minRot) * analogueHandOffset;
        minStartY = (float) -Math.cos(minRot) * analogueHandOffset;

        if (mFixChin) {
            difference = centerY + ((float) -Math.cos(minRot) * secLength) - (height - mChinHeight);

            if (mGotChin && seconds.contains(mTime.minute)) {
                minX = (float) Math.sin(minRot) * (secLength * modifier);
                minY = (float) -Math.cos(minRot) * secLength - difference - getPixelsFromDp(18);
            }
        }

        mMinutePaint.setStyle(Paint.Style.STROKE);
        mMinutePaint.setColor(Color.parseColor(mBackgroundColour));
        mMinutePaint.setAlpha(255);
        canvas.drawLine(centerX + minStartX, centerY + minStartY, centerX + minX, centerY + minY, mMinutePaint);

        mMinutePaint.setStyle(Paint.Style.FILL);
        mMinutePaint.setColor(Color.parseColor(mForegroundColour));
        mMinutePaint.setAlpha(mForegroundOpacityLevel);
        canvas.drawLine(centerX + minStartX, centerY + minStartY, centerX + minX, centerY + minY, mMinutePaint);
    }

    private void drawHourHand(Canvas canvas) {
        hrLength = centerX - getPixelsFromDp(75);
        hrRot = ((mTime.hour + (mTime.minute / 60f)) / 6f) * (float) Math.PI;

        hrX = (float) Math.sin(hrRot) * hrLength;
        hrY = (float) -Math.cos(hrRot) * hrLength;
        hrStartX = (float) Math.sin(hrRot) * analogueHandOffset;
        hrStartY = (float) -Math.cos(hrRot) * analogueHandOffset;

        mHourPaint.setStyle(Paint.Style.STROKE);
        mHourPaint.setColor(Color.parseColor(mBackgroundColour));
        mHourPaint.setAlpha(255);
        canvas.drawLine(centerX + hrStartX, centerY + hrStartY, centerX + hrX, centerY + hrY, mHourPaint);

        mHourPaint.setStyle(Paint.Style.FILL);
        mHourPaint.setColor(Color.parseColor(mForegroundColour));
        mHourPaint.setAlpha(mForegroundOpacityLevel);
        canvas.drawLine(centerX + hrStartX, centerY + hrStartY, centerX + hrX, centerY + hrY, mHourPaint);
    }

    private void drawDigital(Canvas canvas) {
        if (mToggleDigital) {
            // Digital
            x = centerX - mXOffset;

            // Draw the hours.
            drawHourText(canvas);

            drawColon(canvas);

            // Draw the minutes.
            drawMinuteText(canvas);

            // Draw AM/PM indicator
            drawAmPm(canvas);
        }
    }

    private void drawHourText(Canvas canvas) {
        hourString = formatTwoDigitHourNumber(mTime.hour);
        backgroundColour = mToggleSolidText ? mBackgroundColour : mIsInAmbientMode ? mForegroundColour : mBackgroundColour;

        mDigitalHourPaint.setStyle(Paint.Style.STROKE);
        mDigitalHourPaint.setColor(Color.parseColor(backgroundColour));
        mDigitalHourPaint.setAlpha(mIsInAmbientMode ? mForegroundOpacityLevel : 255);
        canvas.drawText(hourString, x, centerY + mYOffset, mDigitalHourPaint);

        foregroundColour = mToggleSolidText ? mForegroundColour : mIsInAmbientMode ? mBackgroundColour : mForegroundColour;

        mDigitalHourPaint.setStyle(Paint.Style.FILL);
        mDigitalHourPaint.setColor(Color.parseColor(foregroundColour));
        mDigitalHourPaint.setAlpha(mForegroundOpacityLevel);
        canvas.drawText(hourString, x, centerY + mYOffset, mDigitalHourPaint);

        x += mDigitalHourPaint.measureText(hourString);
    }

    private void drawColon(Canvas canvas) {
        middleBackgroundColour = mToggleSolidText ? mBackgroundColour : mIsInAmbientMode ? mMiddleColour : mBackgroundColour;

        mColonPaint.setStyle(Paint.Style.STROKE);
        mColonPaint.setColor(Color.parseColor(middleBackgroundColour));
        mColonPaint.setAlpha(mIsInAmbientMode ? mForegroundOpacityLevel : 255);
        canvas.drawText(COLON_STRING, x, centerY + mYOffset, mColonPaint);

        middleForegroundColour = mToggleSolidText ? mMiddleColour : mIsInAmbientMode ? mBackgroundColour : mMiddleColour;

        mColonPaint.setStyle(Paint.Style.FILL);
        mColonPaint.setColor(Color.parseColor(middleForegroundColour));
        mColonPaint.setAlpha(mForegroundOpacityLevel);
        canvas.drawText(COLON_STRING, x, centerY + mYOffset, mColonPaint);

        x += mColonWidth;
    }

    private void drawMinuteText(Canvas canvas) {
        minuteString = formatTwoDigitNumber(mTime.minute);

        mDigitalMinutePaint.setStyle(Paint.Style.STROKE);
        mDigitalMinutePaint.setColor(Color.parseColor(backgroundColour));
        mDigitalMinutePaint.setAlpha(mIsInAmbientMode ? mForegroundOpacityLevel : 255);
        canvas.drawText(minuteString, x, centerY + mYOffset, mDigitalMinutePaint);

        mDigitalMinutePaint.setStyle(Paint.Style.FILL);
        mDigitalMinutePaint.setColor(Color.parseColor(foregroundColour));
        mDigitalMinutePaint.setAlpha(mForegroundOpacityLevel);
        canvas.drawText(minuteString, x, centerY + mYOffset, mDigitalMinutePaint);
    }

    private void drawAmPm(Canvas canvas) {
        if (mToggleAmPm) {
            x += mDigitalMinutePaint.measureText(minuteString);

            mDigitalAmPmPaint.setStyle(Paint.Style.STROKE);
            mDigitalAmPmPaint.setColor(Color.parseColor(mBackgroundColour));
            mDigitalAmPmPaint.setAlpha(255);
            canvas.drawText(getAmPmString(mTime.hour), x, centerY + mYOffset, mDigitalAmPmPaint);

            mDigitalAmPmPaint.setStyle(Paint.Style.FILL);
            mDigitalAmPmPaint.setColor(Color.parseColor(mForegroundColour));
            mDigitalAmPmPaint.setAlpha(mForegroundOpacityLevel);
            canvas.drawText(getAmPmString(mTime.hour), x, centerY + mYOffset, mDigitalAmPmPaint);
        }
    }

    private void drawIndicators(Canvas canvas) {
        drawDayDate(canvas);
        drawBattery(canvas);
        drawWeather(canvas);
    }

    private void drawDayDate(Canvas canvas) {
        if (mToggleDayDate) {
            // Draw the Day, Date.
            dayString = sdf.format(new Date(mTime.toMillis(true)));

            mTextElementPaint.setStyle(Paint.Style.STROKE);
            mTextElementPaint.setColor(Color.parseColor(mBackgroundColour));
            mTextElementPaint.setAlpha(255);
            canvas.drawText(dayString, (centerX * 1.5f) - getPixelsFromDp(10), centerY + mSmallTextYOffset, mTextElementPaint);

            mTextElementPaint.setStyle(Paint.Style.FILL);
            mTextElementPaint.setColor(Color.parseColor(mForegroundColour));
            mTextElementPaint.setAlpha(mForegroundOpacityLevel);
            canvas.drawText(dayString, (centerX * 1.5f) - getPixelsFromDp(10), centerY + mSmallTextYOffset, mTextElementPaint);
        }
    }

    private void drawBattery(Canvas canvas) {
        if (mToggleBattery) {
            // Draw Battery icon
            batteryIcon.reset();
            batteryIcon.moveTo((centerX / 2f) - getPixelsFromDp(35), centerY + mSmallTextYOffset);
            batteryIcon.rLineTo(0, getPixelsFromDp(-13));
            batteryIcon.rLineTo(getPixelsFromDp(2), 0);
            batteryIcon.rLineTo(0, getPixelsFromDp(-2));
            batteryIcon.rLineTo(getPixelsFromDp(5), 0);
            batteryIcon.rLineTo(0, getPixelsFromDp(2));
            batteryIcon.rLineTo(getPixelsFromDp(2), 0);
            batteryIcon.rLineTo(0, getPixelsFromDp(13));
            batteryIcon.close();

            mBatteryFullPaint.setStyle(Paint.Style.STROKE);
            mBatteryFullPaint.setColor(Color.parseColor(mBackgroundColour));
            mBatteryFullPaint.setAlpha(255);
            canvas.drawPath(batteryIcon, mBatteryFullPaint);

            mBatteryFullPaint.setStyle(Paint.Style.FILL);
            mBatteryFullPaint.setColor(Color.parseColor(mMiddleColour));
            mBatteryFullPaint.setAlpha(mForegroundOpacityLevel);
            canvas.drawPath(batteryIcon, mBatteryFullPaint);

            batteryHeight = (float) Math.ceil(15f * mBatteryLevel / 100f);

            batteryIconLevel.reset();
            batteryIconLevel.moveTo((centerX / 2f) - getPixelsFromDp(35), centerY + mSmallTextYOffset);

            if (batteryHeight >= 13f) {
                batteryIconLevel.rLineTo(0, getPixelsFromDp(-13));
                batteryIconLevel.rLineTo(getPixelsFromDp(2), 0);
                batteryIconLevel.rLineTo(0, getPixelsFromDp(-(batteryHeight - 13)));
                batteryIconLevel.rLineTo(getPixelsFromDp(5), 0);
                batteryIconLevel.rLineTo(0, getPixelsFromDp((batteryHeight - 13)));
                batteryIconLevel.rLineTo(getPixelsFromDp(2), 0);
                batteryIconLevel.rLineTo(0, getPixelsFromDp(13));
            } else {
                batteryIconLevel.rLineTo(0, getPixelsFromDp(-batteryHeight));
                batteryIconLevel.rLineTo(getPixelsFromDp(9), 0);
                batteryIconLevel.rLineTo(0, getPixelsFromDp(batteryHeight));
            }

            batteryIconLevel.close();

            mBatteryPaint.setStyle(Paint.Style.FILL);
            mBatteryPaint.setColor(Color.parseColor(mBackgroundColour));
            mBatteryPaint.setAlpha(255);
            canvas.drawPath(batteryIconLevel, mBatteryPaint);

            mBatteryPaint.setStyle(Paint.Style.FILL);
            mBatteryPaint.setColor(Color.parseColor(mForegroundColour));
            mBatteryPaint.setAlpha(mForegroundOpacityLevel);
            canvas.drawPath(batteryIconLevel, mBatteryPaint);

            // Battery level
            mTextElementPaint.setStyle(Paint.Style.STROKE);
            mTextElementPaint.setColor(Color.parseColor(mBackgroundColour));
            mTextElementPaint.setAlpha(255);
            canvas.drawText(String.valueOf(mBatteryLevel), (centerX / 2f) - getPixelsFromDp(20), centerY + mSmallTextYOffset, mTextElementPaint);

            mTextElementPaint.setStyle(Paint.Style.FILL);
            mTextElementPaint.setColor(Color.parseColor(mForegroundColour));
            mTextElementPaint.setAlpha(mForegroundOpacityLevel);
            canvas.drawText(String.valueOf(mBatteryLevel), (centerX / 2f) - getPixelsFromDp(20), centerY + mSmallTextYOffset, mTextElementPaint);
        }
    }

    private void drawWeather(Canvas canvas) {
        if (mToggleWeather) {
            weatherIconCenterX = centerX - getPixelsFromDp(15);
            weatherIconCenterY = (centerY * 0.6f) - getPixelsFromDp(6);

            if (mTemperatureC != -999 && mTemperatureF != -999 && mCode != Utility.WeatherCodes.UNKNOWN) {
                // Draw temperature
                mTextElementPaint.setStyle(Paint.Style.STROKE);
                mTextElementPaint.setColor(Color.parseColor(mBackgroundColour));
                mTextElementPaint.setAlpha(255);
                canvas.drawText(String.valueOf(mFahrenheit ? mTemperatureF : mTemperatureC) + DEGREE_STRING, centerX + getPixelsFromDp(3), centerY * 0.6f, mTextElementPaint);

                mTextElementPaint.setStyle(Paint.Style.FILL);
                mTextElementPaint.setColor(Color.parseColor(mForegroundColour));
                mTextElementPaint.setAlpha(mForegroundOpacityLevel);
                canvas.drawText(String.valueOf(mFahrenheit ? mTemperatureF : mTemperatureC) + DEGREE_STRING, centerX + getPixelsFromDp(3), centerY * 0.6f, mTextElementPaint);

                // Draw icon based on conditions
                switch (mCode) {
                    case Utility.WeatherCodes.CLEAR: // sun or moon
                        if (mIsDayTime)
                            drawSun(canvas, weatherIconCenterX, weatherIconCenterY);
                        else
                            drawMoon(canvas, weatherIconCenterX, weatherIconCenterY);
                        break;
                    case Utility.WeatherCodes.PARTLY_CLOUDY:
                        if (mIsDayTime)
                            drawSun(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(2));
                        else
                            drawMoon(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(2));

                        drawCloud(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(2));
                        break;
                    case Utility.WeatherCodes.CLOUDY:
                    case Utility.WeatherCodes.OVERCAST:
                        drawCloud(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(6));
                        break;
                    case Utility.WeatherCodes.MIST:
                    case Utility.WeatherCodes.FOG:
                    case Utility.WeatherCodes.FREEZING_FOG:
                        drawFog(canvas, weatherIconCenterX, weatherIconCenterY);
                        break;
                    case Utility.WeatherCodes.PATCHY_RAIN_NEARBY:
                    case Utility.WeatherCodes.PATCHY_LIGHT_DRIZZLE:
                    case Utility.WeatherCodes.LIGHT_RAIN_SHOWER:
                        if (mIsDayTime)
                            drawSun(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(6));
                        else
                            drawMoon(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(6));

                        drawCloud(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(6));
                        drawRainLine(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(6));
                        break;
                    case Utility.WeatherCodes.PATCHY_SNOW_NEARBY:
                    case Utility.WeatherCodes.LIGHT_SLEET_SHOWERS:
                    case Utility.WeatherCodes.MODERATE_OR_HEAVY_SLEET_SHOWERS:
                    case Utility.WeatherCodes.LIGHT_SHOWERS_OF_ICE_PELLETS:
                        if (mIsDayTime)
                            drawSun(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(6));
                        else
                            drawMoon(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(6));

                        drawCloud(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(6));
                        drawRainLine(canvas, weatherIconCenterX - getPixelsFromDp(4), weatherIconCenterY - getPixelsFromDp(6));
                        drawSnowFlake(canvas, weatherIconCenterX + getPixelsFromDp(4), weatherIconCenterY - getPixelsFromDp(6));
                        break;
                    case Utility.WeatherCodes.PATCHY_SLEET_NEARBY:
                    case Utility.WeatherCodes.PATCHY_FREEZING_DRIZZLE_NEARBY:
                    case Utility.WeatherCodes.FREEZING_DRIZZLE:
                    case Utility.WeatherCodes.HEAVY_FREEZING_DRIZZLE:
                    case Utility.WeatherCodes.LIGHT_FREEZING_RAIN:
                    case Utility.WeatherCodes.MODERATE_OR_HEAVY_FREEZING_RAIN:
                    case Utility.WeatherCodes.LIGHT_SLEET:
                    case Utility.WeatherCodes.ICE_PELLETS:
                    case Utility.WeatherCodes.MODERATE_OR_HEAVY_SHOWERS_OF_ICE_PELLETS:
                        drawCloud(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(8));
                        drawRainLine(canvas, weatherIconCenterX - getPixelsFromDp(4), weatherIconCenterY - getPixelsFromDp(8));
                        drawSnowFlake(canvas, weatherIconCenterX + getPixelsFromDp(4), weatherIconCenterY - getPixelsFromDp(8));
                        break;
                    case Utility.WeatherCodes.THUNDERY_OUTBREAKS:
                    case Utility.WeatherCodes.PATCHY_LIGHT_RAIN_IN_AREA_WITH_THUNDER:
                    case Utility.WeatherCodes.PATCHY_LIGHT_SNOW_IN_AREA_WITH_THUNDER:
                        if (mIsDayTime)
                            drawSun(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(6));
                        else
                            drawMoon(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(6));

                        drawCloud(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(6));
                        drawLightning(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(6));

                        break;
                    case Utility.WeatherCodes.BLOWING_SNOW:
                    case Utility.WeatherCodes.MODERATE_OR_HEAVY_SLEET:
                        drawCloud(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(8));
                        drawSnowFlake(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(8));
                        break;
                    case Utility.WeatherCodes.BLIZZARD:
                    case Utility.WeatherCodes.PATCHY_MODERATE_SNOW:
                    case Utility.WeatherCodes.MODERATE_SNOW:
                    case Utility.WeatherCodes.HEAVY_SNOW:
                        drawCloud(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(8));
                        drawSnowFlake(canvas, weatherIconCenterX - getPixelsFromDp(4), weatherIconCenterY - getPixelsFromDp(8));
                        drawSnowFlake(canvas, weatherIconCenterX + getPixelsFromDp(4), weatherIconCenterY - getPixelsFromDp(8));
                        break;
                    case Utility.WeatherCodes.LIGHT_DRIZZLE:
                    case Utility.WeatherCodes.PATCHY_LIGHT_RAIN:
                    case Utility.WeatherCodes.LIGHT_RAIN:
                        drawCloud(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(8));
                        drawRainLine(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(8));
                        break;
                    case Utility.WeatherCodes.MODERATE_RAIN_AT_TIMES:
                    case Utility.WeatherCodes.HEAVY_RAIN_AT_TIMES:
                    case Utility.WeatherCodes.MODERATE_OR_HEAVY_RAIN_SHOWER:
                        if (mIsDayTime)
                            drawSun(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(6));
                        else
                            drawMoon(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(6));

                        drawCloud(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(6));
                        drawRainLine(canvas, weatherIconCenterX - getPixelsFromDp(4), weatherIconCenterY - getPixelsFromDp(6));
                        drawRainLine(canvas, weatherIconCenterX + getPixelsFromDp(4), weatherIconCenterY - getPixelsFromDp(6));
                        break;
                    case Utility.WeatherCodes.MODERATE_RAIN:
                    case Utility.WeatherCodes.HEAVY_RAIN:
                    case Utility.WeatherCodes.TORRENTIAL_RAIN_SHOWER:
                        drawCloud(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(8));
                        drawRainLine(canvas, weatherIconCenterX - getPixelsFromDp(4), weatherIconCenterY - getPixelsFromDp(8));
                        drawRainLine(canvas, weatherIconCenterX + getPixelsFromDp(4), weatherIconCenterY - getPixelsFromDp(8));
                        break;
                    case Utility.WeatherCodes.PATCHY_LIGHT_SNOW:
                    case Utility.WeatherCodes.LIGHT_SNOW:
                    case Utility.WeatherCodes.LIGHT_SNOW_SHOWERS:
                        if (mIsDayTime)
                            drawSun(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(6));
                        else
                            drawMoon(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(6));

                        drawCloud(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(6));
                        drawSnowFlake(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(6));
                        break;
                    case Utility.WeatherCodes.PATCHY_HEAVY_SNOW:
                    case Utility.WeatherCodes.MODERATE_OR_HEAVY_SNOW_SHOWERS:
                    case Utility.WeatherCodes.MODERATE_OR_HEAVY_SNOW_IN_AREA_WITH_THUNDER:
                        if (mIsDayTime)
                            drawSun(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(6));
                        else
                            drawMoon(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(6));

                        drawCloud(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(6));
                        drawSnowFlake(canvas, weatherIconCenterX - getPixelsFromDp(4), weatherIconCenterY - getPixelsFromDp(6));
                        drawSnowFlake(canvas, weatherIconCenterX + getPixelsFromDp(4), weatherIconCenterY - getPixelsFromDp(6));
                        break;
                    case Utility.WeatherCodes.MODERATE_OR_HEAVY_RAIN_IN_AREA_WITH_THUNDER:
                        drawCloud(canvas, weatherIconCenterX, weatherIconCenterY - getPixelsFromDp(8));
                        drawRainLine(canvas, weatherIconCenterX - getPixelsFromDp(4), weatherIconCenterY - getPixelsFromDp(8));
                        drawLightning(canvas, weatherIconCenterX + getPixelsFromDp(5), weatherIconCenterY - getPixelsFromDp(8));
                        break;

                    default: // line
                        mWidgetWeatherPaint.setColor(Color.parseColor(mBackgroundColour));
                        mWidgetWeatherPaint.setAlpha(255);
                        mWidgetWeatherPaint.setStyle(Paint.Style.STROKE);
                        canvas.drawLine(weatherIconCenterX, weatherIconCenterY, weatherIconCenterX + getPixelsFromDp(10), weatherIconCenterY, mWidgetWeatherPaint);

                        mWidgetWeatherPaint.setColor(Color.parseColor(mForegroundColour));
                        mWidgetWeatherPaint.setAlpha(mForegroundOpacityLevel);
                        mWidgetWeatherPaint.setStyle(Paint.Style.FILL);
                        canvas.drawLine(weatherIconCenterX, weatherIconCenterY, weatherIconCenterX + getPixelsFromDp(10), weatherIconCenterY, mWidgetWeatherPaint);
                        break;
                }
            } else {
                // No weather data to display
                mWidgetWeatherPaint.setColor(Color.parseColor(mBackgroundColour));
                mWidgetWeatherPaint.setAlpha(255);
                mWidgetWeatherPaint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(weatherIconCenterX, weatherIconCenterY, weatherIconCenterX + getPixelsFromDp(10), weatherIconCenterY, mWidgetWeatherPaint);

                mWidgetWeatherPaint.setColor(Color.parseColor(mForegroundColour));
                mWidgetWeatherPaint.setAlpha(mForegroundOpacityLevel);
                mWidgetWeatherPaint.setStyle(Paint.Style.FILL);
                canvas.drawLine(weatherIconCenterX, weatherIconCenterY, weatherIconCenterX + getPixelsFromDp(10), weatherIconCenterY, mWidgetWeatherPaint);
            }
        }
    }
    //endregion

    //region Drawing icon methods
    private void drawSun(Canvas canvas, float x, float y) {
        for (int beam = 0; beam < 8; beam++) {
            float beamRot = (float) (beam * Math.PI * 2f / 8f);
            float innerX = (float) Math.sin(beamRot) * getPixelsFromDp(8);
            float innerY = (float) -Math.cos(beamRot) * getPixelsFromDp(8);
            float outerX = (float) Math.sin(beamRot) * getPixelsFromDp(12);
            float outerY = (float) -Math.cos(beamRot) * getPixelsFromDp(12);

            mWidgetWeatherPaint.setColor(Color.parseColor(mBackgroundColour));
            mWidgetWeatherPaint.setAlpha(255);
            mWidgetWeatherPaint.setStyle(Paint.Style.STROKE);
            canvas.drawLine(x + innerX, y + innerY, x + outerX, y + outerY, mWidgetWeatherPaint);

            mWidgetWeatherPaint.setColor(Color.parseColor(mForegroundColour));
            mWidgetWeatherPaint.setAlpha(mForegroundOpacityLevel);
            mWidgetWeatherPaint.setStyle(Paint.Style.FILL);
            canvas.drawLine(x + innerX, y + innerY, x + outerX, y + outerY, mWidgetWeatherPaint);
        }

        mWidgetWeatherPaint.setColor(Color.parseColor(mForegroundColour));
        mWidgetWeatherPaint.setAlpha(mForegroundOpacityLevel);
        mWidgetWeatherPaint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(x, y, getPixelsFromDp(6), mWidgetWeatherPaint);

        mWidgetWeatherPaint.setColor(Color.parseColor(mBackgroundColour));
        mWidgetWeatherPaint.setAlpha(255);
        mWidgetWeatherPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(x, y, getPixelsFromDp(6), mWidgetWeatherPaint);
    }

    private void drawMoon(Canvas canvas, float x, float y) {
        moonPath.reset();
        moonPath.moveTo(x, y - getPixelsFromDp(8));
        moonPath.arcTo(new RectF(x - getPixelsFromDp(8), y - getPixelsFromDp(8), x + getPixelsFromDp(8), y + getPixelsFromDp(8)), 270f, -270f, false);
        moonPath.arcTo(new RectF(x - getPixelsFromDp(4), y - getPixelsFromDp(8), x + getPixelsFromDp(8), y + getPixelsFromDp(4)), 0f, 270f, false);

        mWidgetWeatherPaint.setColor(Color.parseColor(mForegroundColour));
        mWidgetWeatherPaint.setAlpha(mForegroundOpacityLevel);
        mWidgetWeatherPaint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(moonPath, mWidgetWeatherPaint);

        mWidgetWeatherPaint.setColor(Color.parseColor(mBackgroundColour));
        mWidgetWeatherPaint.setAlpha(255);
        mWidgetWeatherPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(moonPath, mWidgetWeatherPaint);
    }

    private void drawCloud(Canvas canvas, float x, float y) {
        cloudPath.reset();
        cloudPath.moveTo(x - getPixelsFromDp(8), y + getPixelsFromDp(16));
        cloudPath.arcTo(new RectF(x, y + getPixelsFromDp(6), x + getPixelsFromDp(10), y + getPixelsFromDp(16)), 90f, -250f, false);
        cloudPath.arcTo(new RectF(x - getPixelsFromDp(8), y, x + getPixelsFromDp(4), y + getPixelsFromDp(9)), 0f, -210f, false);
        cloudPath.arcTo(new RectF(x - getPixelsFromDp(14), y + getPixelsFromDp(8), x - getPixelsFromDp(6), y + getPixelsFromDp(16)), 340f, -230f, false);
        cloudPath.close();

        mWidgetWeatherPaint.setColor(Color.parseColor(mForegroundColour));
        mWidgetWeatherPaint.setAlpha(mForegroundOpacityLevel);
        mWidgetWeatherPaint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(cloudPath, mWidgetWeatherPaint);

        mWidgetWeatherPaint.setColor(Color.parseColor(mBackgroundColour));
        mWidgetWeatherPaint.setAlpha(255);
        mWidgetWeatherPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(cloudPath, mWidgetWeatherPaint);
    }

    private void drawRainLine(Canvas canvas, float x, float y) {
        linePath.reset();
        linePath.moveTo(x - getPixelsFromDp(4), y + getPixelsFromDp(14));
        linePath.rLineTo(getPixelsFromDp(-1), getPixelsFromDp(5));
        linePath.rMoveTo(getPixelsFromDp(0), getPixelsFromDp(2));
        linePath.rLineTo(getPixelsFromDp(-1), getPixelsFromDp(5));
        linePath.rMoveTo(getPixelsFromDp(6), getPixelsFromDp(-12));
        linePath.rLineTo(getPixelsFromDp(-1), getPixelsFromDp(5));
        linePath.rMoveTo(getPixelsFromDp(0), getPixelsFromDp(2));
        linePath.rLineTo(getPixelsFromDp(-1), getPixelsFromDp(5));
        linePath.close();

        mWidgetWeatherPaint.setColor(Color.parseColor(mForegroundColour));
        mWidgetWeatherPaint.setAlpha(mForegroundOpacityLevel);
        mWidgetWeatherPaint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(linePath, mWidgetWeatherPaint);

        mWidgetWeatherPaint.setColor(Color.parseColor(mBackgroundColour));
        mWidgetWeatherPaint.setAlpha(255);
        mWidgetWeatherPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(linePath, mWidgetWeatherPaint);
    }

    private void drawSnowFlake(Canvas canvas, float x, float y) {
        flakePath.reset();
        flakePath.moveTo(x - getPixelsFromDp(2), y + getPixelsFromDp(19));
        flakePath.rMoveTo(getPixelsFromDp(-2), getPixelsFromDp(4));
        flakePath.rLineTo(getPixelsFromDp(6), getPixelsFromDp(0));
        flakePath.rMoveTo(getPixelsFromDp(-5), getPixelsFromDp(-4));
        flakePath.rLineTo(getPixelsFromDp(4), getPixelsFromDp(8));
        flakePath.rMoveTo(getPixelsFromDp(0), getPixelsFromDp(-8));
        flakePath.rLineTo(getPixelsFromDp(-4), getPixelsFromDp(8));
        flakePath.close();

        mWidgetWeatherPaint.setColor(Color.parseColor(mForegroundColour));
        mWidgetWeatherPaint.setAlpha(mForegroundOpacityLevel);
        mWidgetWeatherPaint.setStyle(Paint.Style.STROKE);
        mWidgetWeatherPaint.setStrokeWidth(getPixelsFromDp(1));
        canvas.drawPath(flakePath, mWidgetWeatherPaint);

        mWidgetWeatherPaint.setColor(Color.parseColor(mBackgroundColour));
        mWidgetWeatherPaint.setAlpha(255);
        mWidgetWeatherPaint.setStyle(Paint.Style.FILL);
        mWidgetWeatherPaint.setStrokeWidth(getPixelsFromDp(1));
        canvas.drawPath(flakePath, mWidgetWeatherPaint);
        mWidgetWeatherPaint.setStrokeWidth(getPixelsFromDp(2));
    }

    private void drawLightning(Canvas canvas, float x, float y) {
        lightningPath.reset();
        lightningPath.moveTo(x, y + getPixelsFromDp(11));
        lightningPath.rLineTo(getPixelsFromDp(-1), 0);
        lightningPath.rLineTo(getPixelsFromDp(-7), getPixelsFromDp(10));
        lightningPath.rLineTo(getPixelsFromDp(4), 0);
        lightningPath.rLineTo(getPixelsFromDp(-2), getPixelsFromDp(7));
        lightningPath.rLineTo(getPixelsFromDp(1), 0);
        lightningPath.rLineTo(getPixelsFromDp(6), getPixelsFromDp(-9));
        lightningPath.rLineTo(getPixelsFromDp(-4), 0);
        lightningPath.close();

        mWidgetWeatherPaint.setColor(Color.parseColor(mBackgroundColour));
        mWidgetWeatherPaint.setAlpha(255);
        mWidgetWeatherPaint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(lightningPath, mWidgetWeatherPaint);

        mWidgetWeatherPaint.setColor(Color.parseColor(mForegroundColour));
        mWidgetWeatherPaint.setAlpha(mForegroundOpacityLevel);
        mWidgetWeatherPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(lightningPath, mWidgetWeatherPaint);
    }

    private void drawFog(Canvas canvas, float x, float y) {
        float left = x - getPixelsFromDp(5);
        float top = y - getPixelsFromDp(4);
        float length = getPixelsFromDp(14);

        mWidgetWeatherPaint.setColor(Color.parseColor(mBackgroundColour));
        mWidgetWeatherPaint.setAlpha(255);
        mWidgetWeatherPaint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(left, top, left + length, top, mWidgetWeatherPaint);
        top += getPixelsFromDp(4);
        canvas.drawLine(left, top, left + length, top, mWidgetWeatherPaint);
        top += getPixelsFromDp(4);
        canvas.drawLine(left, top, left + length, top, mWidgetWeatherPaint);
        top += getPixelsFromDp(4);
        canvas.drawLine(left, top, left + length, top, mWidgetWeatherPaint);

        top = y - getPixelsFromDp(4);

        mWidgetWeatherPaint.setColor(Color.parseColor(mForegroundColour));
        mWidgetWeatherPaint.setAlpha(mForegroundOpacityLevel);
        mWidgetWeatherPaint.setStyle(Paint.Style.FILL);
        canvas.drawLine(left, top, left + length, top, mWidgetWeatherPaint);
        top += getPixelsFromDp(4);
        canvas.drawLine(left, top, left + length, top, mWidgetWeatherPaint);
        top += getPixelsFromDp(4);
        canvas.drawLine(left, top, left + length, top, mWidgetWeatherPaint);
        top += getPixelsFromDp(4);
        canvas.drawLine(left, top, left + length, top, mWidgetWeatherPaint);
    }
    //endregion

    //region String methods
    private String formatTwoDigitNumber(int number) {
        return String.format("%02d", number);
    }

    private String formatTwoDigitHourNumber(int hour) {
        if (mToggleAmPm)
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

    //region Colour methods
    private void updatePaint(Paint paint, String colour, int opacityLevel) {
        paint.setColor(Color.parseColor(colour));
        paint.setAlpha(opacityLevel);
    }

    private void updatePaint(Paint paint, String colour, int opacityLevel, float strokeWidth) {
        updatePaint(paint, colour, opacityLevel);
        paint.setStrokeWidth(mIsInAmbientMode ? getPixelsFromDp(2) : strokeWidth);
    }

    private void setBackgroundColor(String color) {
        mBackgroundColour = color;
        updatePaint(mBackgroundPaint, color, 255);
    }

    private void setForegroundColor(String color) {
        mForegroundColour = color;

        updatePaint(mHourPaint, color, mForegroundOpacityLevel, getPixelsFromDp(3));
        updatePaint(mMinutePaint, color, mForegroundOpacityLevel, getPixelsFromDp(3));

        updatePaint(mDigitalHourPaint, color, mForegroundOpacityLevel);
        updatePaint(mDigitalMinutePaint, color, mForegroundOpacityLevel);

        updatePaint(mTextElementPaint, color, mForegroundOpacityLevel);
        updatePaint(mBatteryPaint, color, mForegroundOpacityLevel);

        updatePaint(mHourTickPaint, color, 100);
        updatePaint(mMinuteTickPaint, color, 100);
    }

    private void setAccentColor(String color) {
        mAccentColour = color;
        updatePaint(mSecondPaint, color, mAccentOpacityLevel, getPixelsFromDp(2));
    }

    private void setMiddleColor(String color) {
        mMiddleColour = color;
        updatePaint(mColonPaint, color, mForegroundOpacityLevel);
        updatePaint(mBatteryFullPaint, color, mForegroundOpacityLevel);
    }

    private Paint createTextPaint(int defaultInteractiveColour) {
        Paint paint = new Paint();
        paint.setColor(defaultInteractiveColour);
        paint.setTypeface(NORMAL_TYPEFACE);
        paint.setAntiAlias(true);
        paint.setStrokeJoin(Paint.Join.BEVEL);
        paint.setStrokeWidth(getPixelsFromDp(2));
        return paint;
    }
    //endregion
}

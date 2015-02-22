package com.greenman.digilogue;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Wearable;
import com.greenman.common.Utility;


public class DigilogueWearableConfigActivity extends Activity implements WearableListView.ClickListener, WearableListView.OnScrollListener {
    private static final String TAG = "WearableConfigActivity";

    private DataMap existingConfig;
    private TextView mHeaderBackground;

    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_digilogue_wearable_config);

        // TODO: figure out why this activity sometimes crashes

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "onConnected: " + connectionHint);
                        }

                        WatchFaceUtil.fetchConfigDataMap(mGoogleApiClient, new WatchFaceUtil.FetchConfigDataMapCallback() {
                            @Override
                            public void onConfigDataMapFetched(DataMap config) {
                                existingConfig = config;
                            }
                        });
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "onConnectionSuspended: " + cause);
                        }
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "onConnectionFailed: " + result);
                        }
                    }
                })
                .addApi(Wearable.API)
                .build();

        mHeaderBackground = (TextView) findViewById(R.id.background_header);
        WearableListView listView = (WearableListView) findViewById(R.id.background_color_picker);
        BoxInsetLayout content = (BoxInsetLayout) findViewById(R.id.content);
        // BoxInsetLayout adds padding by default on round devices. Add some on square devices.
        content.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                if (!insets.isRound()) {
                    v.setPaddingRelative(
                            getResources().getDimensionPixelSize(R.dimen.content_padding_start),
                            v.getPaddingTop(),
                            v.getPaddingEnd(),
                            v.getPaddingBottom());
                }
                return v.onApplyWindowInsets(insets);
            }
        });

        listView.setHasFixedSize(true);
        listView.setClickListener(this);
        listView.addOnScrollListener(this);

        String[] colors = getResources().getStringArray(R.array.color_array);
        listView.setAdapter(new ColorListAdapter(colors));

        /*WearableListView foregroundListView = (WearableListView) findViewById(R.id.foreground_color_picker);
        // BoxInsetLayout adds padding by default on round devices. Add some on square devices.
        content.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                if (!insets.isRound()) {
                    v.setPaddingRelative(
                            (int) getResources().getDimensionPixelSize(R.dimen.content_padding_start),
                            v.getPaddingTop(),
                            v.getPaddingEnd(),
                            v.getPaddingBottom());
                }
                return v.onApplyWindowInsets(insets);
            }
        });

        foregroundListView.setHasFixedSize(true);
        foregroundListView.setClickListener(this);
        foregroundListView.addOnScrollListener(this);

        foregroundListView.setAdapter(new ColorListAdapter(colors));*/
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

    @Override // WearableListView.ClickListener
    public void onClick(WearableListView.ViewHolder viewHolder) {
        ColorItemViewHolder colorItemViewHolder = (ColorItemViewHolder) viewHolder;
        updateConfigDataItem(colorItemViewHolder.mColorItem.getColor());
        finish();
    }

    @Override // WearableListView.ClickListener
    public void onTopEmptyRegionClick() {
    }

    @Override // WearableListView.OnScrollListener
    public void onScroll(int scroll) {
    }

    @Override // WearableListView.OnScrollListener
    public void onAbsoluteScrollChange(int scroll) {
        float newTranslation = Math.min(-scroll, 0);
        mHeaderBackground.setTranslationY(newTranslation);
    }

    @Override // WearableListView.OnScrollListener
    public void onScrollStateChanged(int scrollState) {
    }

    @Override // WearableListView.OnScrollListener
    public void onCentralPositionChanged(int centralPosition) {
    }

    private void updateConfigDataItem(final String backgroundColour) {
        String foregroundColour = Utility.COLOUR_NAME_DEFAULT_AND_AMBIENT_FOREGROUND;
        String middleColour = Utility.COLOUR_NAME_DEFAULT_AND_AMBIENT_MIDDLE;
        String accentColour = Utility.COLOUR_NAME_DEFAULT_AND_AMBIENT_ACCENT;

        switch (backgroundColour) {
            /*case "black":
                foregroundColour = getString(R.string.color_white);
                middleColour = getString(R.string.color_gray);
                accentColour = getString(R.string.color_red);
                break;*/
            case "blue":
                foregroundColour = getString(R.string.color_white);
                /*middleColour = getString(R.string.color_gray);
                accentColour = getString(R.string.color_red);*/
                break;
            case "gray":
                foregroundColour = getString(R.string.color_black);
                middleColour = getString(R.string.color_white);
                //accentColour = getString(R.string.color_red);
                break;
            case "green":
                foregroundColour = getString(R.string.color_black);
                /*middleColour = getString(R.string.color_gray);
                accentColour = getString(R.string.color_red);*/
                break;
            /*case "navy":
                foregroundColour = getString(R.string.color_white);
                middleColour = getString(R.string.color_gray);
                accentColour = getString(R.string.color_red);
                break;*/
            case "red":
                /*foregroundColour = getString(R.string.color_white);
                middleColour = getString(R.string.color_gray);*/
                accentColour = getString(R.string.color_black);
                break;
            case "white":
                foregroundColour = getString(R.string.color_black);
                /*middleColour = getString(R.string.color_gray);
                accentColour = getString(R.string.color_red);*/
                break;
        }

        DataMap configKeysToOverwrite = existingConfig;

        if (configKeysToOverwrite == null)
            configKeysToOverwrite = new DataMap();

        //configKeysToOverwrite.putBoolean(WatchFaceUtil.KEY_TOGGLE_AM_PM, true); // TODO: checkbox
        configKeysToOverwrite.putString(Utility.KEY_BACKGROUND_COLOUR, backgroundColour);
        configKeysToOverwrite.putString(Utility.KEY_MIDDLE_COLOUR, middleColour);
        configKeysToOverwrite.putString(Utility.KEY_FOREGROUND_COLOUR, foregroundColour);
        configKeysToOverwrite.putString(Utility.KEY_ACCENT_COLOUR, accentColour);

        WatchFaceUtil.overwriteKeysInConfigDataMap(mGoogleApiClient, configKeysToOverwrite);
    }

    private class ColorListAdapter extends WearableListView.Adapter {
        private final String[] mColors;

        public ColorListAdapter(String[] colors) {
            mColors = colors;
        }

        @Override
        public ColorItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ColorItemViewHolder(new ColorItem(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
            ColorItemViewHolder colorItemViewHolder = (ColorItemViewHolder) holder;
            String colorName = mColors[position];
            colorItemViewHolder.mColorItem.setColor(colorName);

            RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int colorPickerItemMargin = (int) getResources().getDimension(R.dimen.digital_config_color_picker_item_margin);
            // Add margins to first and last item to make it possible for user to tap on them.
            if (position == 0) {
                layoutParams.setMargins(0, colorPickerItemMargin, 0, 0);
            } else if (position == mColors.length - 1) {
                layoutParams.setMargins(0, 0, 0, colorPickerItemMargin);
            } else {
                layoutParams.setMargins(0, 0, 0, 0);
            }
            colorItemViewHolder.itemView.setLayoutParams(layoutParams);
        }

        @Override
        public int getItemCount() {
            return mColors.length;
        }
    }

    /**
     * The layout of a color item including image and label.
     */
    private static class ColorItem extends LinearLayout implements WearableListView.OnCenterProximityListener {
        /**
         * The duration of the expand/shrink animation.
         */
        private static final int ANIMATION_DURATION_MS = 150;
        /**
         * The ratio for the size of a circle in shrink state.
         */
        private static final float SHRINK_CIRCLE_RATIO = .75f;

        private static final float SHRINK_LABEL_ALPHA = .5f;
        private static final float EXPAND_LABEL_ALPHA = 1f;

        private final TextView mLabel;
        private final CircledImageView mColor;

        private final float mExpandCircleRadius;
        private final float mShrinkCircleRadius;

        private final ObjectAnimator mExpandCircleAnimator;
        private final ObjectAnimator mExpandLabelAnimator;
        private final AnimatorSet mExpandAnimator;

        private final ObjectAnimator mShrinkCircleAnimator;
        private final ObjectAnimator mShrinkLabelAnimator;
        private final AnimatorSet mShrinkAnimator;

        public ColorItem(Context context) {
            super(context);
            View.inflate(context, R.layout.colour_picker_item, this);

            mLabel = (TextView) findViewById(R.id.label);
            mColor = (CircledImageView) findViewById(R.id.color);

            mExpandCircleRadius = mColor.getCircleRadius();
            mShrinkCircleRadius = mExpandCircleRadius * SHRINK_CIRCLE_RATIO;

            mShrinkCircleAnimator = ObjectAnimator.ofFloat(mColor, "circleRadius", mExpandCircleRadius, mShrinkCircleRadius);
            mShrinkLabelAnimator = ObjectAnimator.ofFloat(mLabel, "alpha", EXPAND_LABEL_ALPHA, SHRINK_LABEL_ALPHA);
            mShrinkAnimator = new AnimatorSet().setDuration(ANIMATION_DURATION_MS);
            mShrinkAnimator.playTogether(mShrinkCircleAnimator, mShrinkLabelAnimator);

            mExpandCircleAnimator = ObjectAnimator.ofFloat(mColor, "circleRadius", mShrinkCircleRadius, mExpandCircleRadius);
            mExpandLabelAnimator = ObjectAnimator.ofFloat(mLabel, "alpha", SHRINK_LABEL_ALPHA, EXPAND_LABEL_ALPHA);
            mExpandAnimator = new AnimatorSet().setDuration(ANIMATION_DURATION_MS);
            mExpandAnimator.playTogether(mExpandCircleAnimator, mExpandLabelAnimator);
        }

        @Override
        public void onCenterPosition(boolean animate) {
            if (animate) {
                mShrinkAnimator.cancel();
                if (!mExpandAnimator.isRunning()) {
                    mExpandCircleAnimator.setFloatValues(mColor.getCircleRadius(), mExpandCircleRadius);
                    mExpandLabelAnimator.setFloatValues(mLabel.getAlpha(), EXPAND_LABEL_ALPHA);
                    mExpandAnimator.start();
                }
            } else {
                mExpandAnimator.cancel();
                mColor.setCircleRadius(mExpandCircleRadius);
                mLabel.setAlpha(EXPAND_LABEL_ALPHA);
            }
        }

        @Override
        public void onNonCenterPosition(boolean animate) {
            if (animate) {
                mExpandAnimator.cancel();
                if (!mShrinkAnimator.isRunning()) {
                    mShrinkCircleAnimator.setFloatValues(mColor.getCircleRadius(), mShrinkCircleRadius);
                    mShrinkLabelAnimator.setFloatValues(mLabel.getAlpha(), SHRINK_LABEL_ALPHA);
                    mShrinkAnimator.start();
                }
            } else {
                mShrinkAnimator.cancel();
                mColor.setCircleRadius(mShrinkCircleRadius);
                mLabel.setAlpha(SHRINK_LABEL_ALPHA);
            }
        }

        private void setColor(String colorName) {
            mLabel.setText(colorName);
            mColor.setCircleColor(Color.parseColor(colorName));
        }

        private String getColor() {
            return mLabel.getText().toString().toLowerCase();
            //return mColor.getDefaultCircleColor();
        }

        // TODO: show colours in icon
        /*private class DualColourDrawable extends Drawable {
            private Paint mPaint, mPaintAlternate;

            public DualColourDrawable(String colorName) {
                mPaint = new Paint();
                mPaint.setStrokeWidth(50f);

                mPaintAlternate = new Paint();
                mPaintAlternate.setStrokeWidth(50f);

                if (colorName.toLowerCase().contains("black")) {
                    mPaint.setColor(Color.parseColor("black"));
                    mPaintAlternate.setColor(Color.parseColor("white"));
                } else if (colorName.toLowerCase().contains("blue")) {
                    mPaint.setColor(Color.parseColor("blue"));
                    mPaintAlternate.setColor(Color.parseColor("red"));
                } else if (colorName.toLowerCase().contains("navy")) {
                    mPaint.setColor(Color.parseColor("navy"));
                    mPaintAlternate.setColor(Color.parseColor("green"));
                }
            }

            @Override
            public void draw(Canvas canvas) {
                //setBounds(0, 0, 50, 50);

                canvas.drawColor(Color.parseColor("black"));
                //canvas.drawLine(50, 0, 50, 50, mPaintAlternate);
            }

            @Override
            public void setAlpha(int alpha) {

            }

            @Override
            public void setColorFilter(ColorFilter cf) {

            }

            @Override
            public int getOpacity() {
                return PixelFormat.TRANSLUCENT;
            }
        }*/
    }

    private static class ColorItemViewHolder extends WearableListView.ViewHolder {
        private final ColorItem mColorItem;

        public ColorItemViewHolder(ColorItem colorItem) {
            super(colorItem);
            mColorItem = colorItem;
        }
    }
}

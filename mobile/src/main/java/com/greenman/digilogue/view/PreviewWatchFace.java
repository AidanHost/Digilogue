package com.greenman.digilogue.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.gms.wearable.DataMap;
import com.greenman.common.Utility;
import com.greenman.common.WatchFace;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class PreviewWatchFace extends View {
    //region variables
    private boolean mIsInAmbientMode = false;
    private DataMap mConfig;
    //endregion

    public PreviewWatchFace(Context context) {
        super(context);

        WatchFace.init();
    }

    public PreviewWatchFace(Context context, AttributeSet attrs) {
        super(context, attrs);

        WatchFace.init();
    }

    //region overrides
    @Override
    public void onDraw(Canvas canvas) {
        WatchFace.draw(canvas, getWidth(), getHeight(), true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            mIsInAmbientMode = !mIsInAmbientMode;
            WatchFace.updateUI(dataMapToBundle(), mIsInAmbientMode);
            invalidate();
        }

        return true;
    }
    //endregion

    //region custom methods
    public void setConfig(DataMap config) {
        mConfig = config;
        WatchFace.updateUI(dataMapToBundle(), mIsInAmbientMode);
        invalidate();
    }

    private Bundle dataMapToBundle() {
        Bundle bundle = new Bundle();

        for (int i = 0; i < mConfig.keySet().size(); i++) {
            String key = mConfig.keySet().toArray()[i].toString();
            Object value = mConfig.get(key);
            if (value instanceof Integer) {
                bundle.putInt(key, (int)value);
            } else if (value instanceof Boolean) {
                bundle.putBoolean(key, (boolean)value);
            } else if (value instanceof String) {
                bundle.putString(key, (String)value);
            } else if (value instanceof Long) {
                bundle.putLong(key, (long)value);
            }
        }

        return bundle;
    }

    public void setHintText(String text, boolean showHint) {
        WatchFace.setHintText(text, showHint);
    }
    //endregion
}

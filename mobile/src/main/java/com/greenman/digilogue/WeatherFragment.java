package com.greenman.digilogue;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.greenman.common.Utility;

public class WeatherFragment extends Fragment {
    public static final String ARG_TOGGLE_WEATHER = "toggleWeather";
    public static final String ARG_AUTO_LOCATION = "autoLocation";
    public static final String ARG_FAHRENHEIT = "fahrenheit";
    public static final String ARG_LOCATION = "location";
    public static final String ARG_DATA = "data";

    private CheckBox toggle_weather;
    private CheckBox widget_weather_fahrenheit;
    private CheckBox widget_weather_auto_location;

    private LinearLayout location;
    private TextView widget_weather_text_data;
    private EditText widget_weather_text_location;

    private boolean mToggleWeather = Utility.CONFIG_DEFAULT_TOGGLE_WEATHER;
    private boolean mAutoLocation = Utility.CONFIG_DEFAULT_WIDGET_WEATHER_AUTO_LOCATION;
    private boolean mFahrenheit = Utility.CONFIG_DEFAULT_WIDGET_WEATHER_FAHRENHEIT;
    private String mManualLocation = Utility.CONFIG_DEFAULT_WIDGET_WEATHER_LOCATION;
    private String mData = "";

    private OnFragmentInteractionListener mListener;

    public WeatherFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getActivity() instanceof DigilogueConfigActivity) {
            mToggleWeather = ((DigilogueConfigActivity)getActivity()).toggleWeather;
            mFahrenheit = ((DigilogueConfigActivity)getActivity()).fahrenheit;
            mAutoLocation = ((DigilogueConfigActivity)getActivity()).autoLocation;
            mData = ((DigilogueConfigActivity)getActivity()).weatherData;
            mManualLocation = ((DigilogueConfigActivity)getActivity()).manualLocation;
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_weather, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        toggle_weather.setChecked(mToggleWeather);
        widget_weather_auto_location.setChecked(mAutoLocation);
        widget_weather_fahrenheit.setChecked(mFahrenheit);
        setText();

        toggle_weather.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mToggleWeather = toggle_weather.isChecked();
                fireWeatherChanged();
            }
        });

        widget_weather_auto_location.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    location.setVisibility(View.GONE);
                else
                    location.setVisibility(View.VISIBLE);

                fireWeatherChanged();
            }
        });

        widget_weather_fahrenheit.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                fireWeatherChanged();
            }
        });

        widget_weather_text_location.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                fireWeatherChanged();
            }
        });

        if (!mAutoLocation)
            location.setVisibility(View.VISIBLE);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstance) {
        super.onViewCreated(view, savedInstance);

        toggle_weather = (CheckBox) view.findViewById(R.id.toggle_weather);
        widget_weather_text_data = (TextView) view.findViewById(R.id.widget_weather_text_data);
        widget_weather_fahrenheit = (CheckBox) view.findViewById(R.id.widget_weather_fahrenheit);
        widget_weather_auto_location = (CheckBox) view.findViewById(R.id.widget_weather_auto_location);
        widget_weather_text_location = (EditText) view.findViewById(R.id.widget_weather_text_location);
        location = (LinearLayout) view.findViewById(R.id.location);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void fireWeatherChanged() {
        if (mListener != null) {
            Bundle weather = new Bundle();
            weather.putBoolean(ARG_TOGGLE_WEATHER, toggle_weather.isChecked());
            weather.putBoolean(ARG_AUTO_LOCATION, widget_weather_auto_location.isChecked());
            weather.putBoolean(ARG_FAHRENHEIT, widget_weather_fahrenheit.isChecked());
            weather.putString(ARG_LOCATION, widget_weather_text_location.getText().toString());

            mListener.onWeatherChanged(weather);
        }
    }

    private void setText() {
        if (!mData.isEmpty())
            widget_weather_text_data.setText(mData);
        else
            widget_weather_text_data.setText(getString(R.string.weather_data_info));

        if (!mManualLocation.isEmpty())
            widget_weather_text_location.setText(mManualLocation);
    }

    public void setWeather(boolean toggle) {
        mToggleWeather = toggle;
        toggle_weather.setChecked(mToggleWeather);
    }

    public void setAutoLocation(boolean toggle) {
        mAutoLocation = toggle;
        widget_weather_auto_location.setChecked(mAutoLocation);
    }

    public void setFahrenheit(boolean toggle) {
        mFahrenheit = toggle;
        widget_weather_fahrenheit.setChecked(mFahrenheit);
    }

    public void setManualLocation(String location) {
        mManualLocation = location;
        if (!mManualLocation.isEmpty())
            widget_weather_text_location.setText(mManualLocation);
    }

    public void setWeatherData(String data) {
        mData = data;
        if (!mData.isEmpty())
            widget_weather_text_data.setText(mData);
        else
            widget_weather_text_data.setText(getString(R.string.weather_data_info));
    }

    public interface OnFragmentInteractionListener {
        public void onWeatherChanged(Bundle weather);
    }
}

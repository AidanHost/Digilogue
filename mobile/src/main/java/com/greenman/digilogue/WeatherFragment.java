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

    public boolean mToggleWeather;
    public boolean mAutoLocation;
    public boolean mFahrenheit;
    public String mManualLocation;
    public String mData;

    /*public void setWeather(boolean weather) {
        mToggleWeather = weather;
        toggle_weather.setChecked(weather);
    }

    public void setAutoLocation(boolean autoLocation) {
        mAutoLocation = autoLocation;
        widget_weather_auto_location.setChecked(autoLocation);
    }

    public void setFahrenheit(boolean fahrenheit) {
        mFahrenheit = fahrenheit;
        widget_weather_fahrenheit.setChecked(fahrenheit);
    }

    public void setLocation(String location) {
        mManualLocation = location;
        widget_weather_text_location.setText(location);
    }*/

    private OnFragmentInteractionListener mListener;

    public static WeatherFragment newInstance(boolean toggleWeather, boolean autoLocation, boolean fahrenheit, String location, String data) {
        WeatherFragment fragment = new WeatherFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_TOGGLE_WEATHER, toggleWeather);
        args.putBoolean(ARG_AUTO_LOCATION, autoLocation);
        args.putBoolean(ARG_FAHRENHEIT, fahrenheit);
        args.putString(ARG_LOCATION, location);
        args.putString(ARG_DATA, data);
        fragment.setArguments(args);
        return fragment;
    }

    public WeatherFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mToggleWeather = getArguments().getBoolean(ARG_TOGGLE_WEATHER);
            mAutoLocation = getArguments().getBoolean(ARG_AUTO_LOCATION);
            mFahrenheit = getArguments().getBoolean(ARG_FAHRENHEIT);
            mManualLocation = getArguments().getString(ARG_LOCATION);
            mData = getArguments().getString(ARG_DATA);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_weather, container, false);
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

    @Override
    public void onViewCreated(View view, Bundle savedInstance) {
        toggle_weather = (CheckBox) view.findViewById(R.id.toggle_weather);
        widget_weather_text_data = (TextView) view.findViewById(R.id.widget_weather_text_data);
        widget_weather_fahrenheit = (CheckBox) view.findViewById(R.id.widget_weather_fahrenheit);
        widget_weather_auto_location = (CheckBox) view.findViewById(R.id.widget_weather_auto_location);
        widget_weather_text_location = (EditText) view.findViewById(R.id.widget_weather_text_location);
        location = (LinearLayout) view.findViewById(R.id.location);

        toggle_weather.setChecked(mToggleWeather);
        widget_weather_auto_location.setChecked(mAutoLocation);
        widget_weather_fahrenheit.setChecked(mFahrenheit);
        setText();

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

        toggle_weather.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mToggleWeather = toggle_weather.isChecked();
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

    private void setText() {
        if (!mData.isEmpty())
            widget_weather_text_data.setText(mData);
        else
            widget_weather_text_data.setText(getString(R.string.weather_data_info));

        if (!mManualLocation.isEmpty())
            widget_weather_text_location.setText(mManualLocation);
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

    public interface OnFragmentInteractionListener {
        public void onWeatherChanged(Bundle weather);
    }

}

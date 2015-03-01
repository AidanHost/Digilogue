package com.greenman.digilogue;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

public class TogglesFragment extends Fragment {
    public static final String ARG_AM_PM = "toggleAmPm";
    public static final String ARG_DAY_DATE = "toggleDayDate";
    public static final String ARG_DIM_COLOUR = "toggleDimColour";
    public static final String ARG_SOLID_TEXT = "toggleSolidText";
    public static final String ARG_DIGITAL = "toggleDigital";
    public static final String ARG_ANALOGUE = "toggleAnalogue";
    public static final String ARG_BATTERY = "toggleBattery";
    public static final String ARG_FIX_CHIN = "toggleFixChin";
    public static final String ARG_DIAL = "toggleDial";
    public static final String ARG_WEATHER = "toggleWeather";

    private boolean mToggleAmPm;
    private boolean mToggleDayDate;
    private boolean mToggleDimColour;
    private boolean mToggleSolidText;
    private boolean mToggleDigital;
    private boolean mToggleAnalogue;
    private boolean mToggleBattery;
    private boolean mToggleFixChin;
    private boolean mToggleDial;
    private boolean mToggleWeather;

    public boolean getToggleWeather() {
        return mToggleWeather;
    }

    private OnFragmentInteractionListener mListener;

    public static TogglesFragment newInstance(boolean toggleAmPm,
                                              boolean toggleDayDate,
                                              boolean toggleDimColour,
                                              boolean toggleSolidText,
                                              boolean toggleDigital,
                                              boolean toggleAnalogue,
                                              boolean toggleBattery,
                                              boolean toggleFixChin,
                                              boolean toggleDial,
                                              boolean toggleWeather) {
        TogglesFragment fragment = new TogglesFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_AM_PM, toggleAmPm);
        args.putBoolean(ARG_DAY_DATE, toggleDayDate);
        args.putBoolean(ARG_DIM_COLOUR, toggleDimColour);
        args.putBoolean(ARG_SOLID_TEXT, toggleSolidText);
        args.putBoolean(ARG_DIGITAL, toggleDigital);
        args.putBoolean(ARG_ANALOGUE, toggleAnalogue);
        args.putBoolean(ARG_BATTERY, toggleBattery);
        args.putBoolean(ARG_FIX_CHIN, toggleFixChin);
        args.putBoolean(ARG_DIAL, toggleDial);
        args.putBoolean(ARG_WEATHER, toggleWeather);
        fragment.setArguments(args);
        return fragment;
    }

    public TogglesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mToggleAmPm = getArguments().getBoolean(ARG_AM_PM);
            mToggleDayDate = getArguments().getBoolean(ARG_DAY_DATE);
            mToggleDimColour = getArguments().getBoolean(ARG_DIM_COLOUR);
            mToggleSolidText = getArguments().getBoolean(ARG_SOLID_TEXT);
            mToggleDigital = getArguments().getBoolean(ARG_DIGITAL);
            mToggleAnalogue = getArguments().getBoolean(ARG_ANALOGUE);
            mToggleBattery = getArguments().getBoolean(ARG_BATTERY);
            mToggleFixChin = getArguments().getBoolean(ARG_FIX_CHIN);
            mToggleDial = getArguments().getBoolean(ARG_DIAL);
            mToggleWeather = getArguments().getBoolean(ARG_WEATHER);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_toggles, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstance) {
        final CheckBox toggle_am_pm = (CheckBox) view.findViewById(R.id.toggle_am_pm);
        final CheckBox toggle_day_date = (CheckBox) view.findViewById(R.id.toggle_date_day);
        final CheckBox toggle_dim_colour = (CheckBox) view.findViewById(R.id.toggle_dim);
        final CheckBox toggle_solid_text = (CheckBox) view.findViewById(R.id.toggle_solid_number);
        final CheckBox toggle_digital = (CheckBox) view.findViewById(R.id.toggle_digital);
        final CheckBox toggle_analogue = (CheckBox) view.findViewById(R.id.toggle_analogue);
        final CheckBox toggle_battery = (CheckBox) view.findViewById(R.id.toggle_battery);
        final CheckBox toggle_fix_chin = (CheckBox) view.findViewById(R.id.toggle_fix_chin);
        final CheckBox toggle_dial = (CheckBox) view.findViewById(R.id.toggle_dial);
        final CheckBox toggle_weather = (CheckBox) view.findViewById(R.id.toggle_weather);

        setUpCheckBox(toggle_am_pm, mToggleAmPm);
        setUpCheckBox(toggle_day_date, mToggleDayDate);
        setUpCheckBox(toggle_dim_colour, mToggleDimColour);
        setUpCheckBox(toggle_solid_text, mToggleSolidText);
        setUpCheckBox(toggle_digital, mToggleDigital);
        setUpCheckBox(toggle_analogue, mToggleAnalogue);
        setUpCheckBox(toggle_battery, mToggleBattery);
        setUpCheckBox(toggle_fix_chin, mToggleFixChin);
        setUpCheckBox(toggle_dial, mToggleDial);
        setUpCheckBox(toggle_weather, mToggleWeather);

        CompoundButton.OnCheckedChangeListener checkBoxListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mToggleWeather = toggle_weather.isChecked();
                if (mListener != null) {
                    Bundle toggles = new Bundle();
                    toggles.putBoolean(ARG_AM_PM, toggle_am_pm.isChecked());
                    toggles.putBoolean(ARG_DAY_DATE, toggle_day_date.isChecked());
                    toggles.putBoolean(ARG_DIM_COLOUR, toggle_dim_colour.isChecked());
                    toggles.putBoolean(ARG_SOLID_TEXT, toggle_solid_text.isChecked());
                    toggles.putBoolean(ARG_DIGITAL, toggle_digital.isChecked());
                    toggles.putBoolean(ARG_ANALOGUE, toggle_analogue.isChecked());
                    toggles.putBoolean(ARG_BATTERY, toggle_battery.isChecked());
                    toggles.putBoolean(ARG_FIX_CHIN, toggle_fix_chin.isChecked());
                    toggles.putBoolean(ARG_DIAL, toggle_dial.isChecked());
                    toggles.putBoolean(ARG_WEATHER, toggle_weather.isChecked());
                    mListener.onToggleChanged(toggles);
                }
            }
        };

        toggle_am_pm.setOnCheckedChangeListener(checkBoxListener);
        toggle_day_date.setOnCheckedChangeListener(checkBoxListener);
        toggle_dim_colour.setOnCheckedChangeListener(checkBoxListener);
        toggle_solid_text.setOnCheckedChangeListener(checkBoxListener);
        toggle_digital.setOnCheckedChangeListener(checkBoxListener);
        toggle_analogue.setOnCheckedChangeListener(checkBoxListener);
        toggle_battery.setOnCheckedChangeListener(checkBoxListener);
        toggle_fix_chin.setOnCheckedChangeListener(checkBoxListener);
        toggle_dial.setOnCheckedChangeListener(checkBoxListener);
        toggle_weather.setOnCheckedChangeListener(checkBoxListener);
    }

    private void setUpCheckBox(CheckBox checkBox, boolean toggle) {
        checkBox.setChecked(toggle);
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
        public void onToggleChanged(Bundle toggles);
    }

}

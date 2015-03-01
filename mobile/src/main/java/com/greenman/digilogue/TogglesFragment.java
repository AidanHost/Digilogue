package com.greenman.digilogue;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

public class TogglesFragment extends Fragment {
    private static final String ARG_AM_PM = "toggleAmPm";
    private static final String ARG_DAY_DATE = "toggleDayDate";
    private static final String ARG_DIM_COLOUR = "toggleDimColour";
    private static final String ARG_SOLID_TEXT = "toggleSolidText";
    private static final String ARG_DIGITAL = "toggleDigital";
    private static final String ARG_ANALOGUE = "toggleAnalogue";
    private static final String ARG_BATTERY = "toggleBattery";
    private static final String ARG_FIX_CHIN = "toggleFixChin";
    private static final String ARG_DIAL = "toggleDial";
    private static final String ARG_WEATHER = "toggleWeather";

    private Boolean mToggleAmPm;
    private Boolean mToggleDayDate;
    private Boolean mToggleDimColour;
    private Boolean mToggleSolidText;
    private Boolean mToggleDigital;
    private Boolean mToggleAnalogue;
    private Boolean mToggleBattery;
    private Boolean mToggleFixChin;
    private Boolean mToggleDial;
    private Boolean mToggleWeather;

    private OnFragmentInteractionListener mListener;

    private CheckBox toggle_am_pm;
    private CheckBox toggle_day_date;
    private CheckBox toggle_dim_colour;
    private CheckBox toggle_solid_text;
    private CheckBox toggle_digital;
    private CheckBox toggle_analogue;
    private CheckBox toggle_battery;
    private CheckBox toggle_fix_chin;
    private CheckBox toggle_dial;
    private CheckBox toggle_weather;

    public static TogglesFragment newInstance(Boolean toggleAmPm,
                                              Boolean toggleDayDate,
                                              Boolean toggleDimColour,
                                              Boolean toggleSolidText,
                                              Boolean toggleDigital,
                                              Boolean toggleAnalogue,
                                              Boolean toggleBattery,
                                              Boolean toggleFixChin,
                                              Boolean toggleDial,
                                              Boolean toggleWeather) {
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
        toggle_am_pm = (CheckBox) view.findViewById(R.id.toggle_am_pm);
        toggle_day_date = (CheckBox) view.findViewById(R.id.toggle_date_day);
        toggle_dim_colour = (CheckBox) view.findViewById(R.id.toggle_dim);
        toggle_solid_text = (CheckBox) view.findViewById(R.id.toggle_solid_number);
        toggle_digital = (CheckBox) view.findViewById(R.id.toggle_digital);
        toggle_analogue = (CheckBox) view.findViewById(R.id.toggle_analogue);
        toggle_battery = (CheckBox) view.findViewById(R.id.toggle_battery);
        toggle_fix_chin = (CheckBox) view.findViewById(R.id.toggle_fix_chin);
        toggle_dial = (CheckBox) view.findViewById(R.id.toggle_dial);
        toggle_weather = (CheckBox) view.findViewById(R.id.toggle_weather);

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
                if (mListener != null) {
                    mListener.onToggleChanged(toggle_am_pm.isSelected(),
                            toggle_day_date.isSelected(),
                            toggle_dim_colour.isSelected(),
                            toggle_solid_text.isSelected(),
                            toggle_digital.isSelected(),
                            toggle_analogue.isSelected(),
                            toggle_battery.isSelected(),
                            toggle_fix_chin.isSelected(),
                            toggle_dial.isSelected(),
                            toggle_weather.isSelected());
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

    private void setUpCheckBox(CheckBox checkBox, Boolean toggle) {
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
        public void onToggleChanged(Boolean toggleAmPm,
                                    Boolean toggleDayDate,
                                    Boolean toggleDimColour,
                                    Boolean toggleSolidText,
                                    Boolean toggleDigital,
                                    Boolean toggleAnalogue,
                                    Boolean toggleBattery,
                                    Boolean toggleFixChin,
                                    Boolean toggleDial,
                                    Boolean toggleWeather);
    }

}

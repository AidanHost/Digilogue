package com.greenman.digilogue;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.greenman.common.Utility;

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

    private boolean mToggleAmPm = Utility.CONFIG_DEFAULT_TOGGLE_AM_PM;
    private boolean mToggleDayDate = Utility.CONFIG_DEFAULT_TOGGLE_DAY_DATE;
    private boolean mToggleDimColour = Utility.CONFIG_DEFAULT_TOGGLE_DIM_COLOUR;
    private boolean mToggleSolidText = Utility.CONFIG_DEFAULT_TOGGLE_SOLID_TEXT;
    private boolean mToggleDigital = Utility.CONFIG_DEFAULT_TOGGLE_DIGITAL;
    private boolean mToggleAnalogue = Utility.CONFIG_DEFAULT_TOGGLE_ANALOGUE;
    private boolean mToggleBattery = Utility.CONFIG_DEFAULT_TOGGLE_BATTERY;
    private boolean mToggleFixChin = Utility.CONFIG_DEFAULT_TOGGLE_FIX_CHIN;
    private boolean mToggleDial = Utility.CONFIG_DEFAULT_TOGGLE_DIAL;

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
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getActivity() instanceof DigilogueConfigActivity) {
            mToggleAmPm = ((DigilogueConfigActivity) getActivity()).toggleAmPm;
            mToggleDayDate = ((DigilogueConfigActivity) getActivity()).toggleDayDate;
            mToggleDimColour = ((DigilogueConfigActivity) getActivity()).toggleDimColour;
            mToggleSolidText = ((DigilogueConfigActivity) getActivity()).toggleSolidText;
            mToggleDigital = ((DigilogueConfigActivity) getActivity()).toggleDigital;
            mToggleAnalogue = ((DigilogueConfigActivity) getActivity()).toggleAnalogue;
            mToggleBattery = ((DigilogueConfigActivity) getActivity()).toggleBattery;
            mToggleFixChin = ((DigilogueConfigActivity) getActivity()).toggleFixChin;
            mToggleDial = ((DigilogueConfigActivity) getActivity()).toggleDial;
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_toggles, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        toggle_am_pm.setOnCheckedChangeListener(null);
        toggle_day_date.setOnCheckedChangeListener(null);
        toggle_dim_colour.setOnCheckedChangeListener(null);
        toggle_solid_text.setOnCheckedChangeListener(null);
        toggle_digital.setOnCheckedChangeListener(null);
        toggle_analogue.setOnCheckedChangeListener(null);
        toggle_battery.setOnCheckedChangeListener(null);
        toggle_fix_chin.setOnCheckedChangeListener(null);
        toggle_dial.setOnCheckedChangeListener(null);

        setUpCheckBox(toggle_am_pm, mToggleAmPm);
        setUpCheckBox(toggle_day_date, mToggleDayDate);
        setUpCheckBox(toggle_dim_colour, mToggleDimColour);
        setUpCheckBox(toggle_solid_text, mToggleSolidText);
        setUpCheckBox(toggle_digital, mToggleDigital);
        setUpCheckBox(toggle_analogue, mToggleAnalogue);
        setUpCheckBox(toggle_battery, mToggleBattery);
        setUpCheckBox(toggle_fix_chin, mToggleFixChin);
        setUpCheckBox(toggle_dial, mToggleDial);

        CompoundButton.OnCheckedChangeListener checkBoxListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
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
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstance) {
        super.onViewCreated(view, savedInstance);

        toggle_am_pm = (CheckBox) view.findViewById(R.id.toggle_am_pm);
        toggle_day_date = (CheckBox) view.findViewById(R.id.toggle_date_day);
        toggle_dim_colour = (CheckBox) view.findViewById(R.id.toggle_dim);
        toggle_solid_text = (CheckBox) view.findViewById(R.id.toggle_solid_number);
        toggle_digital = (CheckBox) view.findViewById(R.id.toggle_digital);
        toggle_analogue = (CheckBox) view.findViewById(R.id.toggle_analogue);
        toggle_battery = (CheckBox) view.findViewById(R.id.toggle_battery);
        toggle_fix_chin = (CheckBox) view.findViewById(R.id.toggle_fix_chin);
        toggle_dial = (CheckBox) view.findViewById(R.id.toggle_dial);
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

    private void setUpCheckBox(CheckBox checkBox, boolean toggle) {
        checkBox.setChecked(toggle);
    }

    public interface OnFragmentInteractionListener {
        public void onToggleChanged(Bundle toggles);
    }

}

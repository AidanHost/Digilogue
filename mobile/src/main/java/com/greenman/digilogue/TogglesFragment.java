package com.greenman.digilogue;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

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
    public static final String ARG_AMBIENT_TICKS = "toggleAmbientTicks";
    public static final String ARG_ANALOGUE_ELEMENT_SIZE = "analogueElementSize";
    public static final String ARG_DIGITAL_ELEMENT_SIZE = "digitalElementSize";

    private boolean mToggleAmPm = Utility.CONFIG_DEFAULT_TOGGLE_AM_PM;
    private boolean mToggleDayDate = Utility.CONFIG_DEFAULT_TOGGLE_DAY_DATE;
    private boolean mToggleDimColour = Utility.CONFIG_DEFAULT_TOGGLE_DIM_COLOUR;
    private boolean mToggleSolidText = Utility.CONFIG_DEFAULT_TOGGLE_SOLID_TEXT;
    private boolean mToggleDigital = Utility.CONFIG_DEFAULT_TOGGLE_DIGITAL;
    private boolean mToggleAnalogue = Utility.CONFIG_DEFAULT_TOGGLE_ANALOGUE;
    private boolean mToggleBattery = Utility.CONFIG_DEFAULT_TOGGLE_BATTERY;
    private boolean mToggleFixChin = Utility.CONFIG_DEFAULT_TOGGLE_FIX_CHIN;
    private boolean mToggleDial = Utility.CONFIG_DEFAULT_TOGGLE_DIAL;
    private boolean mToggleAmbientTicks = Utility.CONFIG_DEFAULT_TOGGLE_AMBIENT_TICKS;
    private int mAnalogueElementSize = Utility.CONFIG_DEFAULT_ANALOGUE_ELEMENT_SIZE;
    private int mDigitalElementSize = Utility.CONFIG_DEFAULT_DIGITAL_ELEMENT_SIZE;

    private OnFragmentInteractionListener mListener;

    private Switch toggle_am_pm;
    private Switch toggle_day_date;
    private Switch toggle_dim_colour;
    private Switch toggle_solid_text;
    private Switch toggle_digital;
    private Switch toggle_analogue;
    private Switch toggle_battery;
    private Switch toggle_fix_chin;
    private Switch toggle_dial;
    private Switch toggle_ambient_ticks;
    private TextView analogue_element_size_text;
    private TextView digital_element_size_text;
    private SeekBar analogue_element_size;
    private SeekBar digital_element_size;

    public TogglesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            mToggleAmbientTicks = ((DigilogueConfigActivity) getActivity()).toggleAmbientTicks;
            mAnalogueElementSize = ((DigilogueConfigActivity) getActivity()).analogueElementSize;
            mDigitalElementSize = ((DigilogueConfigActivity) getActivity()).digitalElementSize;
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_toggles, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        toggle_am_pm.setChecked(mToggleAmPm);
        toggle_day_date.setChecked(mToggleDayDate);
        toggle_dim_colour.setChecked(mToggleDimColour);
        toggle_solid_text.setChecked(mToggleSolidText);
        toggle_digital.setChecked(mToggleDigital);
        toggle_analogue.setChecked(mToggleAnalogue);
        toggle_battery.setChecked(mToggleBattery);
        toggle_fix_chin.setChecked(mToggleFixChin);
        toggle_dial.setChecked(mToggleDial);
        toggle_ambient_ticks.setChecked(mToggleAmbientTicks);
        analogue_element_size_text.setText(String.format(getActivity().getString(R.string.analogue_size), mAnalogueElementSize));
        analogue_element_size.setProgress(mAnalogueElementSize);
        digital_element_size_text.setText(String.format(getActivity().getString(R.string.digital_size), mDigitalElementSize));
        digital_element_size.setProgress(mDigitalElementSize);

        toggle_am_pm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mListener != null) {
                    Bundle toggles = new Bundle();
                    toggles.putBoolean(ARG_AM_PM, toggle_am_pm.isChecked());
                    mListener.onToggleChanged(toggles);
                }
            }
        });
        toggle_day_date.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mListener != null) {
                    Bundle toggles = new Bundle();
                    toggles.putBoolean(ARG_DAY_DATE, toggle_day_date.isChecked());
                    mListener.onToggleChanged(toggles);
                }
            }
        });
        toggle_dim_colour.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mListener != null) {
                    Bundle toggles = new Bundle();
                    toggles.putBoolean(ARG_DIM_COLOUR, toggle_dim_colour.isChecked());
                    mListener.onToggleChanged(toggles);
                }
            }
        });
        toggle_solid_text.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mListener != null) {
                    Bundle toggles = new Bundle();
                    toggles.putBoolean(ARG_SOLID_TEXT, toggle_solid_text.isChecked());
                    mListener.onToggleChanged(toggles);
                }
            }
        });
        toggle_digital.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mListener != null) {
                    Bundle toggles = new Bundle();
                    toggles.putBoolean(ARG_DIGITAL, toggle_digital.isChecked());
                    mListener.onToggleChanged(toggles);
                }
            }
        });
        toggle_analogue.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mListener != null) {
                    Bundle toggles = new Bundle();
                    toggles.putBoolean(ARG_ANALOGUE, toggle_analogue.isChecked());
                    mListener.onToggleChanged(toggles);
                }
            }
        });
        toggle_battery.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mListener != null) {
                    Bundle toggles = new Bundle();
                    toggles.putBoolean(ARG_BATTERY, toggle_battery.isChecked());
                    mListener.onToggleChanged(toggles);
                }
            }
        });
        toggle_fix_chin.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mListener != null) {
                    Bundle toggles = new Bundle();
                    toggles.putBoolean(ARG_FIX_CHIN, toggle_fix_chin.isChecked());
                    mListener.onToggleChanged(toggles);
                }
            }
        });
        toggle_dial.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mListener != null) {
                    Bundle toggles = new Bundle();
                    toggles.putBoolean(ARG_DIAL, toggle_dial.isChecked());
                    mListener.onToggleChanged(toggles);
                }
            }
        });
        toggle_ambient_ticks.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mListener != null) {
                    Bundle toggles = new Bundle();
                    toggles.putBoolean(ARG_AMBIENT_TICKS, toggle_ambient_ticks.isChecked());
                    mListener.onToggleChanged(toggles);
                }
            }
        });

        analogue_element_size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                analogue_element_size_text.setText(String.format(getActivity().getString(R.string.analogue_size), progress));

                if (mListener != null) {
                    Bundle toggles = new Bundle();
                    toggles.putInt(ARG_ANALOGUE_ELEMENT_SIZE, analogue_element_size.getProgress());
                    mListener.onToggleChanged(toggles);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        digital_element_size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                digital_element_size_text.setText(String.format(getActivity().getString(R.string.digital_size), progress));

                if (mListener != null) {
                    Bundle toggles = new Bundle();
                    toggles.putInt(ARG_DIGITAL_ELEMENT_SIZE, digital_element_size.getProgress());
                    mListener.onToggleChanged(toggles);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstance) {
        super.onViewCreated(view, savedInstance);

        toggle_am_pm = (Switch) view.findViewById(R.id.toggle_am_pm);
        toggle_day_date = (Switch) view.findViewById(R.id.toggle_date_day);
        toggle_dim_colour = (Switch) view.findViewById(R.id.toggle_dim);
        toggle_solid_text = (Switch) view.findViewById(R.id.toggle_solid_number);
        toggle_digital = (Switch) view.findViewById(R.id.toggle_digital);
        toggle_analogue = (Switch) view.findViewById(R.id.toggle_analogue);
        toggle_battery = (Switch) view.findViewById(R.id.toggle_battery);
        toggle_fix_chin = (Switch) view.findViewById(R.id.toggle_fix_chin);
        toggle_dial = (Switch) view.findViewById(R.id.toggle_dial);
        toggle_ambient_ticks = (Switch) view.findViewById(R.id.toggle_ambient_ticks);
        analogue_element_size_text = (TextView) view.findViewById(R.id.analogue_element_size_text);
        analogue_element_size = (SeekBar) view.findViewById(R.id.analogue_element_size);
        digital_element_size_text = (TextView) view.findViewById(R.id.digital_element_size_text);
        digital_element_size = (SeekBar) view.findViewById(R.id.digital_element_size);
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

    public void setAmPm(boolean toggle) {
        mToggleAmPm = toggle;
        toggle_am_pm.setChecked(mToggleAmPm);
    }

    public void setDayDate(boolean toggle) {
        mToggleDayDate = toggle;
        toggle_day_date.setChecked(mToggleDayDate);
    }

    public void setDimColour(boolean toggle) {
        mToggleDimColour = toggle;
        toggle_dim_colour.setChecked(mToggleDimColour);
    }

    public void setSolidText(boolean toggle) {
        mToggleSolidText = toggle;
        toggle_solid_text.setChecked(mToggleSolidText);
    }

    public void setDigital(boolean toggle) {
        mToggleDigital = toggle;
        toggle_digital.setChecked(mToggleDigital);
    }

    public void setAnalogue(boolean toggle) {
        mToggleAnalogue = toggle;
        toggle_analogue.setChecked(mToggleAnalogue);
    }

    public void setBattery(boolean toggle) {
        mToggleBattery = toggle;
        toggle_battery.setChecked(mToggleBattery);
    }

    public void setFixChin(boolean toggle) {
        mToggleFixChin = toggle;
        toggle_fix_chin.setChecked(mToggleFixChin);
    }

    public void setDial(boolean toggle) {
        mToggleDial = toggle;
        toggle_dial.setChecked(mToggleDial);
    }

    public void setAmbientTicks(boolean toggle) {
        mToggleAmbientTicks = toggle;
        toggle_ambient_ticks.setChecked(mToggleAmbientTicks);
    }

    public void setAnalogueElementSize(int size) {
        mAnalogueElementSize = size;
        analogue_element_size.setProgress(mAnalogueElementSize);
    }

    public void setDigitalElementSize(int size) {
        mDigitalElementSize = size;
        digital_element_size.setProgress(mDigitalElementSize);
    }

    public interface OnFragmentInteractionListener {
        public void onToggleChanged(Bundle toggles);
    }
}

package com.greenman.digilogue;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;

import com.greenman.common.Utility;

public class ColoursFragment extends Fragment {
    public static final String ARG_BACKGROUND = "background";
    public static final String ARG_MIDDLE = "middle";
    public static final String ARG_FOREGROUND = "foreground";
    public static final String ARG_ACCENT = "accent";

    private String[] colourNames;

    private String mBackground = Utility.COLOUR_NAME_DEFAULT_BACKGROUND;
    private String mMiddle = Utility.COLOUR_NAME_DEFAULT_MIDDLE;
    private String mForeground = Utility.COLOUR_NAME_DEFAULT_FOREGROUND;
    private String mAccent = Utility.COLOUR_NAME_DEFAULT_ACCENT;

    private OnFragmentInteractionListener mListener;

    private Spinner background;
    private Spinner middle;
    private Spinner foreground;
    private Spinner accent;

    AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (mListener != null) {
                Bundle colours = new Bundle();
                colours.putString(ColoursFragment.ARG_BACKGROUND, colourNames[background.getSelectedItemPosition()]);
                colours.putString(ColoursFragment.ARG_MIDDLE, colourNames[middle.getSelectedItemPosition()]);
                colours.putString(ColoursFragment.ARG_FOREGROUND, colourNames[foreground.getSelectedItemPosition()]);
                colours.putString(ColoursFragment.ARG_ACCENT, colourNames[accent.getSelectedItemPosition()]);

                mListener.onColourSelected(colours);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    public ColoursFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        colourNames = getResources().getStringArray(R.array.color_array);
        if (getArguments() != null) {
            mBackground = getArguments().getString(ARG_BACKGROUND);
            mMiddle = getArguments().getString(ARG_MIDDLE);
            mForeground = getArguments().getString(ARG_FOREGROUND);
            mAccent = getArguments().getString(ARG_ACCENT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getActivity() instanceof DigilogueConfigActivity) {
            mBackground = ((DigilogueConfigActivity) getActivity()).backgroundColour;
            mMiddle = ((DigilogueConfigActivity) getActivity()).middleColour;
            mForeground = ((DigilogueConfigActivity) getActivity()).foregroundColour;
            mAccent = ((DigilogueConfigActivity) getActivity()).accentColour;
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_colours, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        background.setOnItemSelectedListener(null);
        middle.setOnItemSelectedListener(null);
        foreground.setOnItemSelectedListener(null);
        accent.setOnItemSelectedListener(null);

        setUpColorPickerSelection(background, mBackground);
        setUpColorPickerSelection(middle, mMiddle);
        setUpColorPickerSelection(foreground, mForeground);
        setUpColorPickerSelection(accent, mAccent);

        background.setOnItemSelectedListener(spinnerListener);
        middle.setOnItemSelectedListener(spinnerListener);
        foreground.setOnItemSelectedListener(spinnerListener);
        accent.setOnItemSelectedListener(spinnerListener);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstance) {
        super.onViewCreated(view, savedInstance);

        background = (Spinner) view.findViewById(R.id.background);
        middle = (Spinner) view.findViewById(R.id.middle);
        foreground = (Spinner) view.findViewById(R.id.foreground);
        accent = (Spinner) view.findViewById(R.id.accent);
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

    private void setUpColorPickerSelection(Spinner spinner, final String defaultColorName) {
        if (colourNames == null)
            colourNames = getResources().getStringArray(R.array.color_array);

        if (colourNames != null) {
            for (int i = 0; i < colourNames.length; i++) {
                if (colourNames[i].toLowerCase().equals(defaultColorName.toLowerCase())) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }
    }

    public interface OnFragmentInteractionListener {
        public void onColourSelected(Bundle colours);
    }

}

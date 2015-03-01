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

public class ColoursFragment extends Fragment {
    public static final String ARG_BACKGROUND = "background";
    public static final String ARG_MIDDLE = "middle";
    public static final String ARG_FOREGROUND = "foreground";
    public static final String ARG_ACCENT = "accent";

    private String[] colourNames;

    private String mBackground;
    private String mMiddle;
    private String mForeground;
    private String mAccent;

    private OnFragmentInteractionListener mListener;

    Spinner background;
    Spinner middle;
    Spinner foreground;
    Spinner accent;

    public static ColoursFragment newInstance(String background, String middle, String foreground, String accent) {
        ColoursFragment fragment = new ColoursFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BACKGROUND, background);
        args.putString(ARG_MIDDLE, middle);
        args.putString(ARG_FOREGROUND, foreground);
        args.putString(ARG_ACCENT, accent);
        fragment.setArguments(args);
        return fragment;
    }

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
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_colours, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstance) {
        background = (Spinner) view.findViewById(R.id.background);
        middle = (Spinner) view.findViewById(R.id.middle);
        foreground = (Spinner) view.findViewById(R.id.foreground);
        accent = (Spinner) view.findViewById(R.id.accent);

        setUpColorPickerSelection(background, mBackground);
        setUpColorPickerSelection(middle, mMiddle);
        setUpColorPickerSelection(foreground, mForeground);
        setUpColorPickerSelection(accent, mAccent);

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

        background.setOnItemSelectedListener(spinnerListener);
        middle.setOnItemSelectedListener(spinnerListener);
        foreground.setOnItemSelectedListener(spinnerListener);
        accent.setOnItemSelectedListener(spinnerListener);
    }

    private void setUpColorPickerSelection(Spinner spinner, final String defaultColorName) {
        for (int i = 0; i < colourNames.length; i++) {
            if (colourNames[i].toLowerCase().equals(defaultColorName.toLowerCase())) {
                spinner.setSelection(i);
                break;
            }
        }
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
        public void onColourSelected(Bundle colours);
    }

}

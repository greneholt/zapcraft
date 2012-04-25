package edu.mines.zapcraft.FuelBehavior;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;


public class ButtonListFragment extends Fragment {
	public ViewChangeListener mViewChangeListener;

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mViewChangeListener = (ViewChangeListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement ViewChangeListener");
        }
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		View view = inflater.inflate(R.layout.button_list, container, false);

		Button buttonInstant = (Button) view.findViewById(R.id.instant);
		buttonInstant.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				mViewChangeListener.onViewChange(ViewChangeListener.INSTANT);
			}
		});

		Button buttonMap = (Button) view.findViewById(R.id.map);
		buttonMap.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				mViewChangeListener.onViewChange(ViewChangeListener.MAP);
			}
		});

		Button buttonLogs = (Button) view.findViewById(R.id.logs);
		buttonLogs.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				mViewChangeListener.onViewChange(ViewChangeListener.LOGS);
			}
		});

		Button buttonSettings = (Button) view.findViewById(R.id.settings);
		buttonSettings.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				mViewChangeListener.onViewChange(ViewChangeListener.SETTINGS);
			}
		});

		return view;
	}
}

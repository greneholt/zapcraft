/*******************************************************************************
 * Copyright (C) 2012 Team ZapCraft, Colorado School of Mines
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 ******************************************************************************/
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

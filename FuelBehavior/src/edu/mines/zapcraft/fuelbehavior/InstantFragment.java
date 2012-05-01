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
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class InstantFragment extends Fragment implements Updatable {
	private static final String TAG = InstantFragment.class.getSimpleName();

	private static final int[][] GRADIENTS = new int[][] {
		new int[] { Color.RED, Color.YELLOW, Color.GREEN },
		new int[] { Color.GREEN, Color.YELLOW, Color.RED },
		new int[] { Color.YELLOW, Color.BLUE, Color.MAGENTA },
		new int[] { Color.MAGENTA, Color.BLUE, Color.YELLOW }
	};

	private DataProvider mDataProvider;
	private PeriodicUpdater mUpdater;
	private Gauge mMpgGauge;
	private Gauge mRpmGauge;
	private TextView mMpgText;
	private TextView mRpmText;
	private TextView mSpeedText;
	private TextView mThrottleText;
	private TextView mXAccelText;
	private TextView mYAccelText;

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mDataProvider = (DataProvider) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement DataProvider");
        }
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mUpdater = new PeriodicUpdater(100, this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		View view = inflater.inflate(R.layout.instant, container, false);

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

		mMpgGauge = (Gauge) view.findViewById(R.id.mpgGauge);
		Gradient gradient = getGradient(sharedPrefs.getString("mpg_gradient_style", "0"));
		mMpgGauge.setGradient(gradient);

		mRpmGauge = (Gauge) view.findViewById(R.id.rpmGauge);
		gradient = getGradient(sharedPrefs.getString("rpm_gradient_style", "1"));
		mRpmGauge.setGradient(gradient);

		mMpgText = (TextView) view.findViewById(R.id.mpg_text);
		mRpmText = (TextView) view.findViewById(R.id.rpm_text);
		mSpeedText = (TextView) view.findViewById(R.id.speed_text);
		mThrottleText = (TextView) view.findViewById(R.id.throttle_text);
		mXAccelText = (TextView) view.findViewById(R.id.x_accel_text);
		mYAccelText = (TextView) view.findViewById(R.id.y_accel_text);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();

		mUpdater.start();
	}

	@Override
	public void onPause() {
		super.onPause();

		mUpdater.stop();
	}

	@Override
	public void update() {
		DataHandler dataHandler = mDataProvider.getDataHandler();

		mMpgGauge.setHandValue(dataHandler.getMpg());
		mRpmGauge.setHandValue(dataHandler.getRpm());
		mMpgText.setText(String.format("%.1f MPG", dataHandler.getMpg()));
		mRpmText.setText(dataHandler.getRpm() + " RPM");
		mSpeedText.setText(String.format("%.0f KPH", dataHandler.getGpsSpeed()));
		mThrottleText.setText(dataHandler.getThrottle() + "%");
		mXAccelText.setText(String.format("%+.2fG X", dataHandler.getXAccel()));
		mYAccelText.setText(String.format("%+.2fG Y", dataHandler.getYAccel()));
	}

	private Gradient getGradient(String index) {
		int i;

		try {
			i = Integer.parseInt(index);
		} catch (NumberFormatException e) {
			i = 0;
		}

		if (i < 0 || i >= GRADIENTS.length){
			i = 0;
		}

		return new Gradient(GRADIENTS[i], null);
	}
}

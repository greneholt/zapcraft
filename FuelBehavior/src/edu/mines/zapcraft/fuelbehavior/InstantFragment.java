package edu.mines.zapcraft.FuelBehavior;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class InstantFragment extends Fragment implements Updatable {
	private static final String TAG = InstantFragment.class.getSimpleName();

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
            throw new ClassCastException(activity.toString() + " must implement DataHandlerProvider");
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

		mMpgGauge = (Gauge) view.findViewById(R.id.mpgGauge);
		mRpmGauge = (Gauge) view.findViewById(R.id.rpmGauge);
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
}

package edu.mines.zapcraft.FuelBehavior;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.text.DateFormat;

import java.io.File;

import java.util.Calendar;
import java.util.TimeZone;

import org.mapsforge.android.maps.MapContext;
import org.mapsforge.android.maps.MapController;
import org.mapsforge.android.maps.MapScaleBar;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.overlay.ArrayItemizedOverlay;
import org.mapsforge.android.maps.overlay.ItemizedOverlay;
import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.core.GeoPoint;


public class OldFragment extends Fragment implements Updatable {
	private static final String TAG = OldFragment.class.getSimpleName();

	private static final int MAP_VIEW_ID = 1;

	private MapController mMapController;

	private ArrayItemizedOverlay mItemizedOverlay;
	private OverlayItem mOverlayItem;

	private MapView mMapView;

	private PeriodicUpdater mUpdater;

	private DataProvider mDataProvider;

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

		Context mapFragmentContext = new MapFragmentContext(inflater.getContext());
		LayoutInflater mapInflater = inflater.cloneInContext(mapFragmentContext);

		View view = mapInflater.inflate(R.layout.old, container, false);

		mMapView.setClickable(true);
		mMapView.setBuiltInZoomControls(true);
		mMapView.setMapFile(new File("/sdcard/frontrange.map"));
		MapScaleBar mapScaleBar = mMapView.getMapScaleBar();
		mapScaleBar.setImperialUnits(true);
		mapScaleBar.setShowMapScaleBar(true);

		mMapController = mMapView.getController();
		mMapController.setZoom(15);

		mItemizedOverlay = new ArrayItemizedOverlay(null);
		mOverlayItem = new OverlayItem();
		mOverlayItem.setMarker(ItemizedOverlay.boundCenter(getResources().getDrawable(R.drawable.my_location)));
		mItemizedOverlay.addItem(this.mOverlayItem);
		mMapView.getOverlays().add(this.mItemizedOverlay);

		Button button1 = (Button) view.findViewById(R.id.button1);
		button1.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("gmt"));
				cal.setTimeInMillis(mDataProvider.getDataHandler().getTimeInMillis());
				handleStringMessage(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(cal.getTime()));
			}
		});

		Button button2 = (Button) view.findViewById(R.id.start_log_button);
		button2.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				if (!mDataProvider.getDataHandler().hasFix()) {
					handleStringMessage("No GPS fix");
					handleStringMessage("Tracking " + mDataProvider.getDataHandler().getSatelliteCount() + " satellites");
					return;
				}
				ContentValues values = new ContentValues();
				values.put("start_time", mDataProvider.getDataHandler().getTimeInMillis());
				mDataProvider.getDataLogger().setDriveId(mDataProvider.getDbAdapter().createDrive(values));
				mDataProvider.getDataLogger().start();
			}
		});

        return view;
	}

	@Override
	public void onResume() {
		super.onResume();

		mMapView.onResume();

		mUpdater.start();
	}

	@Override
	public void onPause() {
		super.onPause();

		mUpdater.stop();

		mMapView.onPause();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		mMapView.onDestroy();
	}

	@Override
	public void update() {
		updatePosition();
		setRpmGauge(mDataProvider.getDataHandler().getRpm());
		setKphGauge(mDataProvider.getDataHandler().getObd2Speed());
	}

	public void handleStringMessage(String message) {
		TextView textView = (TextView) getActivity().findViewById(R.id.output_text);
		textView.append(message + "\n");
	}

	public void setRpmGauge(float value) {
		Gauge gauge = (Gauge) getActivity().findViewById(R.id.gauge1);
		gauge.setHandValue(value);
	}

	public void setKphGauge(float value) {
		Gauge gauge = (Gauge) getActivity().findViewById(R.id.gauge2);
		gauge.setHandValue(value);
	}

	public void updatePosition() {
		GeoPoint point = new GeoPoint(mDataProvider.getDataHandler().getLatitude(), mDataProvider.getDataHandler().getLongitude());
		mOverlayItem.setPoint(point);
		mItemizedOverlay.requestRedraw();
		mMapController.setCenter(point);
	}

	private class MapFragmentContext extends ContextWrapper implements MapContext {
		public MapFragmentContext(Context context) {
			super(context);
		}

		@Override
	    public int getMapViewId() {
			return MAP_VIEW_ID;
	    }

		@Override
		public void registerMapView(MapView mapView) {
			mMapView = mapView;
		}
	}
}

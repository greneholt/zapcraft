package edu.mines.zapcraft.FuelBehavior;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import java.io.File;

import org.mapsforge.android.maps.MapContext;
import org.mapsforge.android.maps.MapController;
import org.mapsforge.android.maps.MapScaleBar;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.overlay.ArrayItemizedOverlay;
import org.mapsforge.android.maps.overlay.ItemizedOverlay;
import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.core.GeoPoint;


public class MapFragment extends Fragment implements Updatable {
	private static final String TAG = MapFragment.class.getSimpleName();

	private static final int MAP_VIEW_ID = 1;
	private static final String KEY_MAP_FOLLOW = "map_follow";

	private MapController mMapController;

	private ArrayItemizedOverlay mItemizedOverlay;
	private OverlayItem mOverlayItem;

	private MapView mMapView;

	private CheckBox mMapFollow;
	private TextView mGpsStatus;

	private PeriodicUpdater mUpdater;

	private DataProvider mDataProvider;

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

		mUpdater = new PeriodicUpdater(500, this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		// MapViews require a context that implements the MapContext interface, so
		// wrap the current context in a MapFragmentContext and use it when inflating
		// the view.
		Context mapFragmentContext = new MapFragmentContext(inflater.getContext());
		LayoutInflater mapInflater = inflater.cloneInContext(mapFragmentContext);

		View view = mapInflater.inflate(R.layout.map, container, false);

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

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

		mMapFollow = (CheckBox) view.findViewById(R.id.map_follow);
		mMapFollow.setChecked(sharedPrefs.getBoolean(KEY_MAP_FOLLOW, true));
		mGpsStatus = (TextView) view.findViewById(R.id.gps_status);

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

		Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
		editor.putBoolean(KEY_MAP_FOLLOW, mMapFollow.isChecked());
		editor.commit();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		mMapView.onDestroy();
	}

	@Override
	public void update() {
		updatePosition();
	}

	public void updatePosition() {
		DataHandler dataHandler = mDataProvider.getDataHandler();

		GeoPoint point = new GeoPoint(dataHandler.getLatitude(), dataHandler.getLongitude());
		mOverlayItem.setPoint(point);
		mItemizedOverlay.requestRedraw();

		String hasFix = "";
		if (!dataHandler.hasFix()) {
			hasFix = "No fix. ";
		}

		mGpsStatus.setText(hasFix + "Tracking " + mDataProvider.getDataHandler().getSatelliteCount() + " satellites");

		if (mMapFollow.isChecked()) {
			mMapController.setCenter(point);
		}
	}

	// Extends the current context to provide the methods necessary for a MapView to function
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

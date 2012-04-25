package edu.mines.zapcraft.FuelBehavior;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android_serialport_api.SerialPort;

import java.text.DateFormat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.util.Calendar;
import java.util.TimeZone;

import net.sf.marineapi.nmea.io.SentenceReader;

import org.mapsforge.android.maps.MapContext;
import org.mapsforge.android.maps.MapController;
import org.mapsforge.android.maps.MapScaleBar;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.overlay.ArrayItemizedOverlay;
import org.mapsforge.android.maps.overlay.ItemizedOverlay;
import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.core.GeoPoint;

public class FuelBehaviorActivity extends Activity implements MapContext, Updatable {
	private static final String TAG = FuelBehaviorActivity.class.getSimpleName();

	private SerialPort mGPSSerialPort;
	private SentenceReader mGPSSentenceReader;
	private InputStream mGPSInputStream;

	private DataHandler mDataHandler;

	private WakeLock mWakeLock;

	private MapController mMapController;
	private MapView mMapView;

	private ArrayItemizedOverlay mItemizedOverlay;
	private OverlayItem mOverlayItem;

	private DbAdapter mDbAdapter;

	private DataLogger mDataLogger;

	private PeriodicUpdater mUpdater;

	private boolean mControlsVisible = false;

	private int mLastMapViewId;

	private InputStream mArduinoInputStream;

	private ArduinoReader mArduinoReader;

	private SerialPort mArduinoSerialPort;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "AMV");

		mDbAdapter = new DbAdapter(this);
        mDbAdapter.open();

		mDataHandler = new DataHandler();
		mUpdater = new PeriodicUpdater(100, this);

		mDataLogger = new DataLogger(mDataHandler, mDbAdapter);

		showControls();
    }

    @Override
	public void onResume() {
		super.onResume();

		if (!mWakeLock.isHeld()) {
			mWakeLock.acquire();
		}

		startArduino();
		startGPS();

		//mDataLogger.start();

		if (mControlsVisible) {
			mUpdater.start();
		}

		if (mMapView != null) {
			mMapView.onResume();
		}
	}

    @Override
	public void onPause() {
		super.onPause();

		if (mWakeLock.isHeld()) {
			mWakeLock.release();
		}

		mDataLogger.stop();

		mUpdater.stop();

		stopArduino();
		stopGPS();

		if (mMapView != null) {
			mMapView.onPause();
		}
	}

    @Override
	public void onDestroy() {
    	super.onDestroy();

		mDbAdapter.close();

		if (mMapView != null) {
			mMapView.onDestroy();
		}
	}

	private void startArduino() {
		try {
			mArduinoSerialPort = new SerialPort(new File("/dev/ttyACM0"), 115200, 0);
			mArduinoInputStream = mArduinoSerialPort.getInputStream();
			mArduinoReader = new ArduinoReader(mArduinoInputStream);
            mArduinoReader.setListener(mDataHandler);
            mArduinoReader.start();
		} catch (SecurityException e) {
			DisplayError(R.string.error_no_arduino);
		} catch (IOException e) {
			DisplayError(R.string.error_arduino_ioexception);
		}
	}

	private void stopArduino() {
		try {
			if (mArduinoReader != null) {
				mArduinoReader.stop();
			}

			if (mArduinoSerialPort != null) {
				mArduinoSerialPort.close();
			}
		} finally {
			mArduinoReader = null;
			mArduinoSerialPort = null;
			mArduinoInputStream = null;
		}
	}

	private void startGPS() {
		try {
			mGPSSerialPort = new SerialPort(new File("/dev/ttyUSB0"), 4800, 0);
			mGPSInputStream = mGPSSerialPort.getInputStream();
			mGPSSentenceReader = new SentenceReader(mGPSInputStream);
            mGPSSentenceReader.addSentenceListener(mDataHandler);
            mGPSSentenceReader.start();
		} catch (SecurityException e) {
			DisplayError(R.string.error_no_gps);
		} catch (IOException e) {
			DisplayError(R.string.error_gps_ioexception);
		}
	}

	private void stopGPS() {
		try {
			if (mGPSSentenceReader != null) {
				mGPSSentenceReader.stop();
			}

			if (mGPSSerialPort != null) {
				mGPSSerialPort.close();
			}
		} finally {
			mGPSSentenceReader = null;
			mGPSSerialPort = null;
			mGPSInputStream = null;
		}
	}

	public void hideControls() {
		mControlsVisible = false;
		mUpdater.stop();

		if (mMapView != null) {
			mMapView.onDestroy();
		}

		mMapView = null;
	}

	public void showControls() {
		setContentView(R.layout.main);

		mMapView.setClickable(true);
		mMapView.setBuiltInZoomControls(true);
		mMapView.setMapFile(new File("/sdcard/frontrange.map"));
		MapScaleBar mapScaleBar = mMapView.getMapScaleBar();
		mapScaleBar.setImperialUnits(true);
		mapScaleBar.setShowMapScaleBar(true);

		mMapController = mMapView.getController();

		mItemizedOverlay = new ArrayItemizedOverlay(null);
		mOverlayItem = new OverlayItem();
		mOverlayItem.setMarker(ItemizedOverlay.boundCenter(getResources().getDrawable(R.drawable.my_location)));
		mItemizedOverlay.addItem(this.mOverlayItem);
		mMapView.getOverlays().add(this.mItemizedOverlay);

		Button button1 = (Button) findViewById(R.id.button1);
		button1.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("gmt"));
				cal.setTimeInMillis(mDataHandler.getTimeInMillis());
				handleStringMessage(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(cal.getTime()));
			}
		});

		Button button2 = (Button) findViewById(R.id.start_log_button);
		button2.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				if (!mDataHandler.hasFix()) {
					handleStringMessage("No GPS fix");
					handleStringMessage("Tracking " + mDataHandler.getSatelliteCount() + " satellites");
					return;
				}
				ContentValues values = new ContentValues();
				values.put("start_time", mDataHandler.getTimeInMillis());
				mDataLogger.setDriveId(mDbAdapter.createDrive(values));
				mDataLogger.start();
			}
		});

		mControlsVisible = true;
		mUpdater.start();
	}

	public void update() {
		updatePosition();
		setRpmGauge(mDataHandler.getRpm());
		setKphGauge(mDataHandler.getObd2Speed());
	}

	public void handleStringMessage(String message) {
		TextView textView = (TextView) findViewById(R.id.output_text);
		textView.append(message + "\n");
	}

	public void setRpmGauge(float value) {
		Gauge gauge = (Gauge) findViewById(R.id.gauge1);
		gauge.setHandValue(value);
	}

	public void setKphGauge(float value) {
		Gauge gauge = (Gauge) findViewById(R.id.gauge2);
		gauge.setHandValue(value);
	}

	public void updatePosition() {
		//handleStringMessage("Lat: " + mDataHandler.getLatitude() + " Lon: " + mDataHandler.getLongitude() + " Alt: " + mDataHandler.getAltitude() + " m");
		//handleStringMessage("Speed: " + mDataHandler.getGpsSpeed() + " Course: " + mDataHandler.getCourse());

		GeoPoint point = new GeoPoint(mDataHandler.getLatitude(), mDataHandler.getLongitude());
		mOverlayItem.setPoint(point);
		mItemizedOverlay.requestRedraw();
		mMapController.setCenter(point);
	}

	private void DisplayError(int resourceId) {
		AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setTitle("Error");
		b.setCancelable(false);
		b.setMessage(getResources().getString(resourceId));
		b.setPositiveButton("Quit", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	                FuelBehaviorActivity.this.finish();
	           }
		});
		b.show();
	}

	/**
     * Returns a unique MapView ID on each call.
     *
     * @return the new MapView ID.
     */
	@Override
    public int getMapViewId() {
		return ++mLastMapViewId;
    }

	@Override
	public void registerMapView(MapView mapView) {
		mMapView = mapView;
	}
}
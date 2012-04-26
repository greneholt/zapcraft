package edu.mines.zapcraft.FuelBehavior;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android_serialport_api.SerialPort;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import net.sf.marineapi.nmea.io.SentenceReader;

public class FuelBehaviorActivity extends Activity implements DataProvider, ViewChangeListener {
	private static final String TAG = FuelBehaviorActivity.class.getSimpleName();

	private WakeLock mWakeLock;

	private DbAdapter mDbAdapter;

	private DataHandler mDataHandler;
	private DataLogger mDataLogger;
	private boolean mLogging;

	private SerialPort mGPSSerialPort;
	private SentenceReader mGPSSentenceReader;
	private InputStream mGPSInputStream;

	private SerialPort mArduinoSerialPort;
	private InputStream mArduinoInputStream;
	private ArduinoReader mArduinoReader;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "AMV");

		mDbAdapter = new DbAdapter(this);
        mDbAdapter.open();

		mDataHandler = new DataHandler();

		mDataLogger = new DataLogger(mDataHandler, mDbAdapter);

		setContentView(R.layout.main);

		FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
	    fragmentTransaction.add(R.id.fragment_container, new InstantFragment());
	    fragmentTransaction.commit();
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
	}

    @Override
	public void onPause() {
		super.onPause();

		if (mWakeLock.isHeld()) {
			mWakeLock.release();
		}

		mDataLogger.stop();

		stopArduino();
		stopGPS();
	}

    @Override
	public void onDestroy() {
    	super.onDestroy();

		mDbAdapter.close();
	}

    @Override
    public DataHandler getDataHandler() {
    	return mDataHandler;
    }

    @Override
    public DataLogger getDataLogger() {
    	return mDataLogger;
    }

    @Override
    public DbAdapter getDbAdapter() {
    	return mDbAdapter;
    }

    @Override
    public void startLogging() {
    	if (!mLogging) {
    		mLogging = true;

    		ContentValues values = new ContentValues();
			values.put("start_time", mDataHandler.getTimeInMillis());
			mDataLogger.setDriveId(mDbAdapter.createDrive(values));
    		mDataLogger.start();
    	}
    }

	@Override
	public void stopLogging() {
		mLogging = false;
		mDataLogger.stop();
	}

	@Override
	public void resumeLogging() {
		if (mDataLogger.hasDriveId()) {
			mDataLogger.start();
		} else {
			displayMessage(getResources().getString(R.string.no_drive_started));
		}
	}

	@Override
	public void onViewChange(int view) {
		Fragment newFragment;

		switch(view) {
		default:
		case ViewChangeListener.INSTANT:
			newFragment = new InstantFragment();
			break;
		case ViewChangeListener.MAP:
			newFragment = new MapFragment();
			break;
		case ViewChangeListener.LOGS:
			newFragment = new LogsFragment();
			break;
		case ViewChangeListener.SETTINGS:
			newFragment = new SettingsFragment();
			break;
		}

		FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
	    fragmentTransaction.replace(R.id.fragment_container, newFragment);
	    fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
	    fragmentTransaction.commit();
	}

	public void displayError(String message) {
		AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setTitle("Error");
		b.setCancelable(false);
		b.setMessage(message);
		b.setPositiveButton("Quit", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	                FuelBehaviorActivity.this.finish();
	           }
		});
		b.show();
	}

	public void displayMessage(String message) {
		AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setTitle("Message");
		b.setMessage(message);
		b.setPositiveButton("Ok", null);
		b.show();
	}

	private void startArduino() {
		try {
			mArduinoSerialPort = new SerialPort(new File("/dev/ttyACM0"), 115200, 0);
			mArduinoInputStream = mArduinoSerialPort.getInputStream();
			mArduinoReader = new ArduinoReader(mArduinoInputStream);
            mArduinoReader.setListener(mDataHandler);
            mArduinoReader.start();
		} catch (SecurityException e) {
			displayError(getResources().getString(R.string.error_no_arduino));
		} catch (IOException e) {
			displayError(getResources().getString(R.string.error_arduino_ioexception));
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
			displayError(getResources().getString(R.string.error_no_gps));
		} catch (IOException e) {
			displayError(getResources().getString(R.string.error_gps_ioexception));
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
}
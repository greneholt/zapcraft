package edu.mines.zapcraft.FuelBehavior;

import android.content.ContentValues;


public class DataLogger implements Updatable {
	private static final String TAG = DataLogger.class.getSimpleName();

	private DataHandler mDataHandler;
	private PeriodicUpdater mUpdater;
	private DbAdapter mDbAdapter;
	private long mDriveId;

	public DataLogger(DataHandler dataHandler, DbAdapter dbAdapter) {
		mDataHandler = dataHandler;
		mDbAdapter = dbAdapter;

		mUpdater = new PeriodicUpdater(5000, this);
	}

	@Override
	public void update() {
		ContentValues values = new ContentValues();
		values.put("drive_id", mDriveId);
		values.put("latitude", mDataHandler.getLatitude());
		values.put("longitude", mDataHandler.getLongitude());
		if (mDataHandler.getAltitude() != Double.NaN) {
			values.put("altitude", mDataHandler.getAltitude());
		}
		values.put("gpsSpeed", mDataHandler.getGpsSpeed());
		values.put("course", mDataHandler.getCourse());
		values.put("mpg", mDataHandler.getMpg());
		values.put("rpm", mDataHandler.getRpm());
		values.put("obd2Speed", mDataHandler.getObd2Speed());
		values.put("xAccel", mDataHandler.getXAccel());
		values.put("yAccel", mDataHandler.getYAccel());
		values.put("zAccel", mDataHandler.getZAccel());
		mDbAdapter.createLog(values);
	}

	public void start() {
		mUpdater.start();
	}

	public void stop() {
		mUpdater.stop();
	}

	public void setDriveId(long driveId) {
		mDriveId = driveId;
	}

	public long getDriveId() {
		return mDriveId;
	}
}
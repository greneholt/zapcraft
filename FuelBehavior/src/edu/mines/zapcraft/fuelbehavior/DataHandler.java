package edu.mines.zapcraft.FuelBehavior;

import android.util.Log;
import net.sf.marineapi.nmea.event.SentenceEvent;
import net.sf.marineapi.nmea.event.SentenceListener;
import net.sf.marineapi.nmea.sentence.GGASentence;
import net.sf.marineapi.nmea.sentence.RMCSentence;
import net.sf.marineapi.nmea.sentence.Sentence;
import net.sf.marineapi.nmea.util.CompassPoint;
import net.sf.marineapi.nmea.util.Position;


public class DataHandler implements SentenceListener {
	private static final String TAG = DataHandler.class.getSimpleName();

	// GPS data
	private double mLatitude;
	private double mLongitude;
	private double mAltitude = Double.NaN; // altitude data may not always be available
	private double mGpsSpeed;
	private double mCourse;

	// Engine data
	private float mMpg;
	private float mRpm;
	private float mObd2Speed;

	// Accelerometer data
	private float mXAccel, mYAccel, mZAccel;

	public void setMpg(float mpg) {
		mMpg = mpg;
	}

	public void setRpm(float rpm) {
		mRpm = rpm;
	}

	public void setObd2Speed(float obd2Speed) {
		mObd2Speed = obd2Speed;
	}

	public synchronized void setAcceleration(float xAccel, float yAccel, float zAccel) {
		mXAccel = xAccel;
		mYAccel = yAccel;
		mZAccel = zAccel;
	}

	@Override
	public void readingPaused() {
	}

	@Override
	public void readingStarted() {
	}

	@Override
	public void readingStopped() {
	}

	@Override
	public synchronized void sentenceRead(SentenceEvent sentenceEvent) {
		Sentence sentence = sentenceEvent.getSentence();

		Log.d(TAG, "Received GPS sentence: " + sentence.toSentence());

		if (sentence instanceof GGASentence) {
			Position position = ((GGASentence) sentence).getPosition();
			setLatLon(position);
			mAltitude = position.getAltitude();
		} else if (sentence instanceof RMCSentence) {
			setLatLon(((RMCSentence) sentence).getPosition());
			mGpsSpeed = ((RMCSentence) sentence).getSpeed();
			mCourse = ((RMCSentence) sentence).getCourse();
		}
	}

	/**
	 * Returns the latitude in degrees.
	 *
	 * @return the latitude in degrees
	 */
	public double getLatitude() {
		return mLatitude;
	}

	/**
	 * Returns the longitude in degrees.
	 *
	 * @return the longitude in degrees
	 */
	public double getLongitude() {
		return mLongitude;
	}

	/**
	 * Returns the altitude in meters above sea level.
	 *
	 * @return the altitude in meters above sea level
	 */
	public double getAltitude() {
		return mAltitude;
	}

	/**
	 * Returns the speed in kilometers per hour as reported by the GPS.
	 *
	 * @return the speed in kilometers per hour as reported by the GPS
	 */
	public double getGpsSpeed() {
		return mGpsSpeed * 1.852;
	}

	/**
	 * Returns the current course in degrees from true north.
	 *
	 * @return the current course in degrees from true north
	 */
	public double getCourse() {
		return mCourse;
	}

	public double getMpg() {
		return mMpg;
	}

	public double getRpm() {
		return mRpm;
	}

	/**
	 * Returns the speed in kilometers per hour as reported by the engine.
	 *
	 * @return the speed in kilometers per hour as reported by the engine
	 */
	public double getObd2Speed() {
		return mObd2Speed;
	}

	public double getXAccel() {
		return mXAccel;
	}


	public double getYAccel() {
		return mYAccel;
	}


	public double getZAccel() {
		return mZAccel;
	}

	private void setLatLon(Position position) {
		mLatitude = position.getLatHemisphere() == CompassPoint.NORTH ? position.getLatitude() : -position.getLatitude();
		mLongitude = position.getLonHemisphere() == CompassPoint.EAST ? position.getLongitude() : -position.getLongitude();
	}
}

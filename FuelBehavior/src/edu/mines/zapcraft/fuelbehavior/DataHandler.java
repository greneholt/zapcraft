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

import android.util.Log;

import java.util.Calendar;
import java.util.TimeZone;

import net.sf.marineapi.nmea.event.SentenceEvent;
import net.sf.marineapi.nmea.event.SentenceListener;
import net.sf.marineapi.nmea.sentence.GGASentence;
import net.sf.marineapi.nmea.sentence.GSASentence;
import net.sf.marineapi.nmea.sentence.RMCSentence;
import net.sf.marineapi.nmea.sentence.Sentence;
import net.sf.marineapi.nmea.util.CompassPoint;
import net.sf.marineapi.nmea.util.Date;
import net.sf.marineapi.nmea.util.GpsFixStatus;
import net.sf.marineapi.nmea.util.Position;
import net.sf.marineapi.nmea.util.Time;


public class DataHandler implements SentenceListener, ArduinoListener {
	private static final String TAG = DataHandler.class.getSimpleName();

	private static final TimeZone UTC = TimeZone.getTimeZone("gmt");

	// GPS data
	private long mTimeOffset; // how many milliseconds the real GPS time is ahead of the system time
	private double mLatitude;
	private double mLongitude;
	private double mAltitude;
	private double mGpsSpeed;
	private double mCourse;
	private int mSatelliteCount;
	private boolean mHasFix = false;

	// Engine data
	private float mMpg;
	private int mRpm;
	private int mThrottle; // the throttle percentage out of 100
	private int mObd2Speed;

	// Accelerometer data
	private float mXAccel, mYAccel, mZAccel;

	public synchronized void readTime(Time time, Date date) {
		Calendar gps = Calendar.getInstance(UTC);
		gps.set(Calendar.YEAR, date.getYear());
		gps.set(Calendar.MONTH, date.getMonth());
		gps.set(Calendar.DAY_OF_MONTH, date.getDay());
		gps.set(Calendar.HOUR_OF_DAY, time.getHour());
		gps.set(Calendar.MINUTE, time.getMinutes());
		gps.set(Calendar.SECOND, (int)time.getSeconds());

		Calendar now = Calendar.getInstance(UTC);
		mTimeOffset = gps.getTimeInMillis() - now.getTimeInMillis();
	}

	public void readMpg(float mpg) {
		mMpg = mpg;
	}

	public void readRpm(int rpm) {
		mRpm = rpm;
	}

	public void readThrottle(int throttle) {
		mThrottle = throttle;
	}

	public void readSpeed(int obd2Speed) {
		mObd2Speed = obd2Speed;
	}

	public synchronized void readAcceleration(float xAccel, float yAccel, float zAccel) {
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

		Log.v(TAG, "Received GPS sentence: " + sentence.toSentence());

		if (sentence instanceof GGASentence) {
			Position position = ((GGASentence) sentence).getPosition();
			setLatLon(position);
			mAltitude = position.getAltitude();
			mSatelliteCount = ((GGASentence) sentence).getSatelliteCount();
		} else if (sentence instanceof RMCSentence) {
			RMCSentence rmcSentence = (RMCSentence) sentence;
			setLatLon(rmcSentence.getPosition());
			mGpsSpeed = rmcSentence.getSpeed();
			mCourse = rmcSentence.getCourse();
			readTime(rmcSentence.getTime(), rmcSentence.getDate());
		} else if (sentence instanceof GSASentence) {
			mHasFix = ((GSASentence) sentence).getFixStatus() != GpsFixStatus.GPS_NA;
		}
	}

	public long getTimeInMillis() {
		Calendar now = Calendar.getInstance(UTC);
		return now.getTimeInMillis() + mTimeOffset;
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

	public int getSatelliteCount() {
		return mSatelliteCount;
	}

	public boolean hasFix() {
		return mHasFix;
	}

	public float getMpg() {
		return mMpg;
	}

	public int getRpm() {
		return mRpm;
	}

	public int getThrottle() {
		return mThrottle;
	}

	/**
	 * Returns the speed in kilometers per hour as reported by the engine.
	 *
	 * @return the speed in kilometers per hour as reported by the engine
	 */
	public int getObd2Speed() {
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

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
	private double latitude;
	private double longitude;
	private double altitude;
	private double gpsSpeed;
	private double course;

	// Engine data
	private float mpg;
	private float rpm;
	private float obd2Speed;

	// Accelerometer data
	private float xAccel, yAccel, zAccel;

	public void setMpg(float mpg) {
		this.mpg = mpg;
	}

	public void setRpm(float rpm) {
		this.rpm = rpm;
	}

	public void setObd2Speed(float obd2Speed) {
		this.obd2Speed = obd2Speed;
	}

	public synchronized void setAcceleration(float xAccel, float yAccel, float zAccel) {
		this.xAccel = xAccel;
		this.yAccel = yAccel;
		this.zAccel = zAccel;
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
			altitude = position.getAltitude();
		} else if (sentence instanceof RMCSentence) {
			setLatLon(((RMCSentence) sentence).getPosition());
			gpsSpeed = ((RMCSentence) sentence).getSpeed();
			course = ((RMCSentence) sentence).getCourse();
		}
	}

	/**
	 * Returns the latitude in degrees.
	 *
	 * @return the latitude in degrees
	 */
	public double getLatitude() {
		return latitude;
	}

	/**
	 * Returns the longitude in degrees.
	 *
	 * @return the longitude in degrees
	 */
	public double getLongitude() {
		return longitude;
	}

	/**
	 * Returns the altitude in meters above sea level.
	 *
	 * @return the altitude in meters above sea level
	 */
	public double getAltitude() {
		return altitude;
	}

	/**
	 * Returns the speed in kilometers per hour as reported by the GPS.
	 *
	 * @return the speed in kilometers per hour as reported by the GPS
	 */
	public double getGpsSpeed() {
		return gpsSpeed * 1.852;
	}

	/**
	 * Returns the current course in degrees from true north.
	 *
	 * @return the current course in degrees from true north
	 */
	public double getCourse() {
		return course;
	}

	public double getMpg() {
		return mpg;
	}

	public double getRpm() {
		return rpm;
	}

	/**
	 * Returns the speed in kilometers per hour as reported by the engine.
	 *
	 * @return the speed in kilometers per hour as reported by the engine
	 */
	public double getObd2Speed() {
		return obd2Speed;
	}

	public double getXAccel() {
		return xAccel;
	}


	public double getYAccel() {
		return yAccel;
	}


	public double getZAccel() {
		return zAccel;
	}


	private void setLatLon(Position position) {
		latitude = position.getLatHemisphere() == CompassPoint.NORTH ? position.getLatitude() : -position.getLatitude();
		longitude = position.getLonHemisphere() == CompassPoint.EAST ? position.getLongitude() : -position.getLongitude();
	}
}

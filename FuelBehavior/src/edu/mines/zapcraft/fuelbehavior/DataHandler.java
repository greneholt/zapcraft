package edu.mines.zapcraft.FuelBehavior;

import net.sf.marineapi.nmea.event.SentenceEvent;
import net.sf.marineapi.nmea.event.SentenceListener;
import net.sf.marineapi.nmea.sentence.GGASentence;
import net.sf.marineapi.nmea.sentence.RMCSentence;
import net.sf.marineapi.nmea.sentence.Sentence;
import net.sf.marineapi.nmea.util.Position;


public class DataHandler implements SentenceListener {
	private static final String TAG = DataHandler.class.getSimpleName();

	private double latitude;
	private double longitude;
	private double altitude;
	private double speed;
	private double course;

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
	public void sentenceRead(SentenceEvent sentenceEvent) {
		Sentence sentence = sentenceEvent.getSentence();

		if (sentence instanceof GGASentence) {
			Position position = ((GGASentence) sentence).getPosition();
			latitude = position.getLatitude();
			longitude = position.getLongitude();
			altitude = position.getAltitude();
		} else if (sentence instanceof RMCSentence) {
			Position position = ((RMCSentence) sentence).getPosition();
			latitude = position.getLatitude();
			longitude = position.getLongitude();
			speed = ((RMCSentence) sentence).getSpeed();
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
	 * Returns the speed in kilometers per hour.
	 *
	 * @return the speed in kilometers per hour
	 */
	public double getSpeed() {
		return speed * 1.852;
	}

	/**
	 * Returns the current course in degrees from true north.
	 *
	 * @return the current course in degrees from true north
	 */
	public double getCourse() {
		return course;
	}
}

package edu.mines.zapcraft.FuelBehavior;

import net.sf.marineapi.nmea.event.SentenceEvent;
import net.sf.marineapi.nmea.event.SentenceListener;


public class DataHandler implements SentenceListener {

	private SentenceEvent lastSentence;

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
	public void sentenceRead(SentenceEvent sentence) {
		lastSentence = sentence;
	}

	public SentenceEvent getLastSentence() {
		return lastSentence;
	}
}

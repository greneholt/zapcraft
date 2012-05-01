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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class ArduinoReader {
	private static final String TAG = ArduinoReader.class.getSimpleName();

	private Thread mThread;
    private DataReader mReader;
    private ArduinoListener mListener;

    public ArduinoReader(InputStream source) {
        mReader = new DataReader(source);
    }

    public void setListener(ArduinoListener listener) {
    	mListener = listener;
    }

    public void start() {
    	if (mThread != null && mThread.isAlive() && mReader != null && mReader.isRunning()) {
            throw new IllegalStateException("Reader is already running");
        }

        mThread = new Thread(mReader);
        mThread.start();
    }

    public void stop() {
        if (mReader != null && mReader.isRunning()) {
            mReader.stop();
            mThread.interrupt();
        }
    }

    public void readLine(String line) {
    	Log.v(TAG, "Read line: " + line);

    	try {
	    	if (mListener != null) {
		    	String[] parts = line.split("\\*");

		    	if (parts.length != 2) {
		    		Log.d(TAG, "Missing checksum: " + line);
		    		return;
		    	}

		    	if (!validChecksum(parts[0], parts[1])) {
		    		Log.d(TAG, "Invalid checksum: " + line);
		    		return;
		    	}

	    		String[] tokens = parts[0].split(" ");

		    	if (tokens.length == 0) {
		    		Log.d(TAG, "Invalid line: " + line);
		    		return;
		    	}

		    	if (tokens[0].equals("RPM") && tokens.length >= 2) {
		    		mListener.readRpm(Integer.parseInt(tokens[1]));
		    	} else if (tokens[0].equals("MPG") && tokens.length >= 2) {
		    		mListener.readMpg(Float.parseFloat(tokens[1]));
		    	} else if (tokens[0].equals("THROTTLE") && tokens.length >= 2) {
		    		mListener.readThrottle(Integer.parseInt(tokens[1]));
		    	} else if (tokens[0].equals("SPEED") && tokens.length >= 2) {
		    		mListener.readSpeed(Integer.parseInt(tokens[1]));
		    	} else if (tokens[0].equals("ACCEL") && tokens.length >= 4) {
		    		mListener.readAcceleration(Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]), Float.parseFloat(tokens[3]));
		    	}
	    	}
    	} catch (NumberFormatException e) {
	    	Log.d(TAG, "Invalid line: " + line, e);
	    }
    }

    public boolean validChecksum(String input, String checksum) {
    	byte crc = Byte.parseByte(checksum, 16);

    	for (int i = 0; i < input.length(); i++) {
    		crc ^= input.charAt(i);
    	}

    	return crc == 0;
    }

    /**
     * Worker that reads the input stream and fires sentence events.
     */
    private class DataReader implements Runnable {
        private BufferedReader input;
        private volatile boolean isRunning = true;

        /**
         * Creates a new instance of StreamReader.
         *
         * @param source InputStream from where to read data.
         */
        public DataReader(InputStream source) {
            InputStreamReader isr = new InputStreamReader(source);
            input = new BufferedReader(isr);
        }

        /**
         * Tells if the reader is currently running, i.e. actively scanning the
         * input stream for new data.
         *
         * @return <code>true</code> if running, otherwise <code>false</code>.
         */
        public boolean isRunning() {
            return isRunning;
        }

        /**
         * Reads the input stream and fires SentenceEvents
         */
        public void run() {
            while (isRunning) {
                try {
	                String line = input.readLine();
	                readLine(line);
                } catch (IOException e) {
                	Log.e(TAG, "accessory read failed", e);
                	break;
                }
            }
        }

        /**
         * Stops the run loop.
         */
        public void stop() {
            isRunning = false;
        }
    }
}

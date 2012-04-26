/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.mines.zapcraft.FuelBehavior;


/**
 * Modified from the Android Gradient_Delegate class
 */
public class Gradient {
	private final static int GRADIENT_SIZE = 255;

	private final int mColors[];
	private final float mPositions[];
	private int mGradient[];

	/**
     * Creates the base shader and do some basic test on the parameters.
     *
     * @param colors The colors to be distributed along the gradient line
     * @param positions May be null. The relative positions [0..1] of each
     *            corresponding color in the colors array. If this is null, the
     *            the colors are distributed evenly along the gradient line.
     */
    public Gradient(int colors[], float positions[]) {
        if (colors.length < 2) {
            throw new IllegalArgumentException("needs >= 2 number of colors");
        }

        if (positions != null && colors.length != positions.length) {
            throw new IllegalArgumentException("color and position arrays must be of equal length");
        }

        if (positions == null) {
            float spacing = 1.f / (colors.length - 1);
            positions = new float[colors.length];
            positions[0] = 0.f;
            positions[colors.length-1] = 1.f;
            for (int i = 1; i < colors.length - 1 ; i++) {
                positions[i] = spacing * i;
            }
        }

        mColors = colors;
        mPositions = positions;

        precomputeGradientColors();
    }

    /**
     * Returns the color based on the position in the gradient.
     */
    public int getGradientColor(float position) {
		if (position < 0.f || position > 1f) {
			throw new IllegalArgumentException("position must be between 0 and 1");
		}

		int index = (int)((position * GRADIENT_SIZE) + .5);
	    return mGradient[index];
	}

	/**
     * Pre-computes the colors for the gradient. This must be called once before any call
     * to {@link #getGradientColor(float)}
     */
    protected void precomputeGradientColors() {
        if (mGradient == null) {
            // actually create an array with an extra size, so that we can really go
            // from 0 to SIZE (100%), or currentPos in the loop below will never equal 1.0
            mGradient = new int[GRADIENT_SIZE+1];

            int prevPos = 0;
            int nextPos = 1;
            for (int i  = 0 ; i <= GRADIENT_SIZE ; i++) {
                // compute current position
                float currentPos = (float)i/GRADIENT_SIZE;
                while (currentPos > mPositions[nextPos]) {
                    prevPos = nextPos++;
                }

                float percent = (currentPos - mPositions[prevPos]) /
                        (mPositions[nextPos] - mPositions[prevPos]);

                mGradient[i] = computeColor(mColors[prevPos], mColors[nextPos], percent);
            }
        }
    }

	/**
     * Returns the color between c1, and c2, based on the percent of the distance
     * between c1 and c2.
     */
    protected int computeColor(int c1, int c2, float percent) {
        int a = computeChannel((c1 >> 24) & 0xFF, (c2 >> 24) & 0xFF, percent);
        int r = computeChannel((c1 >> 16) & 0xFF, (c2 >> 16) & 0xFF, percent);
        int g = computeChannel((c1 >>  8) & 0xFF, (c2 >>  8) & 0xFF, percent);
        int b = computeChannel((c1      ) & 0xFF, (c2      ) & 0xFF, percent);
        return a << 24 | r << 16 | g << 8 | b;
    }

    /**
     * Returns the channel value between 2 values based on the percent of the distance between
     * the 2 values..
     */
    protected int computeChannel(int c1, int c2, float percent) {
        return c1 + (int)((percent * (c2-c1)) + .5);
    }
}

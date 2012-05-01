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

import android.os.Handler;
import android.os.Message;

/**
 * Updates the provided object at the specified interval.
 *
 * Based on code from http://www.salientia.ca/dave/2010/12/periodically-updating-an-android-ui/
 *
 * @author Connor McKay
 */
public class PeriodicUpdater extends Handler {
    public final static int MSG_UPDATE = 0;

    private int mRefreshInterval; // the refresh interval in milliseconds

    private Updatable mUpdatable;

    public PeriodicUpdater(int refreshInterval, Updatable updatable)
    {
        super();
        mRefreshInterval = refreshInterval;
        mUpdatable = updatable;
    }

    @Override
    public void handleMessage(Message msg)
    {
        super.handleMessage(msg);

        switch (msg.what)
        {
        case MSG_UPDATE:
            mUpdatable.update();
            sendEmptyMessageDelayed(MSG_UPDATE, mRefreshInterval);
            break;

        default:
            break;
        }
    }

    public void start() {
    	sendEmptyMessage(MSG_UPDATE);
    }

    public void stop() {
    	removeMessages(MSG_UPDATE);
    }
}

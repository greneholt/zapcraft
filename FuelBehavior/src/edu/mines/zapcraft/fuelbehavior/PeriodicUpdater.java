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

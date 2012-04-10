package edu.mines.zapcraft.FuelBehavior;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android_serialport_api.SerialPort;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.sf.marineapi.nmea.io.SentenceReader;

public class FuelBehaviorActivity extends Activity {
	private static final String TAG = FuelBehaviorActivity.class.getSimpleName();

	private static final String ACTION_USB_PERMISSION = "edu.mines.zapcraft.FuelBehavior.action.USB_PERMISSION";

	private UsbManager mUsbManager;
	private PendingIntent mPermissionIntent;
	private boolean mPermissionRequestPending;

	private UsbAccessory mAccessory;
	private ParcelFileDescriptor mADKFileDescriptor;
	private InputStream mADKInputStream;
	private OutputStream mADKOutputStream;

	private SerialPort mSerialPort;

	private SentenceReader mSentenceReader;
	private InputStream mGPSInputStream;
	private DataHandler mDataHandler;

	private WakeLock mWakeLock;

	private static final int MESSAGE_STRING = 1;
	private static final int MESSAGE_RPM = 2;

	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbAccessory accessory = UsbManager.getAccessory(intent);
					if (intent.getBooleanExtra(
							UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						openAccessory(accessory);
					} else {
						Log.d(TAG, "permission denied for accessory "
								+ accessory);
					}
					mPermissionRequestPending = false;
				}
			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				UsbAccessory accessory = UsbManager.getAccessory(intent);
				if (accessory != null && accessory.equals(mAccessory)) {
					closeAccessory();
				}
			}
		}
	};

    private class ADKThread extends Thread {
    	@Override
    	public void run() {
			int ret = 0;
			byte[] buffer = new byte[16384];
			int i;

			while (ret >= 0) {
				try {
					ret = mADKInputStream.read(buffer);
				} catch (IOException e) {
					Message m = Message.obtain(mHandler, MESSAGE_STRING);
					Log.e(TAG, "read failed", e);
					m.obj = "IOException";
					mHandler.sendMessage(m);
					break;
				}

				i = 0;
				while (i < ret) {
					int len = ret - i;

					switch (buffer[i]) {
					case 0x1:
						if (len >= 4 && validateChecksum(buffer[i+1], buffer[i+2], buffer[i+3])) {
							Message m = Message.obtain(mHandler, MESSAGE_RPM);
							m.obj = new Integer(composeInt(buffer[i+1], buffer[i+2]));
							mHandler.sendMessage(m);
						}
						i += 4;
						break;

					default:
						Log.d(TAG, "unknown msg: " + buffer[i]);
						i = len;
						break;
					}
				}
			}
		}
    }

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "AMV");

        // setup receivers for USB notifications
        mUsbManager = UsbManager.getInstance(this);
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mUsbReceiver, filter);

		mDataHandler = new DataHandler();

		showControls();// should be hideControls, but I need to test the interface
		//hideControls();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
	public void onResume() {
		super.onResume();

		if (!mWakeLock.isHeld()) {
			mWakeLock.acquire();
		}

		if (mADKInputStream == null || mADKOutputStream == null) {
			UsbAccessory[] accessories = mUsbManager.getAccessoryList();
			UsbAccessory accessory = (accessories == null ? null : accessories[0]);
			if (accessory != null) {
				if (mUsbManager.hasPermission(accessory)) {
					openAccessory(accessory);
				} else {
					synchronized (mUsbReceiver) {
						if (!mPermissionRequestPending) {
							mUsbManager.requestPermission(accessory,
									mPermissionIntent);
							mPermissionRequestPending = true;
						}
					}
				}
			} else {
				Log.d(TAG, "mAccessory is null");
			}
		}

		startGPS();
	}

	@Override
	public void onPause() {
		super.onPause();

		if (mWakeLock.isHeld()) {
			mWakeLock.release();
		}

		closeAccessory();
		stopGPS();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		unregisterReceiver(mUsbReceiver);
	}

	private void openAccessory(UsbAccessory accessory) {
		mADKFileDescriptor = mUsbManager.openAccessory(accessory);
		if (mADKFileDescriptor != null) {
			mAccessory = accessory;
			FileDescriptor fd = mADKFileDescriptor.getFileDescriptor();
			mADKInputStream = new FileInputStream(fd);
			mADKOutputStream = new FileOutputStream(fd);
			Thread thread = new ADKThread();
			thread.start();
			Log.d(TAG, "accessory opened");
			showControls();
		} else {
			Log.d(TAG, "accessory open fail");
		}
	}

	private void closeAccessory() {
		hideControls();

		try {
			if (mADKFileDescriptor != null) {
				mADKFileDescriptor.close();
			}
		} catch (IOException e) {
		} finally {
			mADKFileDescriptor = null;
			mADKInputStream = null;
			mADKOutputStream = null;
			mAccessory = null;
		}
	}

	private void startGPS() {
		try {
			mSerialPort = new SerialPort(new File("/dev/ttyUSB0"), 4800, 0);
			mGPSInputStream = mSerialPort.getInputStream();
			mSentenceReader = new SentenceReader(mGPSInputStream);
            mSentenceReader.addSentenceListener(mDataHandler);
            mSentenceReader.start();
		} catch (SecurityException e) {
			DisplayError(R.string.error_gps_security);
		} catch (IOException e) {
			DisplayError(R.string.error_gps_ioexception);
		}
	}

	private void stopGPS() {
		try {
			if (mSentenceReader != null) {
				mSentenceReader.stop();
			}

			if (mSerialPort != null) {
				mSerialPort.close();
			}
		} finally {
			mSentenceReader = null;
			mSerialPort = null;
			mGPSInputStream = null;
		}
	}

	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STRING:
				String s = (String) msg.obj;
				handleStringMessage(s);
				break;
			case MESSAGE_RPM:
				int v = (Integer) msg.obj;
				setGauge(v);
			}
		}
	};

	public void hideControls() {
		setContentView(R.layout.no_device);
	}

	public void showControls() {
		setContentView(R.layout.main);

		Button button1 = (Button) findViewById(R.id.button1);
		button1.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				handleStringMessage("Lat: " + mDataHandler.getLatitude() + " Lon: " + mDataHandler.getLongitude() + " Alt: " + mDataHandler.getAltitude() + " m");
				handleStringMessage("Speed: " + mDataHandler.getSpeed() + " Course: " + mDataHandler.getCourse());
			}
		});

		Button button2 = (Button) findViewById(R.id.button2);
		button2.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				setGauge(30);
			}
		});
	}

	public void sendCommand(byte[] command) {
		if (mADKOutputStream != null) {
			try {
				mADKOutputStream.write(command);
			} catch (IOException e) {
				Log.e(TAG, "write failed", e);
			}
		}
	}

	public void handleStringMessage(String message) {
		TextView textView = (TextView) findViewById(R.id.output_text);
		textView.append(message + "\n");
	}

	public void setGauge(float value) {
		Gauge gauge = (Gauge) findViewById(R.id.gauge1);
		gauge.setHandValue(value);
	}

	private void DisplayError(int resourceId) {
		AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setTitle("Error");
		b.setMessage(resourceId);
		b.show();
	}

	private int composeInt(byte high, byte low) {
		int val = ((int) high & 0xff) << 8;
		val |= (int) low & 0xff;
		return val;
	}

	private boolean validateChecksum(byte high, byte low, byte checksum) {
		return (high ^ low) == checksum;
	}
}
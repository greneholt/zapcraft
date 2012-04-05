package edu.mines.zapcraft.FuelBehavior;

import android.app.Activity;
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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FuelBehaviorActivity extends Activity implements Runnable {
	private static final String TAG = FuelBehaviorActivity.class.getSimpleName();

	private static final String ACTION_USB_PERMISSION = "edu.mines.zapcraft.FuelBehavior.action.USB_PERMISSION";

	private UsbManager mUsbManager;
	private PendingIntent mPermissionIntent;
	private boolean mPermissionRequestPending;

	UsbAccessory mAccessory;
	ParcelFileDescriptor mFileDescriptor;
	FileInputStream mInputStream;
	FileOutputStream mOutputStream;

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

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUsbManager = UsbManager.getInstance(this);
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mUsbReceiver, filter);

		//showControls();// should be hideControls, but I need to test the interface
		hideControls();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
	public void onResume() {
		super.onResume();

		if (mInputStream != null && mOutputStream != null) {
			return;
		}

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

	@Override
	public void onPause() {
		super.onPause();
		closeAccessory();
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(mUsbReceiver);
		super.onDestroy();
	}

	private void openAccessory(UsbAccessory accessory) {
		mFileDescriptor = mUsbManager.openAccessory(accessory);
		if (mFileDescriptor != null) {
			mAccessory = accessory;
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
			Thread thread = new Thread(null, this, "FuelBehavior");
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
			if (mFileDescriptor != null) {
				mFileDescriptor.close();
			}
		} catch (IOException e) {
		} finally {
			mFileDescriptor = null;
			mInputStream = null;
			mOutputStream = null;
			mAccessory = null;
		}
	}

	public void run() {
		int ret = 0;
		byte[] buffer = new byte[16384];
		int i;

		while (ret >= 0) {
			try {
				ret = mInputStream.read(buffer);
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
				byte[] buffer = new byte[1];
				buffer[0] = 48;
				handleStringMessage("Sent command");
				sendCommand(buffer);
				setGauge(70);
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
		if (mOutputStream != null) {
			try {
				mOutputStream.write(command);
			} catch (IOException e) {
				Log.e(TAG, "write failed", e);
			}
		}
	}

	public void handleStringMessage(String message) {
		TextView textView = (TextView) findViewById(R.id.output_text);
		textView.setText(message);
	}

	public void setGauge(float value) {
		Gauge gauge = (Gauge) findViewById(R.id.gauge1);
		gauge.setHandValue(value);
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
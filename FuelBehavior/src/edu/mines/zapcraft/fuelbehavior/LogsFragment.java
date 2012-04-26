package edu.mines.zapcraft.FuelBehavior;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import java.text.DateFormat;

import java.util.Calendar;
import java.util.TimeZone;

public class LogsFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String TAG = LogsFragment.class.getSimpleName();

	private DataProvider mDataProvider;

	private CursorAdapter mAdapter;

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mDataProvider = (DataProvider) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement DataHandlerProvider");
        }
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		View view = inflater.inflate(R.layout.logs, container, false);

		Button buttonStart = (Button) view.findViewById(R.id.start_logging_button);
		buttonStart.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				if (!mDataProvider.getDataHandler().hasFix()) {
					mDataProvider.displayMessage("No GPS fix\nTracking " + mDataProvider.getDataHandler().getSatelliteCount() + " satellites");
					return;
				}

				mDataProvider.startLogging();
			}
		});

		Button buttonStop = (Button) view.findViewById(R.id.stop_logging_button);
		buttonStop.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				mDataProvider.stopLogging();
			}
		});

		Button buttonResume = (Button) view.findViewById(R.id.resume_logging_button);
		buttonResume.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				mDataProvider.resumeLogging();
			}
		});

		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //setEmptyText(getResources().getString(R.string.no_drives));

        Cursor drivesCursor = mDataProvider.getDbAdapter().fetchAllDrives();

        mAdapter = new DrivesCursorAdapter(getActivity(), R.layout.drive_row, drivesCursor, 0);

        setListAdapter(mAdapter);

        //setListShown(false);

        getLoaderManager().initLoader(0, null, this);
	}

	private class DrivesCursorAdapter extends ResourceCursorAdapter {
		private Calendar mCal = Calendar.getInstance(TimeZone.getTimeZone("gmt"));

		public DrivesCursorAdapter(Context context, int layout, Cursor cursor, int flags) {
			super(context, layout, cursor, flags);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			TextView dateTime = (TextView) view.findViewById(R.id.date_time);

			mCal.setTimeInMillis(cursor.getLong(1));
			dateTime.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(mCal.getTime()));
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new AsyncTaskLoader<Cursor>(getActivity()) {
			@Override
			public Cursor loadInBackground() {
				return mDataProvider.getDbAdapter().fetchAllDrives();
			}
		};
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);
/*
		if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }*/
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}
}
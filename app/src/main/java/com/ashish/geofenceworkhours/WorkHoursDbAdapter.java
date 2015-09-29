package com.ashish.geofenceworkhours;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Calendar;

public class WorkHoursDbAdapter
{
	private static final int    MILLIS_PER_SECOND            = 1000;
	private static final int    SECONDS_PER_MINUTE           = 60;
	private static final int    MINUTES_PER_HOUR             = 60;
	private static final String DATABASE                     = "WorkHours.db";
	private static final int    DATABASE_VERSION             = 2;
	private static final String WORK_HOURS_TABLE             = "WorkHours";
	private static final String WORK_HOURS_TABLE_ID          = "id";
	private static final String WORK_HOURS_TABLE_IS_EXIT     = "isExit";
	private static final String WORK_HOURS_TABLE_TIMESTAMP   = "timeInMillis";
	private static final String WORK_HOURS_TABLE_TOTAL_HOURS = "totalHours";

	private static final String SQL_CREATE_TABLE =
		"CREATE TABLE " + WORK_HOURS_TABLE + " (" +
		WORK_HOURS_TABLE_ID + " INTEGER PRIMARY KEY, " +
		WORK_HOURS_TABLE_IS_EXIT + " INTEGER, " +
		WORK_HOURS_TABLE_TIMESTAMP + " INTEGER, " +
		WORK_HOURS_TABLE_TOTAL_HOURS + " REAL" + " )";

	private static final String SQL_DELETE_TABLE =
		"DROP TABLE IF EXISTS " + WORK_HOURS_TABLE;

	private Context           mContext;
	private WorkHoursDbHelper mDbHelper;
	private SQLiteDatabase    mDb;

	public WorkHoursDbAdapter(Context ctx)
	{
		this.mContext = ctx;
	}

	public WorkHoursDbAdapter open() throws SQLException
	{
		mDbHelper = new WorkHoursDbHelper(mContext);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	public void close()
	{
		mDbHelper.close();
	}

	public void insert(boolean isExit)
	{
		long currentTime = Calendar.getInstance().getTimeInMillis();
		ContentValues values = new ContentValues();
		values.put(WORK_HOURS_TABLE_IS_EXIT, isExit);
		values.put(WORK_HOURS_TABLE_TIMESTAMP, currentTime);

		//sometimes GPS can be inaccurate, want to make sure enters/exists are at least consistent
		boolean hasMatchingEvent = false;
		long enterTime = 0;
		Cursor cursor =
			mDb.query(
				WORK_HOURS_TABLE,
				new String[] {WORK_HOURS_TABLE_IS_EXIT, WORK_HOURS_TABLE_TOTAL_HOURS, WORK_HOURS_TABLE_TIMESTAMP},
				null, null,
				null, null,
				WORK_HOURS_TABLE_TIMESTAMP + " DESC",
				"1");

		if (cursor.getCount() > 0)
		{
			while (cursor.moveToNext())
			{
				hasMatchingEvent = cursor.getInt(cursor.getColumnIndex(WORK_HOURS_TABLE_IS_EXIT)) == (isExit ? 0 : 1);
				enterTime = cursor.getLong(cursor.getColumnIndex(WORK_HOURS_TABLE_TIMESTAMP));
			}
		}
		else
		{
			// there is no data, so we are fine if this is enter event
			if (!isExit)
			{
				hasMatchingEvent = true;
			}
		}

		cursor.close();

		values.put(WORK_HOURS_TABLE_TOTAL_HOURS, isExit ? millisToHours(currentTime - enterTime) : 0);

		if (hasMatchingEvent)
		{
			mDb.insert(WORK_HOURS_TABLE, null, values);
		}
	}

	public ArrayList<WorkHoursItem> getAllWorkHours()
	{
		Cursor cursor =
			mDb.query(
				WORK_HOURS_TABLE,
				new String[] {WORK_HOURS_TABLE_ID, WORK_HOURS_TABLE_IS_EXIT, WORK_HOURS_TABLE_TIMESTAMP,
				              WORK_HOURS_TABLE_TOTAL_HOURS},
				null, null,
				null, null,
				WORK_HOURS_TABLE_TIMESTAMP + " DESC");

		if (cursor.getCount() > 0)
		{
			ArrayList<WorkHoursItem> list = new ArrayList<>(cursor.getCount());
			while (cursor.moveToNext())
			{
				int id = cursor.getInt(cursor.getColumnIndex(WORK_HOURS_TABLE_ID));
				boolean isExit = cursor.getInt(cursor.getColumnIndex(WORK_HOURS_TABLE_IS_EXIT)) == 1;
				long millis = cursor.getLong(cursor.getColumnIndex(WORK_HOURS_TABLE_TIMESTAMP));
				float total = cursor.getFloat(cursor.getColumnIndex(WORK_HOURS_TABLE_TOTAL_HOURS));
				list.add(new WorkHoursItem(id, isExit, millis, total));
			}

			cursor.close();

			//if last activity was an entrance, calculate the current time spent at work
			WorkHoursItem lastEvent = list.get(0);
			if (!lastEvent.mIsExit)
			{
				lastEvent.mTotal =
					millisToHours(Calendar.getInstance().getTimeInMillis() - lastEvent.mCalendar.getTimeInMillis());
			}

			return list;
		}
		else
		{
			cursor.close();
			return new ArrayList<>();
		}
	}

	public void clear()
	{
		mDb.execSQL(SQL_DELETE_TABLE);
		mDbHelper.onCreate(mDb);
	}

	private float millisToHours(long millis)
	{
		return (float)millis / MILLIS_PER_SECOND / SECONDS_PER_MINUTE / MINUTES_PER_HOUR;
	}

	private static class WorkHoursDbHelper extends SQLiteOpenHelper
	{
		public WorkHoursDbHelper(Context context)
		{
			super(context, DATABASE, null, DATABASE_VERSION);
		}

		public void onCreate(SQLiteDatabase db)
		{
			db.execSQL(SQL_CREATE_TABLE);
		}

		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			db.execSQL(SQL_DELETE_TABLE);
			onCreate(db);
		}

		public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			onUpgrade(db, oldVersion, newVersion);
		}
	}
}
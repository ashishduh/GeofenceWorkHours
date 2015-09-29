package com.ashish.geofenceworkhours;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class WorkHoursItem
{
	public int      mId;
	public boolean  mIsExit;
	public Calendar mCalendar;
	public float    mTotal;

	public WorkHoursItem()
	{
		mId = -1;
		mIsExit = false;
		mCalendar = Calendar.getInstance();
		mTotal = 0;
	}

	public WorkHoursItem(WorkHoursItem item)
	{
		mId = item.mId;
		mIsExit = item.mIsExit;
		mCalendar = item.mCalendar;
		mTotal = item.mTotal;
	}

	public WorkHoursItem(int id, boolean isExit, long unixMillis, float total)
	{
		mId = id;
		mIsExit = isExit;
		mCalendar = Calendar.getInstance();
		mCalendar.setTimeInMillis(unixMillis);
		mTotal = total;
	}

	public String getTimeFormatted()
	{
		return new SimpleDateFormat("h:mm aa", Locale.getDefault()).format(mCalendar.getTime());
	}
}

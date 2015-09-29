package com.ashish.geofenceworkhours;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

public class WorkHoursListAdapter extends ArrayAdapter<WorkHoursItem> implements StickyListHeadersAdapter
{
	private final Context             mContext;
	private final LayoutInflater      mInflater;
	private final List<WorkHoursItem> mItems;

	public WorkHoursListAdapter(Context context, ArrayList<WorkHoursItem> list)
	{
		super(context, 0, list);

		mContext = context;
		mInflater = LayoutInflater.from(context);
		mItems = list;
	}

	@Override
	public int getCount()
	{
		return mItems.size();
	}

	@Override
	public WorkHoursItem getItem(int position)
	{
		return mItems.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		ViewHolder holder;
		if (convertView == null)
		{
			holder = new ViewHolder();
			convertView = mInflater.inflate(R.layout.row_work_hours_detail, null);
			holder.action = (TextView)convertView.findViewById(R.id.action);
			holder.timestamp = (TextView)convertView.findViewById(R.id.timestamp);
			holder.total = (TextView)convertView.findViewById(R.id.total);
			convertView.setTag(holder);
		}
		else
		{
			holder = (ViewHolder)convertView.getTag();
		}

		WorkHoursItem item = getItem(position);
		if (item != null)
		{
			holder.action.setText(item.mIsExit ? mContext.getString(R.string.exited) : mContext.getString(R.string.entered));
			holder.timestamp.setText(item.getTimeFormatted());
			holder.total.setText(item.mTotal == 0 ? "" : String.format("%.2f hrs", item.mTotal));
		}

		return convertView;
	}

	@Override
	public View getHeaderView(int position, View convertView, ViewGroup parent)
	{
		HeaderViewHolder holder;
		if (convertView == null)
		{
			holder = new HeaderViewHolder();
			convertView = mInflater.inflate(R.layout.row_work_hours_header, parent, false);
			holder.date = (TextView)convertView.findViewById(R.id.date);
			holder.total = (TextView)convertView.findViewById(R.id.total);
			convertView.setTag(holder);
		}
		else
		{
			holder = (HeaderViewHolder)convertView.getTag();
		}

		long id = getHeaderId(position);
		int day = (int)(id / 10000);
		int year = (int)(id % 10000);

		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.DAY_OF_YEAR, day);
		holder.date.setText(SimpleDateFormat.getDateInstance(DateFormat.SHORT).format(cal.getTime()));
		holder.total.setText(String.format("%.2f hrs", getDailyTotal(day, year)));

		return convertView;
	}

	@Override
	public long getHeaderId(int position)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(mItems.get(position).mCalendar.getTimeInMillis());
		int currentDay = cal.get(Calendar.DAY_OF_YEAR);
		int currentYear = cal.get(Calendar.YEAR);

		// Header Id is an integer in <DAY_OF_YEAR><YEAR> format
		// ex. 155th day of year 2012 = 1552012
		return currentDay * 10000 + currentYear;
	}

	private float getDailyTotal(int day, int year)
	{
		float total = 0;
		for (WorkHoursItem item : mItems)
		{
			if (item.mCalendar.get(Calendar.DAY_OF_YEAR) == day && item.mCalendar.get(Calendar.YEAR) == year)
			{
				total += item.mTotal;
			}
		}

		return total;
	}

	public class HeaderViewHolder
	{
		TextView date;
		TextView total;
	}

	class ViewHolder
	{
		TextView action;
		TextView timestamp;
		TextView total;
	}
}
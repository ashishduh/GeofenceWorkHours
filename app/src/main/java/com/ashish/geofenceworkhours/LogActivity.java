package com.ashish.geofenceworkhours;

import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.location.Geofence;

import java.util.ArrayList;

import se.emilsjolander.stickylistheaders.ExpandableStickyListHeadersListView;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public class LogActivity extends AppCompatActivity implements ResetDialog.OnResetConfirmedListener
{
	private WorkHoursListAdapter                mAdapter;
	private ExpandableStickyListHeadersListView mListView;
	private BroadcastReceiver                   mReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_log);

		mListView = (ExpandableStickyListHeadersListView)findViewById(R.id.list);
		mAdapter = new WorkHoursListAdapter(this, new ArrayList<WorkHoursItem>());
		mListView.setAdapter(mAdapter);
		mListView.setOnHeaderClickListener(
			new StickyListHeadersListView.OnHeaderClickListener()
			{
				@Override
				public void onHeaderClick(StickyListHeadersListView listView, View view, int i, long headerId, boolean b)
				{
					if (mListView.isHeaderCollapsed(headerId))
					{
						mListView.expand(headerId);
					}
					else
					{
						mListView.collapse(headerId);
					}
				}
			});

		mReceiver = new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				populateList();
			}
		};
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		LocalBroadcastManager.getInstance(this)
			.registerReceiver(mReceiver, new IntentFilter(GeofenceBroadcastReceiver.ACTION_GEOFENCE_UPDATE));
		populateList();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.log, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.reset:
				DialogFragment dialog = new ResetDialog();
				dialog.show(getFragmentManager(), "reset");
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onPause()
	{
		super.onPause();

		LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
	}

	@Override
	public void onResetConfirmed(boolean confirmed)
	{
		if (confirmed)
		{
			Intent selectIntent = new Intent(this, SelectLocationActivity.class);
			selectIntent.putExtra(SelectLocationActivity.EXTRA_RESET, true);
			startActivity(selectIntent);
			finish();
		}
	}

	private void populateList()
	{
		WorkHoursDbAdapter dbAdapter = new WorkHoursDbAdapter(this);
		ArrayList<WorkHoursItem> list = dbAdapter.open().getAllWorkHours();
		dbAdapter.close();

		mAdapter.clear();
		mAdapter.addAll(list);
		mAdapter.notifyDataSetChanged();
	}
}

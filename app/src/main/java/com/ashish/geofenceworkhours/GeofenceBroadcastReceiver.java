package com.ashish.geofenceworkhours;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.LocationServices;

public class GeofenceBroadcastReceiver extends BroadcastReceiver
{
	public final static String ACTION_GEOFENCE_UPDATE = BuildConfig.APPLICATION_ID + ".ACTION_GEOFENCE_UPDATE";

	@Override
	public void onReceive(Context context, Intent intent)
	{
		if (!PreferenceManager.getDefaultSharedPreferences(context).contains(SelectLocationActivity.PREFERENCE_LOCATION_SET))
		{
			return;
		}

		GeofencingEvent event = GeofencingEvent.fromIntent(intent);
		if (event.hasError())
		{
			Log.e(this.getClass().getName(), GeofenceStatusCodes.getStatusCodeString(event.getErrorCode()));
			return;
		}

		int transition = event.getGeofenceTransition();
		if (transition == Geofence.GEOFENCE_TRANSITION_ENTER || transition == Geofence.GEOFENCE_TRANSITION_EXIT)
		{
			WorkHoursDbAdapter db = new WorkHoursDbAdapter(context);
			db.open();
			db.insert(transition == Geofence.GEOFENCE_TRANSITION_EXIT);
			db.close();

			LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION_GEOFENCE_UPDATE));
		}
	}
}

package com.ashish.geofenceworkhours;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class SelectLocationActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
	GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status>
{
	public final static  String EXTRA_RESET                    = "reset";
	public final static  String PREFERENCE_LOCATION_SET        = "locationSet";
	private final static int    REQUEST_CODE_PLAY_SERVICES_FIX = 1;
	private final static int    REQUEST_CODE_ENABLE_LOCATION   = 2;
	private final        String EXTRA_MARKER                   = "marker";
    private final        int    GEOFENCE_RADIUS                = 20;
	private GoogleMap         mMap;
	private MarkerOptions     mMarker;
	private Button            mConfirm;
	private SharedPreferences mPreferences;
	private GoogleApiClient   mGoogleApiClient;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		Bundle extras = getIntent().getExtras();
		boolean needsReset = false;
		if (extras != null)
		{
			if (extras.getBoolean(EXTRA_RESET))
			{
				needsReset = true;
			}
		}

		if (mPreferences.getBoolean(PREFERENCE_LOCATION_SET, false) && !needsReset)
		{
			goToLog();
		}
		else
		{
			setContentView(R.layout.activity_select_location);
			mConfirm = (Button)findViewById(R.id.buttonConfirm);
			mConfirm.setOnClickListener(
				new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						if (mGoogleApiClient.isConnected())
						{
							addGeofence();
						}
					}
				});

			if (savedInstanceState != null)
			{
				mMarker = savedInstanceState.getParcelable(EXTRA_MARKER);
			}
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		if (isGooglePlayServicesAvailable() && isLocationFullyAvailable(true))
		{
			connectToGoogle();
			setUpMap();
		}
	}

	private boolean isGooglePlayServicesAvailable()
	{
		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (status != ConnectionResult.SUCCESS)
		{
			if (GooglePlayServicesUtil.isUserRecoverableError(status))
			{
				GooglePlayServicesUtil.getErrorDialog(status, this, REQUEST_CODE_PLAY_SERVICES_FIX).show();
			}
			else
			{
				Toast.makeText(this, R.string.device_not_supported, Toast.LENGTH_LONG).show();
				finish();
			}

			return false;
		}

		return true;
	}

	private boolean isLocationFullyAvailable(boolean prompt)
	{
		LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setAltitudeRequired(false);
		criteria.setBearingRequired(false);
		criteria.setCostAllowed(false);
		criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);

		String provider = locationManager.getBestProvider(criteria, false);

		if (provider == null || !locationManager.isProviderEnabled(provider))
		{
			if (prompt)
			{
				Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				if (locationIntent.resolveActivity(getPackageManager()) != null)
				{
					Toast.makeText(this, R.string.enable_location_message, Toast.LENGTH_SHORT).show();
					startActivityForResult(locationIntent, REQUEST_CODE_ENABLE_LOCATION);
				}
			}

			return false;
		}

		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch (requestCode)
		{
			case REQUEST_CODE_PLAY_SERVICES_FIX:
				if (resultCode == RESULT_CANCELED)
				{
					Toast.makeText(this, R.string.requires_google_play_services, Toast.LENGTH_SHORT).show();
					finish();
				}
				return;
			case REQUEST_CODE_ENABLE_LOCATION:
				if (resultCode == RESULT_CANCELED)
				{
					if (!isLocationFullyAvailable(false))
					{
						Toast.makeText(this, R.string.location_must_be_enabled, Toast.LENGTH_SHORT).show();
						finish();
					}
				}
				return;
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onSaveInstanceState(Bundle savedInstanceState)
	{
		savedInstanceState.putParcelable(EXTRA_MARKER, mMarker);

		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	protected void onStop()
	{
		if (mGoogleApiClient != null)
		{
			mGoogleApiClient.disconnect();
		}

		super.onStop();
	}

	private void setUpMap()
	{
		if (mMap == null)
		{
			((SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map))
				.getMapAsync(
                        new OnMapReadyCallback() {
                            @Override
                            public void onMapReady(GoogleMap googleMap) {
                                mMap = googleMap;
                                mMap.setOnMapClickListener(
                                        new GoogleMap.OnMapClickListener() {
                                            @Override
                                            public void onMapClick(LatLng latLng) {
                                                setNewPosition(latLng);
                                            }
                                        });

                                if (mMarker != null) {
                                    setNewPosition(mMarker.getPosition());
                                }
                            }
                        });
		}
	}

	private void setLastLocation()
	{
		Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

		if (location != null)
		{
			setNewPosition(new LatLng(location.getLatitude(), location.getLongitude()));
		}
	}

	private void setNewPosition(LatLng latLng)
	{
		mMarker = new MarkerOptions().position(latLng).title(getString(R.string.work));
		mMap.clear();
		mMap.addMarker(mMarker);

		float currentZoom = mMap.getCameraPosition().zoom;
		mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, currentZoom > 15 ? currentZoom : 15));
		mMap.addCircle(
			new CircleOptions()
				.center(latLng)
				.radius(GEOFENCE_RADIUS)
				.strokeWidth(getResources().getDisplayMetrics().densityDpi / 80f) // 2 DP
				.strokeColor(Color.argb(200, 0, 180, 220))
				.fillColor(Color.argb(50, 0, 180, 200)));

		mConfirm.setVisibility(View.VISIBLE);
	}

	private void connectToGoogle()
	{
		if (mGoogleApiClient == null || !mGoogleApiClient.isConnected())
		{
			mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addApi(LocationServices.API)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.build();
			mGoogleApiClient.connect();
		}
	}

	@Override
	public void onConnected(Bundle bundle)
	{
		if (getIntent().getExtras() != null && getIntent().getExtras().containsKey(EXTRA_RESET))
		{
			removeGeofence();
		}

		//only initialize marker if it's null
		if (mMap != null && mMarker == null)
		{
			setLastLocation();
		}
	}

	@Override
	public void onConnectionSuspended(int i)
	{
		Log.e(this.getClass().getName(), String.format("GoogleApiClient connection suspended, ec=%d", 1));
	}

	@Override
	public void onConnectionFailed(ConnectionResult result)
	{
		Log.e(this.getClass().getName(), String.format("GoogleApiClient connection failed, ec=%d", result.getErrorCode()));
	}

	@Override
	public void onResult(Status status)
	{
		if (!mPreferences.contains(PREFERENCE_LOCATION_SET))
		{
			if (status.isSuccess())
			{
				mPreferences.edit().putBoolean(PREFERENCE_LOCATION_SET, true).commit();

				Toast.makeText(this, R.string.beginning, Toast.LENGTH_SHORT).show();
				goToLog();
			}
			else
			{
				Toast.makeText(this, R.string.failure, Toast.LENGTH_SHORT).show();
			}
		}
		else
		{
			WorkHoursDbAdapter db = new WorkHoursDbAdapter(this);
			db.open();
			db.clear();
			db.close();

			getIntent().removeExtra(EXTRA_RESET);
			mPreferences.edit().remove(PREFERENCE_LOCATION_SET).commit();
			Toast.makeText(this, R.string.ending, Toast.LENGTH_SHORT).show();
		}
	}

	private PendingIntent getGeofencePendingIntent()
	{
		Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);
		return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}

	private void addGeofence()
	{
		Geofence geofence = new Geofence.Builder()
			.setRequestId("geofence") //only need 1 geofence per device
			.setCircularRegion(mMarker.getPosition().latitude, mMarker.getPosition().longitude, GEOFENCE_RADIUS)
			.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
			.setExpirationDuration(Geofence.NEVER_EXPIRE)
			.build();

		GeofencingRequest request = new GeofencingRequest.Builder()
			.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
			.addGeofence(geofence)
			.build();

		LocationServices.GeofencingApi
			.addGeofences(mGoogleApiClient, request, getGeofencePendingIntent())
			.setResultCallback(this);
	}

	private void removeGeofence()
	{
		LocationServices.GeofencingApi
			.removeGeofences(mGoogleApiClient, getGeofencePendingIntent())
			.setResultCallback(this);
	}

	private void goToLog()
	{
		Intent logIntent = new Intent(this, LogActivity.class);
		startActivity(logIntent);
		finish();
	}
}

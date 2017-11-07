package com.akinn.timebots;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;

//import com.google.android.gms.location.LocationServices;

public class MyLocationService extends Service {
    final static String TAG = MyLocationService.class.getSimpleName();
    LocationManager mLocationManager;
    LocationListener mLocationListener;

    public MyLocationService() {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                sendLocationInfo(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        };

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        mLocationManager = (LocationManager) getApplicationContext()
                .getSystemService(Context.LOCATION_SERVICE);
        String locProvider = mLocationManager.getBestProvider(criteria, true);

        Location location = null;
        try {
            Log.d(TAG, "Provider = " + locProvider);
            mLocationManager.requestLocationUpdates(locProvider,
                    5000, 1, mLocationListener);
            Log.d(TAG, "get last known location");
            location = mLocationManager.getLastKnownLocation(Context.LOCATION_SERVICE);
            if (location != null) {
                Log.d(TAG, String.format("listener request (%f, %f)", location.getLatitude(), location.getLongitude()));
            }
            sendLocationInfo(location);
        } catch (SecurityException e) {
            Log.d(TAG, e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        if (mLocationManager != null) {
            mLocationManager.removeUpdates(mLocationListener);
        }
    }

    private void sendLocationInfo(Location location) {
        Log.d(TAG, "sendLocationInfo");
        if (location != null) {
            Intent intent = new Intent(getResources().getString(R.string.location_broadcast_name));
            intent.putExtra("LONGITUDE", location.getLongitude());
            intent.putExtra("LATITUDE", location.getLatitude());
            Log.d(TAG, String.format("sendLocationInfo (%f,%f)",
                    location.getLatitude(), location.getLongitude()));
            sendBroadcast(intent);
        }
    }
}

package com.osmfogmap.locationtrack;


import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Granularity;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.osmdroid.util.GeoPoint;


public class BackgroundService extends Service {

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private static LocationUpdateCallback callback ;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
    public static void registerCallback(LocationUpdateCallback locationUpdateCallback) {
        callback = locationUpdateCallback;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    Log.d("current loc: ", "Latitude: " + latitude + ", Longitude: " + longitude);
                    GeoPoint newLoc = new GeoPoint(latitude, longitude);
                    Log.d("accuracy", String.valueOf(location.getAccuracy()));
                    if(location.getAccuracy()<50)
                        callback.onLocationUpdated(newLoc);

                }
            }

        };
        startLocationUpdates();
        return START_STICKY; // Újraindul, ha az Android leállítja
    }

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);



        startForeground(1, createNotification());

    }



    private Notification createNotification() {
        NotificationChannel channel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel("location_service", "GPS Tracking", NotificationManager.IMPORTANCE_HIGH);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }


        return new NotificationCompat.Builder(this, "location_service")
                .setOngoing(true)
                .setContentTitle("Helymeghatározás aktív")
                .setContentText("Az alkalmazás figyeli a helyzeted")
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .build();
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,2000)
                .setMinUpdateDistanceMeters(30)
                .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}


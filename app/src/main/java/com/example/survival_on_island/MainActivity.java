package com.example.survival_on_island;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.microsoft.maps.GPSMapLocationProvider;
import com.microsoft.maps.MapRenderMode;
import com.microsoft.maps.MapUserInterfaceOptions;
import com.microsoft.maps.MapUserLocation;
import com.microsoft.maps.MapUserLocationTrackingState;
import com.microsoft.maps.MapView;
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "LOG_FOR_MAIN_ACTIVITY";
    private static final int REQUEST_LOCATION_PERMISSION = 2345;

    private MapView mMapView;
    FrameLayout bingMapLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bingMapLayout = findViewById(R.id.map_view); //

        // initiate MapView instance
        mMapView = new MapView(this, MapRenderMode.VECTOR);  // or use MapRenderMode.RASTER for 2D map
        mMapView.setCredentialsKey(BuildConfig.BING_MAP_CREDENTIALS_KEY); // set bing map api key
        MapUserInterfaceOptions uiOptions = mMapView.getUserInterfaceOptions();
        uiOptions.setUserLocationButtonVisible(true);


        bingMapLayout.addView(mMapView); // add Bing map view to frame
        mMapView.onCreate(savedInstanceState);
        showCurrentLocation();

        requestLocationPermission();

    }

    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,@NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestLocationPermission();
                } else {
                    Toast.makeText(this,
                            R.string.location_permission_denied,
                            Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    // request permission at runtime
    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]
                            {Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            Log.d(TAG, "getLocation: permissions granted");
        }
    }

    private void showCurrentLocation() {
        MapUserLocation userLocation = mMapView.getUserLocation();

        MapUserLocationTrackingState userLocationTrackingState = userLocation.startTracking(new GPSMapLocationProvider.Builder(getApplicationContext()).build());
        if (userLocationTrackingState == MapUserLocationTrackingState.PERMISSION_DENIED)
        {
            requestLocationPermission();
        } else if (userLocationTrackingState == MapUserLocationTrackingState.READY)
        {

            userLocation.setVisible(true);
        } else if (userLocationTrackingState == MapUserLocationTrackingState.DISABLED)
        {
            showSnackMessage("GPS provider is not enable");
        }
    }

    private void showSnackMessage(String message){
        Snackbar.make(bingMapLayout, message, Snackbar.LENGTH_LONG)
                .setAction("CLOSE", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                    }
                })
                .setActionTextColor(getResources().getColor(android.R.color.holo_red_light ))
                .show();
    }

}
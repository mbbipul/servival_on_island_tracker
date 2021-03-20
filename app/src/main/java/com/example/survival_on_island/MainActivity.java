package com.example.survival_on_island;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.survival_on_island.ui.Home.DownLoadImageTask;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.common.SignInButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.GeoPoint;
import com.microsoft.maps.GPSMapLocationProvider;
import com.microsoft.maps.Geopoint;
import com.microsoft.maps.MapElementLayer;
import com.microsoft.maps.MapHoldingEventArgs;
import com.microsoft.maps.MapIcon;
import com.microsoft.maps.MapImage;
import com.microsoft.maps.MapRenderMode;
import com.microsoft.maps.MapTappedEventArgs;
import com.microsoft.maps.MapUserInterfaceOptions;
import com.microsoft.maps.MapUserLocation;
import com.microsoft.maps.MapUserLocationTrackingState;
import com.microsoft.maps.MapView;
import com.microsoft.maps.OnMapHoldingListener;
import com.microsoft.maps.OnMapTappedListener;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
       OnMapHoldingListener,
        View.OnClickListener {

    private static final String TAG = "LOG_FOR_MAIN_ACTIVITY";
    private static final int REQUEST_LOCATION_PERMISSION = 2345;
    private static final int RC_SIGN_IN = 1245 ;

    private MapView mMapView;
    FrameLayout bingMapLayout;
    SignInButton signInButton;
    LinearLayout navProfileInfo ;
    TextView userFullNameV;
    TextView userEmaillV;
    ImageView profileImageView;
    MenuItem logoutMenu;
    private MapElementLayer mPinLayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        Menu navigationMenu = navigationView.getMenu();
        logoutMenu = navigationMenu.findItem(R.id.logout);

        signInButton = headerView.findViewById(R.id.sign_in_button);
        navProfileInfo = headerView.findViewById(R.id.profile_info);
        userFullNameV = headerView.findViewById(R.id.user_full_name);
        userEmaillV = headerView.findViewById(R.id.user_email);
        profileImageView = headerView.findViewById(R.id.profile_image);

        signInButton.setOnClickListener(this);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        drawer.addDrawerListener(toggle);

        toggle.syncState();

        bingMapLayout = findViewById(R.id.map_view); //

        // initiate MapView instance
        mMapView = new MapView(this, MapRenderMode.VECTOR);  // or use MapRenderMode.RASTER for 2D map
        mMapView.setCredentialsKey(BuildConfig.BING_MAP_CREDENTIALS_KEY); // set bing map api key
        MapUserInterfaceOptions uiOptions = mMapView.getUserInterfaceOptions();
        uiOptions.setUserLocationButtonVisible(true);
        mMapView.addOnMapHoldingListener(this);
        mPinLayer = new MapElementLayer();
        mMapView.getLayers().add(mPinLayer);

        bingMapLayout.addView(mMapView); // add Bing map view to frame
        mMapView.onCreate(savedInstanceState);

    }

    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
        updateUI();
        showCurrentLocation();
        requestLocationPermission();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Toast.makeText(this,"Login check on act",Toast.LENGTH_LONG);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            Toast.makeText(this,"Login check on act",Toast.LENGTH_LONG);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                updateUI();

                // ...
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
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

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        switch (id){
            case R.id.logout:
                FirebaseAuth.getInstance().signOut();
                updateUI();
                break;
        }
        return true;
    }

    @Override
    public boolean onMapHolding(MapHoldingEventArgs mapHoldingEventArgs) {
        addPin(mapHoldingEventArgs.location," Pin ");
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
        }
    }

    private void signIn() {
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.GoogleBuilder().build());
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN);

    }

    private void updateUI() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null){
            signInButton.setVisibility(View.GONE);
            navProfileInfo.setVisibility(View.VISIBLE);
            logoutMenu.setVisible(true);

            new DownLoadImageTask(profileImageView).execute(String.valueOf(currentUser.getPhotoUrl()));

            userFullNameV.setText(currentUser.getDisplayName());
            userEmaillV.setText(currentUser.getEmail());
            return;
        }

        signInButton.setVisibility(View.VISIBLE);
        navProfileInfo.setVisibility(View.GONE);
        logoutMenu.setVisible(false);
        profileImageView.setImageResource(R.drawable.ic_user_icon);

    }

    private void addPin(Geopoint location,String title){
        Bitmap pinBitmap = drawableToBitmap(getDrawable(R.drawable.ic_baseline_add_location_alt_24));

        MapIcon pushpin = new MapIcon();
        pushpin.setLocation(location);
        pushpin.setTitle(title);
        pushpin.setImage(new MapImage(pinBitmap));

        mPinLayer.getElements().add(pushpin);
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

}
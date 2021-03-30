package com.example.survival_on_island;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.survival_on_island.Models.Pin;
import com.example.survival_on_island.Models.User;
import com.example.survival_on_island.Models.UserLocation;
import com.example.survival_on_island.ui.Home.DownLoadImageTask;
import com.example.survival_on_island.ui.Home.PinCreateActivity;
import com.example.survival_on_island.utils.ImageUtils;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.annotations.Nullable;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.microsoft.maps.GPSMapLocationProvider;
import com.microsoft.maps.Geopoint;
import com.microsoft.maps.MapElementLayer;
import com.microsoft.maps.MapFlyout;
import com.microsoft.maps.MapHoldingEventArgs;
import com.microsoft.maps.MapIcon;
import com.microsoft.maps.MapImage;
import com.microsoft.maps.MapLocationData;
import com.microsoft.maps.MapRenderMode;
import com.microsoft.maps.MapUserInterfaceOptions;
import com.microsoft.maps.MapUserLocation;
import com.microsoft.maps.MapUserLocationTrackingState;
import com.microsoft.maps.MapView;
import com.microsoft.maps.OnMapHoldingListener;

import org.w3c.dom.Comment;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import static com.example.survival_on_island.utils.FirebaseUtils.DATABASE_USER_REFS;
import static com.example.survival_on_island.utils.FirebaseUtils.FIRESTORE_PIN_REFS;
import static com.example.survival_on_island.utils.FirebaseUtils.FIRESTORE_USERS_REFS;
import static com.example.survival_on_island.utils.FirebaseUtils.getCurrentUser;
import static com.example.survival_on_island.utils.FirebaseUtils.isUserLoggedIn;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
       OnMapHoldingListener,
        View.OnClickListener {

    private static final String TAG = "LOG_FOR_MAIN_ACTIVITY";
    private static final int REQUEST_LOCATION_PERMISSION = 2345;
    private static final int RC_SIGN_IN = 1245 ;

    FirebaseFirestore db;
    private DatabaseReference mDatabase;

    private MapView mMapView;
    FrameLayout bingMapLayout;
    SignInButton signInButton;
    LinearLayout navProfileInfo ;
    TextView userFullNameV;
    TextView userEmaillV;
    ImageView profileImageView;
    MenuItem logoutMenu;
    private MapElementLayer mPinLayer;
    private MapElementLayer usersLocationLayer;

    HashMap<String, MapElementLayer> userLocationHashMap;
    ChildEventListener userLocationsEventListener;
    FusedLocationProviderClient client;
    LocationCallback userLocationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        db = FirebaseFirestore.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference().child(DATABASE_USER_REFS);
        userLocationHashMap = new HashMap<String, MapElementLayer>();

        mPinLayer = new MapElementLayer();
        usersLocationLayer = new MapElementLayer();

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        bingMapLayout = findViewById(R.id.map_view); //
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

        // initiate MapView instance
        mMapView = new MapView(this, MapRenderMode.VECTOR);  // or use MapRenderMode.RASTER for 2D map
        mMapView.setCredentialsKey(BuildConfig.BING_MAP_CREDENTIALS_KEY); // set bing map api key
        MapUserInterfaceOptions uiOptions = mMapView.getUserInterfaceOptions();
        uiOptions.setUserLocationButtonVisible(true);
        mMapView.addOnMapHoldingListener(this);

        mMapView.getLayers().add(usersLocationLayer);
        mMapView.getLayers().add(mPinLayer);

        client = LocationServices.getFusedLocationProviderClient(this);
        userLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {

                Location location = locationResult.getLastLocation();
                UserLocation userLocation = new UserLocation();
                userLocation.setLatitude(location.getLatitude());
                userLocation.setLongitude(location.getLongitude());
                if (location != null) {
                    Log.d(TAG, "location update " + location);
                    //  Toast.makeText(TrackerService.this, location.toString(), Toast.LENGTH_SHORT).show();
                    mDatabase.child(getCurrentUser().getUid()).setValue(userLocation);
                }
            }
        };

        userLocationsEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildAdded:" + dataSnapshot);
                UserLocation userLocationGeoPoin = dataSnapshot.getValue(UserLocation.class);

                updateUsersLayerBycheck(dataSnapshot.getKey(),userLocationGeoPoin);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildChanged:" + dataSnapshot);
                UserLocation userLocationGeoPoin = dataSnapshot.getValue(UserLocation.class);

                updateUsersLayerBycheck(dataSnapshot.getKey(),userLocationGeoPoin);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onChildRemoved:" + dataSnapshot.getKey());
                removeUserLocationPin(dataSnapshot.getKey());
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildMoved:" + dataSnapshot.getKey());



                // ...
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "postComments:onCancelled", databaseError.toException());
                Toast.makeText(MainActivity.this, "Failed to load comments.",
                        Toast.LENGTH_SHORT).show();
            }
        };

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

                User user = new User();
                user.setUserEmail(getCurrentUser().getEmail());
                user.setUserFullname(getCurrentUser().getDisplayName());
                user.setUserProfileImageUrl(String.valueOf(getCurrentUser().getPhotoUrl()));

                // Successfully signed in
                db.collection(FIRESTORE_USERS_REFS).document(getCurrentUser().getUid()).set(user);
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

    private void requestLocationUpdates() {
        LocationRequest request = new LocationRequest();
        request.setInterval(3000);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            // Request location updates and when an update is
            // received, store the location in Firebase
            client.requestLocationUpdates(request,userLocationCallback , null);
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
        FirebaseUser currentUser = getCurrentUser();
        if (currentUser != null){
            signInButton.setVisibility(View.GONE);
            navProfileInfo.setVisibility(View.VISIBLE);
            logoutMenu.setVisible(true);

            new DownLoadImageTask(profileImageView).execute(String.valueOf(currentUser.getPhotoUrl()));

            userFullNameV.setText(currentUser.getDisplayName());
            userEmaillV.setText(currentUser.getEmail());
            showAllPin();

            mDatabase.addChildEventListener(userLocationsEventListener);
            requestLocationUpdates();
            return;
        }

        mDatabase.removeEventListener(userLocationsEventListener);
        client.removeLocationUpdates(userLocationCallback);
        signInButton.setVisibility(View.VISIBLE);
        navProfileInfo.setVisibility(View.GONE);
        logoutMenu.setVisible(false);
        profileImageView.setImageResource(R.drawable.ic_user_icon);
        mMapView.getLayers().clear();

    }

    private void removeUserLocationPin(String key){
        if(userLocationHashMap.containsKey(key)){
            userLocationHashMap.get(key).getElements().clear();
            userLocationHashMap.remove(key);
        }
    }

    private void updateUsersLayerBycheck(String key,UserLocation userLocation){
        if(userLocationHashMap.containsKey(key)){
            updateUserLocationPin(userLocationHashMap.get(key),key,userLocation);
        }else{
            MapElementLayer userMapElementLayer = new MapElementLayer();
            mMapView.getLayers().add(userMapElementLayer);
            userLocationHashMap.put(key,userMapElementLayer);
            updateUserLocationPin(userMapElementLayer,key,userLocation);
        }

    }

    private void updateUserLocationPin(MapElementLayer layer,String key,UserLocation userLocation){
        DocumentReference docRef = db.collection(FIRESTORE_USERS_REFS).document(key);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());

                        try {
                            User user = document.toObject(User.class);
                            Bitmap userImage = new DownLoadImageTask(null)
                                    .execute(user.getUserProfileImageUrl()).get();
                            Bitmap customPinBitmap = getUsersLocationPinBitmap(userImage);
                            MapIcon pushpin = new MapIcon();

                            if(user != null){
                                Geopoint location = new Geopoint(userLocation.getLatitude(),userLocation.getLongitude());

                                pushpin.setLocation(location);
                                pushpin.setImage(new MapImage(customPinBitmap));
                                MapFlyout flyout = new MapFlyout();
                                flyout.setTitle(user.getUserFullname());
//                                    flyout.setDescription("pin.getDetails()");
                                pushpin.setFlyout(flyout);
                                layer.getElements().clear();

                                layer.getElements().add(pushpin);

                            }


                        } catch (ExecutionException executionException) {
                            executionException.printStackTrace();
                        } catch (InterruptedException interruptedException) {
                            interruptedException.printStackTrace();
                        }
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });

    }

    private void showAllPin(){
        db.collection(FIRESTORE_PIN_REFS).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }

                for (QueryDocumentSnapshot doc : value) {
                    Pin pin = doc.toObject(Pin.class);
                    if(pin != null){
                            DocumentReference docRef = db.collection(FIRESTORE_USERS_REFS).document(pin.getCreatedBY());
                            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot document = task.getResult();
                                        if (document.exists()) {
                                            Log.d(TAG, "DocumentSnapshot data: " + document.getData());


                                            try {
                                                Bitmap pinBitmap = new DownLoadImageTask(null)
                                                        .execute(pin.getImageUrl()).get();


                                                User user = document.toObject(User.class);

                                                Bitmap userImage = new DownLoadImageTask(null)
                                                        .execute(user.getUserProfileImageUrl()).get();
                                                Bitmap customPinBitmap = getMarkerBitmapFromView(pinBitmap,
                                                        pin.getTitle(),user.getUserFullname(),pin.getCreatedAt(),userImage);

                                                MapIcon pushpin = new MapIcon();
                                                Geopoint location = new Geopoint(pin.getLatitude(),pin.getLongitude());

                                                pushpin.setLocation(location);
                                                pushpin.setImage(new MapImage(customPinBitmap));

                                                MapFlyout flyout = new MapFlyout();
                                                flyout.setTitle(pin.getTitle());
                                                flyout.setDescription(pin.getDetails());
                                                pushpin.setFlyout(flyout);

                                                mPinLayer.getElements().add(pushpin);
                                            } catch (ExecutionException executionException) {
                                                executionException.printStackTrace();
                                            } catch (InterruptedException interruptedException) {
                                                interruptedException.printStackTrace();
                                            }

                                        } else {
                                            Log.d(TAG, "No such document");
                                        }
                                    } else {
                                        Log.d(TAG, "get failed with ", task.getException());
                                    }
                                }
                            });

                    }
                }
            }
        });
    }

    private void addPin(Geopoint location,String title){
        if(isUserLoggedIn()){
            Intent pinCreateIntent = new Intent(this, PinCreateActivity.class);
            pinCreateIntent.putExtra("latitude",location.getPosition().getLatitude());
            pinCreateIntent.putExtra("longitude",location.getPosition().getLongitude());

            startActivity(pinCreateIntent);
        }else {
            showSnackMessage("Log in to Create a Pin");
        }
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

    private Bitmap getUsersLocationPinBitmap(Bitmap userImage){
        View customMarkerView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.user_location_pin, null);

        ImageView userImageView = customMarkerView.findViewById(R.id.user_image);
        userImageView.setImageBitmap(ImageUtils.getclip(userImage));

        customMarkerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        customMarkerView.layout(0, 0, customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight());
        customMarkerView.buildDrawingCache();
        Bitmap returnedBitmap = Bitmap.createBitmap(customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        canvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC_IN);
        Drawable drawable = customMarkerView.getBackground();
        if (drawable != null)
            drawable.draw(canvas);
        customMarkerView.draw(canvas);
        return returnedBitmap;
    }

    private Bitmap getMarkerBitmapFromView(Bitmap _pinImage,String _pinTitle,String _userName ,
                                            String createdAt,Bitmap userProfileImage
                                           ) {

        View customMarkerView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.custom_push_pin, null);

        ImageView pinImage = customMarkerView.findViewById(R.id.pin_image);
        ImageView pinProfileImage = customMarkerView.findViewById(R.id.profile_image);
        TextView userName = customMarkerView.findViewById(R.id.user_full_name);
        TextView pinTitle = customMarkerView.findViewById(R.id.pin_title);
        TextView createdDate = customMarkerView.findViewById(R.id.pin_created_date);

        pinImage.setImageBitmap(ImageUtils.getclip(_pinImage));
        pinProfileImage.setImageBitmap(ImageUtils.getclip(userProfileImage));
        userName.setText(_userName);
        pinTitle.setText(_pinTitle);
        createdDate.setText(createdAt.substring(0,20));

        customMarkerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        customMarkerView.layout(0, 0, customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight());
        customMarkerView.buildDrawingCache();
        Bitmap returnedBitmap = Bitmap.createBitmap(customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        canvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC_IN);
        Drawable drawable = customMarkerView.getBackground();
        if (drawable != null)
            drawable.draw(canvas);
        customMarkerView.draw(canvas);
        return returnedBitmap;
    }

    private int getRandomInt(){
        int min = 0;
        int max = 80;
        Random r = new Random();
        return r.nextInt(max - min + 1) + min;
    }

}
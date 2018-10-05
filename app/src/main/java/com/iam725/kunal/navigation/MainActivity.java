package com.iam725.kunal.navigation;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

//import android.location.LocationListener;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, NavigationView.OnNavigationItemSelectedListener {

        private static final String TAG = "VehicleMapsActivity";
        private static final long INTERVAL = 1000 * 10;             //time in milliseconds
        private static final long FASTEST_INTERVAL = 1000 * 5;
        private static final long INTERMEDIATE_INTERVAL = 1000 * 8;
        private static final String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates";
        private final String USER = "user";
        private final String LATITUDE = "latitude";
        private final String LONGITUDE = "longitude";
        private final String VEHICLE = "vehicle";
        private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
        String busNumber;
        ProgressDialog progressDialog;

        protected GoogleMap mMap;
        protected DatabaseReference mDatabase;
        LocationRequest mLocationRequest;
        GoogleApiClient mGoogleApiClient;
        Location mCurrentLocation = null;
        private FusedLocationProviderClient mFusedLocationClient;
        private LocationCallback mLocationCallback;
        private Boolean mRequestingLocationUpdates;
        Marker markerName;
        static int i = 0;
        private LatLng pos;
        private String userEmail = null;
        private String name = "Location";
        private NotificationCompat.Builder builder;
        private int notificationId = 1;
        private String status = "0";            //status = 1 means notification off
        //status = 0 means notification on
        private ChildEventListener mRefListener;
        private DatabaseReference mRef;

        @SuppressLint("RestrictedApi")
        protected void createLocationRequest() {
                mLocationRequest = new LocationRequest();
                mLocationRequest.setInterval(INTERMEDIATE_INTERVAL);
                mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
                mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {

                Log.d(TAG, "onCreate ...............................");
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_main);
                Toolbar toolbar = findViewById(R.id.toolbar);
                setSupportActionBar(toolbar);
                progressDialog = new ProgressDialog(this);

                DrawerLayout drawer = findViewById(R.id.drawer_layout);
                ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                        this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
                drawer.setDrawerListener(toggle);
                toggle.syncState();

                NavigationView navigationView = findViewById(R.id.nav_view);
                navigationView.setNavigationItemSelectedListener(this);

                // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
                mapFragment.getMapAsync(this);

                mRequestingLocationUpdates = false;
                FirebaseApp.initializeApp(this);
                mDatabase = FirebaseDatabase.getInstance().getReference();

                if (checkPermissions()) {
                        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

                        createLocationRequest();

                        //show error dialog if GoolglePlayServices not available
                        /*if (!isGooglePlayServicesAvailable()) {
                            finish();
                        }*/

                        mGoogleApiClient = new GoogleApiClient.Builder(this)
                                .addApi(LocationServices.API)
                                .addConnectionCallbacks(this)
                                .addOnConnectionFailedListener(this)
                                .build();

                        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                return;
                        }

                        mFusedLocationClient.getLastLocation()
                                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                                        @Override
                                        public void onSuccess(Location location) {
                                                // Got last known location. In some rare situations this can be null.
                                                if (location != null) {
                                                        mCurrentLocation = location;
                                                        onMapReady(mMap);
                                                }
                                        }
                                });
                        mLocationCallback = new LocationCallback() {
                                @Override
                                public void onLocationResult(LocationResult locationResult) {
                                        for (Location location : locationResult.getLocations()) {
                                                mCurrentLocation = location;
                                                if (null != mCurrentLocation) {
                                                        String lat = String.valueOf(mCurrentLocation.getLatitude());
                                                        String lng = String.valueOf(mCurrentLocation.getLongitude());
                                                        mDatabase = FirebaseDatabase.getInstance().getReference();

                                                        DatabaseReference userDatabase = mDatabase.child(USER).child(busNumber).child("location");
                                                        userDatabase.child(LATITUDE).setValue(lat);
                                                        userDatabase.child(LONGITUDE).setValue(lng);
//                                                        Log.d(TAG, "userDatabase@ =  " + userDatabase.toString());
//                                                        Log.d(TAG, "lat = " + lat + ", lng = " + lng);
                                                }
                                                showMyLocationMarker();
                                        }
                                }

                        };

                }

                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                FirebaseUser currentUser = mAuth.getCurrentUser();

                if (currentUser == null) {
                        mAuth.signOut();
                        Intent i = new Intent(MainActivity.this, Login.class);
                        startActivity(i);
                        finish();
                }

        }

        @Override
        public void onBackPressed() {
                DrawerLayout drawer = findViewById(R.id.drawer_layout);
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                        drawer.closeDrawer(GravityCompat.START);
                } else {
                        super.onBackPressed();
                }
        }

        @Override
        public void onMapReady(GoogleMap googleMap) {
                requestPermissions();
                mMap = googleMap;
                // Show Zoom buttons
                mMap.getUiSettings().setZoomControlsEnabled(false);
                // Turns traffic layer on
                mMap.setTrafficEnabled(true);
                // Enables indoor maps
                mMap.setIndoorEnabled(false);
                //Turns on 3D buildings
                mMap.setBuildingsEnabled(true);
                mMap.getUiSettings().setMapToolbarEnabled(false);
                if (checkPermissions()) {
                        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                // TODO: Consider calling
                                //    ActivityCompat#requestPermissions
                                // here to request the missing permissions, and then overriding
                                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                //                                          int[] grantResults)
                                // to handle the case where the user grants the permission. See the documentation
                                // for ActivityCompat#requestPermissions for more details.
                                return;
                        }
                        mMap.setMyLocationEnabled(true);
                }

                // Add a marker in Sydney and move the camera
                /*LatLng sydney = new LatLng(-34, 151);
                mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                }
                mMap.setMyLocationEnabled(true);
                mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                        @Override
                        public boolean onMarkerClick(Marker marker) {
                                pos = marker.getPosition();
//                                Log.e(TAG, "pos ="+pos);
                                return false;
                        }
                });
                mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                        @Override
                        public void onMapLongClick(LatLng latLng) {
                                if (pos != null) {
//                                        Log.e(TAG, "Now pos = "+pos);
                                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 14.0f));
                                        pos = null;
                                }
                                else {
                                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14.0f));
                                }

                        }
                });

                        showMyLocationMarker();

        }

        private void showMyLocationMarker() {

                if (null != mCurrentLocation) {
                        String lat = String.valueOf(mCurrentLocation.getLatitude());
                        String lng = String.valueOf(mCurrentLocation.getLongitude());
                        mDatabase = FirebaseDatabase.getInstance().getReference();
                        if (busNumber == null)          return;
                        DatabaseReference userDatabase = mDatabase.child(USER).child(busNumber).child("location");
                        userDatabase.child(LATITUDE).setValue(lat);
                        userDatabase.child(LONGITUDE).setValue(lng);
                        Log.d(TAG, "userDatabase@ =  " + userDatabase.toString());

                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()), 16.0f));

                } else {
                        Log.d(TAG, "My location is null ...............");
                }

        }

        @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.refresh) {
                refresh();
        }
        else if (id == R.id.action_settings) {
            return true;
        }
        else if (id == R.id.reset) {
                resetPassword();

        }

        return super.onOptionsItemSelected(item);
    }

        private void resetPassword() {

                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Reset Password")
                        .setMessage("Are you sure you want to reset your password ?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                        FirebaseAuth.getInstance().sendPasswordResetEmail(userEmail)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful()) {
                                                                        Log.d(TAG, "Email sent.");
                                                                        Toast.makeText(MainActivity.this, "Email Sent to " + userEmail, Toast.LENGTH_SHORT).show();
                                                                }
                                                        }
                                                });
                                }
                        })
                        .setNegativeButton("No", null)
                        .show();

        }


        private void refresh() {

                if (mMap != null) {
                        mMap.clear();
                }
                onMapReady(mMap);
                startLocationUpdates();

        }

        @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.signOut :
                    new AlertDialog.Builder(this)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle("Closing Activity")
                            .setMessage("Are you sure you want to log out ?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                            {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                            signingOut();
                                    }

                            })
                            .setNegativeButton("No", null)
                            .show();
                break;
                case R.id.remove_markers :
                        new AlertDialog.Builder(this)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setTitle("Remove all markers")
                                .setMessage("Are you sure you want to remove all markers ?")
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                                {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                                removeAllMarkers();
                                        }

                                })
                                .setNegativeButton("No", null)
                                .show();
                        break;
            case R.id.about :
                break;
            case R.id.help :
                break;
            case R.id.Home :
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

        private void removeAllMarkers() {

                Log.d(TAG, "removeAllMarkers fired...");
                DatabaseReference dr = FirebaseDatabase.getInstance().getReference();
                final DatabaseReference vehicleDatabase = dr.child(VEHICLE);
                vehicleDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                                for (DataSnapshot postDataSnapshot : dataSnapshot.getChildren()) {
                                        String keyBus = postDataSnapshot.getKey();              //keyBus = b1,b2, b3, b4,b5.....
                                        Log.e(TAG, "keyBus = " + keyBus);
                                        for (DataSnapshot childSnapshot : postDataSnapshot.getChildren()) {
                                                String keyOfUser = childSnapshot.getKey();         //keyOfUser = temp, different keys of different users....
                                                Log.e(TAG, "keyOfUser = " + keyOfUser);
                                                if (!childSnapshot.getKey().equals("temp")) {
                                                        DatabaseReference childDR = vehicleDatabase.child(keyBus).child(keyOfUser);
                                                        childDR.removeValue();
                                                        Log.e(TAG, "childDR = " + childDR.toString());
                                                }
                                        }
                                }
                                refresh();
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                });

        }

        @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Firing onLocationChanged..............................................");
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    // ...
                    mCurrentLocation = location;
                }
            }
        };
    }
    @Override
    public void onStart() {
        super.onStart();

        Log.d(TAG, "onStart fired ..............");
        mGoogleApiClient.connect();
        if (!checkPermissions()) {
            requestPermissions();
        }

            SharedPreferences loginPrefs = getSharedPreferences("contact", MODE_PRIVATE);
            String userId = loginPrefs.getString("email", "UserId");
            userEmail = userId;
            busNumber = userId;
            if (!busNumber.equals("UserId")) {
                    if (busNumber.contains("bus")) {
                            busNumber = busNumber.split("@")[0];
                            Log.d(TAG, "split(1) =  " + busNumber);
                            if (busNumber.contains("bus")) {
                                    busNumber = busNumber.split("bus")[1];
                                    if(busNumber.charAt(0) == '0') {
                                            busNumber = busNumber.split("0")[1];
                                    }
                            }
                            Log.d(TAG, "busNumberDebug = " + busNumber);
                            busNumber = "b" + busNumber;
                    }
            }
            Log.e(TAG, "userId = "+userId);
            NavigationView navigationView = findViewById(R.id.nav_view);
            View headerView = navigationView.getHeaderView(0);
            TextView tv = headerView.findViewById(R.id.user_id);
            tv.setText(userId);

            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancelAll();
            if (checkPermissions()) {
                    startLocationUpdates();
                    updateMarker();
            }

    }

        private void updateMarker() {

                mDatabase = FirebaseDatabase.getInstance().getReference();
                Log.d(TAG, "mDatabase = " + mDatabase.toString());
                Log.d(TAG, "busNumber  =  " + busNumber);
                Log.d(TAG, "mDatabase.child(VEHICLE) = " + mDatabase.child(VEHICLE).toString());
                if (busNumber == null)    {
                        Log.e(TAG, "busNumber is null");
                        return;
                }

                try {
                        Log.d(TAG, "mDatabase.child(VEHICLE).child(busNumber) = " + mDatabase.child(VEHICLE).child(busNumber).toString());
                        mRef = mDatabase.child(VEHICLE).child(busNumber);
                        builder = new NotificationCompat.Builder(MainActivity.this);
                        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.gogo_blackblue));
                        builder.setSmallIcon(android.R.drawable.ic_notification_overlay);
                        builder.setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND);
                        Log.d(TAG, "mRef = " + mRef.toString());
                        mRefListener = mRef.addChildEventListener(new ChildEventListener() {
                                @Override
                                public void onChildAdded(final DataSnapshot dataSnapshot, String s) {

                                        Log.d(TAG, "busNumber1 = " + busNumber);
                                        Log.d(TAG, "string s = " + s);
                                        Log.d(TAG, "dataSnapshot onChildAdded : " + dataSnapshot);

//                                        Log.d(TAG, "map = " + map);
                                        if (!dataSnapshot.getKey().equals("temp")) {
                                                name = dataSnapshot.getKey();
                                                Log.d(TAG, "name = " + name);
//                                            key = dataSnapshot.getKey();
//                                            Log.d(TAG, "key = " + key);

                                                if (!name.equals("temp")) {

                                                        DatabaseReference keyRef = mRef.child(name);
                                                        Log.d(TAG, "keyRef = " + keyRef);

                                                        keyRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                                                @Override
                                                                public void onDataChange(DataSnapshot p_dataSnapshot) {

                                                                        final String latitudeStr = (String) p_dataSnapshot.child(LATITUDE).getValue();
                                                                        final String longitudeStr = (String) p_dataSnapshot.child(LONGITUDE).getValue();
                                                                        status = (String) p_dataSnapshot.child("status").getValue();

                                                                        Log.d(TAG, "@@DATASNAPSHOT = "+dataSnapshot.toString());
                                                                        Log.d(TAG, "onDataChange-Latitude = " + latitudeStr);
                                                                        Log.d(TAG, "onDataChange-Longitude = " + longitudeStr);
                                                                        if (latitudeStr == null || longitudeStr == null)
                                                                                return;
                                                                        double latitude = Double.parseDouble(latitudeStr);
                                                                        double longitude = Double.parseDouble(longitudeStr);

                                                                        markerName = mMap.addMarker(new MarkerOptions()
                                                                                .position(new LatLng(latitude, longitude))
                                                                                .title(name));
                                                                        markerName.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_person_pin_circle_black_24dp));
                                                                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 12.0f));
                                                                        Log.e(TAG, "markerji is added");

                                                                        if (status != null && status.equals("0")) {
                                                                                Log.e(TAG, "Notification Chamber");
                                                                                builder.setContentTitle("Pick up Request");
                                                                                builder.setContentText("From : " + name);
                                                                                Intent i = new Intent(MainActivity.this, MainActivity.class);
                                                                                PendingIntent pi = PendingIntent.getActivity(MainActivity.this, 0, i, 0);
                                                                                builder.setContentIntent(pi);
                                                                                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                                                                                notificationManager.notify(notificationId++, builder.build());
                                                                                status = null;
//                                                                                        Log.d(TAG, "NOW status = "+ status);
                                                                                DatabaseReference keyRef_d = mRef.child(name);
                                                                                keyRef_d.child("status").setValue("1");
                                                                        }
                                                                }

                                                                @Override
                                                                public void onCancelled(DatabaseError databaseError) {

                                                                }
                                                        });
                                                }
                                        }
                                }

                                @Override
                                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                                }
                                //
                                @Override
                                public void onChildRemoved(DataSnapshot dataSnapshot) {

                                        Log.d(TAG, "@REMOVE dataSnapshot = " + dataSnapshot.toString());
                                        Log.d(TAG, "@REMOVE dataSnapshot.getKey() = "+ dataSnapshot.getKey());


                                        assert mMap != null;
                                        mMap.clear();
                                        onMapReady(mMap);
//                                            startLocationUpdates();
                                        mRef.removeEventListener(mRefListener);
//                                        mRef.removeEventListener(this);
                                        mRef = null;
                                        updateMarker();

                                }

                                @Override
                                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                        });
                } catch (Exception e) {
                        Log.e(TAG, "ERROR : " + e.toString());
                }

        }


        private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {

            Log.i(TAG, "Displaying permission rationale to provide additional context.");

        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            startLocationPermissionRequest();
        }
    }

    private void startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_PERMISSIONS_REQUEST_CODE);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop fired ..............");
        mGoogleApiClient.disconnect();
        Log.d(TAG, "isConnected ...............: " + mGoogleApiClient.isConnected());
        if(mRef != null)
               mRef.removeEventListener(mRefListener);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected - isConnected ...............: " + mGoogleApiClient.isConnected());

        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "onConnectedSuspended");
    }

    private boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == status) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(status, this, 0).show();
            return false;
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "Connection failed: " + connectionResult.toString());
    }

    @Override
    protected void onPause() {
        super.onPause();
            /** Not stopping location updates so that app will update
             *  the location even when app is not closed
             */
//            if (checkPermissions())
//                stopLocationUpdates();
    }


    protected void stopLocationUpdates() {
            if (mFusedLocationClient != null)
                    mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        Log.d(TAG, "Location update stopped .......................");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    //powerful function
    protected void startLocationUpdates() {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
            }
        /*PendingResult<Status> pendingResult = FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, (com.google.android.gms.location.LocationListener) this);*/
            Log.d(TAG, "Location update started ..............: ");
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback,
                    null /* Looper */);

    }
        @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY,
                mRequestingLocationUpdates);
        // ...
        super.onSaveInstanceState(outState);
    }

    public void signingOut () {

        Intent i = new Intent(MainActivity.this, Login.class);
        FirebaseAuth.getInstance().signOut();

        progressDialog.setTitle("Catch App");
        progressDialog.setMessage("Logging Out...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.show();
        progressDialog.setCancelable(true);
        new Thread(new Runnable() {
              public void run() {
                      try {
                              Thread.sleep(5000);
                      } catch (Exception e) {
                              e.printStackTrace();
                      }
                      if (progressDialog != null) {
                              progressDialog.dismiss();
                              progressDialog = null;
                      }

            }
        }).start();

        startActivity(i);
        finish();

    }
        @Override
        public void onDestroy() {
                super.onDestroy();
                if (progressDialog != null) {
                        progressDialog.dismiss();
                        progressDialog = null;
                }
                if(mRef != null)
                        mRef.removeEventListener(mRefListener);
        }

}

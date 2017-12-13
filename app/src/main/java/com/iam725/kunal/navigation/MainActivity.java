package com.iam725.kunal.navigation;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
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
import android.widget.ListView;
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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import android.location.LocationListener;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, NavigationView.OnNavigationItemSelectedListener {

        private static final String TAG = "VehicleMapsActivity";
        private static final long INTERVAL = 1000 * 10;             //time in milliseconds
        private static final long FASTEST_INTERVAL = 1000 * 5;
        private static final String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates";
        private final String USER = "user";
        private final String LATITUDE = "latitude";
        private final String LONGITUDE = "longitude";
        private final String VEHICLE = "vehicle";
        String key;
        private int checkBusSelection = 0;
        private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
        private Context context;
        private DrawerLayout drawerLayout;
        private ActionBarDrawerToggle mDrawerToggle;
        private CharSequence mDrawerTitle;
        private CharSequence mTitle;
        private ListView mDrawerList;
        boolean isGPSEnabled = false;
        boolean isNetworkEnabled = false;
        boolean canGetLocation = false;
        String busNumber;
        ProgressDialog progressDialog;

        Map<String, Integer> dict = new HashMap<>();
        @SuppressLint("UseSparseArrays")
        Map<Integer, Marker> markers = new HashMap<>();

        protected GoogleMap mMap;
        protected DatabaseReference mDatabase;
        LocationRequest mLocationRequest;
        GoogleApiClient mGoogleApiClient;
        Location mCurrentLocation = null;
        private FusedLocationProviderClient mFusedLocationClient;
        private LocationCallback mLocationCallback;
        private Boolean mRequestingLocationUpdates;
        Marker markerName;
        TextView distance;
        TextView duration;
        protected LocationManager locationManager;
        static int i = 0;
        private ArrayList<Marker> markerset = new ArrayList<>();
        private final String ROUTE = "route";
        private LatLng nearestLatlng = null;
        private double dist = 0;
        private double dur = 0;
        private double minDistance = Double.MAX_VALUE;
        private LatLng minLocation = null;
        private int noOfChildren = 0;
        private Marker myLocationMarker;

        protected void createLocationRequest() {
                mLocationRequest = new LocationRequest();
                mLocationRequest.setInterval(INTERVAL);
                mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
                mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {

                Log.d(TAG, "onCreate ...............................");
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_main);
                Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
                setSupportActionBar(toolbar);
                progressDialog = new ProgressDialog(this);
      /*  FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/

                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                        this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
                drawer.setDrawerListener(toggle);
                toggle.syncState();

                NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
                navigationView.setNavigationItemSelectedListener((NavigationView.OnNavigationItemSelectedListener) this);

                // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
                mapFragment.getMapAsync(this);

                mRequestingLocationUpdates = false;
                FirebaseApp.initializeApp(this);
                mDatabase = FirebaseDatabase.getInstance().getReference();

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

                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                FirebaseUser currentUser = mAuth.getCurrentUser();

                if (currentUser == null) {
                        mAuth.signOut();
                        Intent i = new Intent(MainActivity.this, Login.class);
                        startActivity(i);
                        finish();
                }

                mLocationCallback = new LocationCallback() {
                        @Override
                        public void onLocationResult(LocationResult locationResult) {
                                for (Location location : locationResult.getLocations()) {
                                        mCurrentLocation = location;
                                        showMyLocationMarker();
                                }
                        }

                };
        }

        @Override
        public void onBackPressed() {
                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                        drawer.closeDrawer(GravityCompat.START);
                } else {
                        super.onBackPressed();
                }
        }

        @Override
        public void onMapReady(GoogleMap googleMap) {
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
                String str = "My Location";
                if (null != mCurrentLocation) {

                        SharedPreferences myPrefs = this.getSharedPreferences("contact", MODE_PRIVATE);
                        busNumber = myPrefs.getString("email", "stupid");
                        if (!busNumber.equals("stupid")) {
                                if (!busNumber.contains("bus")) {

                                        Toast.makeText(this, "You are not in the list", Toast.LENGTH_SHORT).show();
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
                                busNumber = busNumber.split("@")[0];
                                Log.d(TAG, "split(1) =  " + busNumber);
                                if (busNumber.contains("bus")) {
                                        busNumber = busNumber.split("bus")[1];
                                }
                                Log.d(TAG, "busNumberDebug = " + busNumber);
                                busNumber = "b" + busNumber;

                        }
                }
                showMyLocationMarker();

        }

        private void showMyLocationMarker() {

                if (null != mCurrentLocation) {
                        String lat = String.valueOf(mCurrentLocation.getLatitude());
                        String lng = String.valueOf(mCurrentLocation.getLongitude());
                        mDatabase = FirebaseDatabase.getInstance().getReference();

                        DatabaseReference userDatabase = mDatabase.child(USER).child(busNumber).child("location");
                        userDatabase.child(LATITUDE).setValue(lat);
                        userDatabase.child(LONGITUDE).setValue(lng);
                        Log.d(TAG, "userDatabase@ =  " + userDatabase.toString());

                        String str = "My Location";
                        Geocoder geocoder = new Geocoder(getApplicationContext());

                        try {
                                List<android.location.Address> addressList = geocoder.getFromLocation(mCurrentLocation.getLatitude(),
                                        mCurrentLocation.getLongitude(), 1);
                                str = "";
                                if (addressList.get(0).getSubLocality() != null) {
                                        str = addressList.get(0).getSubLocality()+",";
                                }
                                str += addressList.get(0).getLocality();
                                Log.d(TAG, "GEOCODER STARTED.");
                        } catch (IOException e) {
                                e.printStackTrace();
                                Log.e(TAG, "GEOCODER DIDN'T WORK.");
                        }

                        if (myLocationMarker != null) {
                                myLocationMarker.remove();
                        }
                        myLocationMarker = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()))
                                .title(str));
                        myLocationMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.end_green));
                        Log.e(TAG, "myLocationMarker = " + myLocationMarker);
                        //                        mMap.animateCamera(CameraUpdateFactory.newLatLng(
//                                new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude())));

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

                if (mMap != null) {
                        mMap.clear();
                }
                onMapReady(mMap);
                startLocationUpdates();
        }
        else if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
            case R.id.about :
                break;
            case R.id.help :
                break;
            case R.id.Home :
                break;
        }
       /* if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }*/

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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
            String userId = loginPrefs.getString("email", "User id");
            Log.e(TAG, "userId = "+userId);
            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            View headerView = navigationView.getHeaderView(0);
            TextView tv = (TextView) headerView.findViewById(R.id.user_id);
            tv.setText(userId);
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
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected - isConnected ...............: " + mGoogleApiClient.isConnected());

        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

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
        stopLocationUpdates();
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
            mDatabase = FirebaseDatabase.getInstance().getReference();
            Log.d(TAG, "mDatabase = " + mDatabase.toString());
            Log.d(TAG, "busNumber  =  " + busNumber);
            Log.d(TAG, "mDatabase.child(VEHICLE) = " + mDatabase.child(VEHICLE).toString());

            try {
                    Log.d(TAG, "mDatabase.child(VEHICLE).child(busNumber) = " + mDatabase.child(VEHICLE).child(busNumber).toString());
                    final DatabaseReference mRef = mDatabase.child(VEHICLE).child(busNumber);
                    Log.d(TAG, "mRef = " + mRef.toString());

                    mRef.addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                                    Log.d(TAG, "dataSnapshot onChildAdded : " + dataSnapshot);

//                                        Log.d(TAG, "map = " + map);
                                    if (!dataSnapshot.getKey().equals("temp")) {
                                            key = dataSnapshot.getKey();
                                            Log.d(TAG, "key = " + key);

                                            if (!key.equals("temp")) {
                                                    DatabaseReference locationRef = mRef.child(key).child("LOCATION");
                                                    Log.d(TAG, "locationRef =  " + locationRef.toString());
                                                    locationRef.addValueEventListener(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                                    GenericTypeIndicator<Map<String, String>> genericTypeIndicator = new GenericTypeIndicator<Map<String, String>>() {
                                                                    };
                                                                    Map<String, String> map = dataSnapshot.getValue(genericTypeIndicator);
                                                                /*GenericTypeIndicator<Map<String, Map<String, String>>> genericTypeIndicator = new GenericTypeIndicator<Map<String, Map<String, String>>>() {
                                                                };
                                                                Map<String, Map<String, String>> map = dataSnapshot.getValue(genericTypeIndicator);*/
                                                                    //Map<String, String> newMap =
                                                                    if (map != null) {
                                                                            Log.d(TAG, "onDataChange-map in onValueEventListener =  " + map);
                                                                            String latitudeStr = map.get(LATITUDE);
                                                                            String longitudeStr = map.get(LONGITUDE);

                                                                            Log.d(TAG, "onDataChange-Latitude = " + latitudeStr);
                                                                            Log.d(TAG, "onDataChange-Longitude = " + longitudeStr);

                                                                            double latitude = Double.parseDouble(latitudeStr);
                                                                            double longitude = Double.parseDouble(longitudeStr);
                                                                            LatLng latLng = new LatLng(latitude, longitude);

                                                                            String str = "Location";
                                                                            if (null != mCurrentLocation) {

                                                                                    Geocoder geocoder = new Geocoder(getApplicationContext());

                                                                                    try {
                                                                                            List<Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);
                                                                                            if (addressList.get(0).getSubLocality() != null) {
                                                                                                    str = "";
                                                                                                    str += addressList.get(0).getSubLocality() + ",";
                                                                                            }
                                                                                            str += addressList.get(0).getLocality();
//                                                                                            str += addressList.get(0).getCountryName();
                                                                                            Log.d(TAG, "onDataChange-GEOCODER STARTED.");
                                                                                    } catch (IOException e) {
                                                                                            e.printStackTrace();
                                                                                            Log.e(TAG, "onDataChange-GEOCODER DIDN'T WORK.");
                                                                                    }
                                                                                    i = (int) dataSnapshot.getChildrenCount() - 1;
                                                                                    markerName = mMap.addMarker(new MarkerOptions()
                                                                                            .position(new LatLng(latitude, longitude))
                                                                                            .title(str));
                                                                                    markerName.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.start_blue));
                                                                                    markers.put(i, markerName);
                                                                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 12.0f));
                                                                                    markerset.add(markerName);
                                                                                    Log.d(TAG, "onDataChange-markerName = " + markerName.toString());
                                                                                    dict.put(key, i);
                                                                                    //Log.d(TAG, "dict = " + dict);
                                                                                    Log.d(TAG, "onDataChange-key = " + key);
                                                                                    Log.d(TAG, "onDataChange-i = " + i);
                                                                                    Log.d(TAG, "onDataChange-dict = " + dict.toString());
                                                                                    Log.d(TAG, "onDataChange-dict.get(key)  = " + dict.get(key));
                                                                            }
                                                                    }


                                                            }

                                                            @Override
                                                            public void onCancelled(DatabaseError databaseError) {

                                                            }

                                                    });


                                                    Log.d(TAG, "Data : " + dataSnapshot.getValue());
                                            }
                                    }
                            }

                            @Override
                            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                            }

                            @Override
                            public void onChildRemoved(DataSnapshot dataSnapshot) {
//                    int userToDelete = dict.get(dataSnapshot.getKey());
//                    Log.d(TAG, "onChildRemoved-userToDelete = " + userToDelete);
//                    Marker markerName = markers.get(userToDelete);
//                    Log.d(TAG, "onChildRemoved-markerName BEFORE DELETION = " + markerName);
//                    Log.d(TAG, "onChildRemoved-markerName.toString() BEFORE DELETION = " + markerName.toString());
//
//                    if (markerName != null) {
//                        markerName.remove();
//                        assert  markerName == null;
//                        //Log.d(TAG, "onChildRemoved-markerName AFTER DELETION = " + markerName);
//                        //Log.d(TAG, "onChildRemoved-markerName.toString() AFTER DELETION = " + markerName.toString());
//                        i = (int) dataSnapshot.getChildrenCount() - 1;
//                    }
                                    assert mMap != null;
                                    mMap.clear();
                                    onMapReady(mMap);
                                    startLocationUpdates();

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
        @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY,
                mRequestingLocationUpdates);
        // ...
        super.onSaveInstanceState(outState);
    }

    public void onNormalMap(View view) {
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    }

    public void onSatelliteMap(View view) {
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
    }

    public void onTerrainMap(View view) {
        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
    }

    public void onHybridMap(View view) {
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
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
        }

}

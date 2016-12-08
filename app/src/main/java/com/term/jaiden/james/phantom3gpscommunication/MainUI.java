package com.term.jaiden.james.phantom3gpscommunication;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.multidex.MultiDex;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.battery.DJIBatteryState;
import dji.common.error.DJIError;
import dji.common.flightcontroller.DJIFlightControllerCurrentState;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.common.error.DJISDKError;
import dji.sdk.battery.DJIBattery;
import dji.sdk.flightcontroller.DJIFlightController;
import dji.sdk.flightcontroller.DJIFlightControllerDelegate;
import dji.sdk.missionmanager.DJIMission;
import dji.sdk.missionmanager.DJIMissionManager;
import dji.sdk.missionmanager.DJIWaypoint;
import dji.sdk.missionmanager.DJIWaypointMission;
import dji.sdk.products.DJIAircraft;
import dji.sdk.sdkmanager.DJIAoaControllerActivity;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdksharedlib.hardware.a;

public class MainUI extends AppCompatActivity implements View.OnClickListener, GoogleMap.OnMapClickListener, OnMapReadyCallback {
    private static final String TAG = MainUI.class.getName();
    public static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";
    private double lat, lon;
    private boolean isAdd = false;
    private final Map<Integer, Marker> mMarkers = new ConcurrentHashMap<Integer, Marker>();
    private float altitude = 100f, mSpeed = 10f;
    private Marker droneMarker = null; // For when we implement map view
    private DJIFlightController mFlightController;
    private DJIBaseProduct mProduct;
    private TextView vout;
    private GoogleMap gMap;
    private Button add;
    private DJIWaypointMission.DJIWaypointMissionFinishedAction mFinishedAction = DJIWaypointMission.DJIWaypointMissionFinishedAction.NoAction;
    private DJIWaypointMission.DJIWaypointMissionHeadingMode mHeadingMode = DJIWaypointMission.DJIWaypointMissionHeadingMode.Auto;
    private DJIWaypointMission mWaypointMission;
    private DJIMissionManager mMissionManager;
    private DJISDKManager sdkManager;
    private Handler mHandler;
    private static final double GPS_THRESHOLD = .00000000000001;
    private AlertDialog settingDialog, mapDialog;

    @Override
    protected void onResume() {
        super.onResume();
        /*
        IntentFilter filter = new IntentFilter();
        filter.addAction(FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);
        onProductConnectionChange();
        */
    }

    @Override
    protected void onPause() {
        super.onPause();
        //unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unregisterReceiver(mReceiver);
    }

    public void onReturn(View view) {
        //Log.d(TAG, "onReturn");
        //this.finish();
    }

    /*
    protected void attachBaseContext(Context base){
        super.attachBaseContext(base);
        MultiDex.install(this);
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //MultiDex.install(this);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_PHONE_STATE)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_PHONE_STATE},
                        1);
            }
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        1);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS (changed to 1) is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        /*
        int REQUEST_EXTERNAL_STORAGE = 1;
        String[] PERMISSIONS_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }*/

        setContentView(R.layout.activity_main_ui);

        //Initialize DJI SDK Manager
        mHandler = new Handler(Looper.getMainLooper());


        /*try {
            Thread.currentThread().wait(500);
        } catch (Exception ex) {
            ex.printStackTrace();
        }*/

        //System.setProperty("http.keepAlive", "false");
        //attemptConnection();

        //Register BroadcastReceiver

        MapsInitializer.initialize(getApplicationContext());

        IntentFilter filter = new IntentFilter();
        filter.addAction(FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

        //set up vout
        vout = ((TextView) findViewById(R.id.textView4));
        vout.setGravity(Gravity.BOTTOM);
        vout.setMovementMethod(new ScrollingMovementMethod());

        Button flight = (Button) findViewById(R.id.toggleButton2);
        Button map = (Button) findViewById(R.id.map_v);
        flight.setOnClickListener(new FlightStatusHandler(this, (ToggleButton) flight));
        map.setOnClickListener(this);

        try {
            //register for GPS updates
            GPSFollowHandler fh = new GPSFollowHandler(this);
            LocationManager lm = ((LocationManager) getSystemService(Context.LOCATION_SERVICE));
            //lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0/*GPSFollowHandler.UPDATE_FREQUENCY_MS, .5F/*meter*/, fh);
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0/*GPSFollowHandler.UPDATE_FREQUENCY_MS, .5F/*meter*/, fh);

        } catch (SecurityException ex) {
            ex.printStackTrace();
            uiConsolePrint(ex.toString() + "\n");
        }

        //attemptConnection();

        //updateDroneLocation();
        //notifyStatusChange();

        sdkManager = DJISDKManager.getInstance();
        sdkManager.initSDKManager(this, mDJISDKManagerCallback);
    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (getResultCode() == Activity.RESULT_OK) {
                onProductConnectionChange();
            }
        }
    };

    private synchronized void onProductConnectionChange() {
        //notifyStatusChange();
        //sdkManager.startConnectionToProduct();
        initFlightController();
        initMissionManager();
    }

    protected synchronized void initFlightController() {
        if (mFlightController != null) {
            return;
        }

        uiConsolePrint("Initializing Flight Controller: ");

        if (mProduct != null && mProduct.isConnected() && mProduct instanceof DJIAircraft) {
            mFlightController = ((DJIAircraft) mProduct).getFlightController();
        }

        if (mFlightController != null) {

            final MainUI save = this;

            mFlightController.setUpdateSystemStateCallback(new DJIFlightControllerDelegate.FlightControllerUpdateSystemStateCallback() {

                @Override
                public void onResult(DJIFlightControllerCurrentState state) {
                    double lat = state.getAircraftLocation().getLatitude();
                    double lon = state.getAircraftLocation().getLongitude();

                    if (Math.abs(lat - save.lat) < GPS_THRESHOLD || Math.abs(lon - save.lon) < GPS_THRESHOLD) {
                        return;//no need to update the same coordinate
                    }

                    if (!checkGpsCoordinates(lat, lon)) {
                        uiConsolePrint("Invalid coordinates: <" + lat + ", " + lon + ">\n");
                        return;
                    }

                    save.lat = lat;
                    save.lon = lon;

                    updateDroneLocation();
                }

            });

        }


        uiConsolePrint((mFlightController == null ? "null" : "Initialized") + "\n");
    }

    public static boolean checkGpsCoordinates(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    public void uiConsolePrint(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                vout.append(s);
            }
        });
    }

    private synchronized void updateDroneLocation() {
        if (!checkGpsCoordinates(lat, lon)) {
            //already checked, they shouldn't be invalid
            uiConsolePrint("Internal location error!\n");
            return;
        }

        LatLng pos = new LatLng(lat, lon);
        uiConsolePrint("Update drone location to " + pos + "\n");

        //Create MarkerOptions object
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(pos);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.mipmap.aircraft));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (droneMarker != null) {
                    droneMarker.remove();
                }
                if (checkGpsCoordinates(lat, lon)) {
                    if (gMap != null){
                        droneMarker = gMap.addMarker(markerOptions);
                    }
                }
            }
        });
    }

    private void showMapDialog() {
        if (mapDialog == null) {
            initMapDialog();
        }
        mapDialog.show();
    }

    private void initMapDialog() {
        LinearLayout mapView = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_mapview, null);

        //buttons
        Button locate = (Button) mapView.findViewById(R.id.locate);
        Button config = (Button) mapView.findViewById(R.id.config);
        add = (Button) mapView.findViewById(R.id.add);
        Button clear = (Button) mapView.findViewById(R.id.clear);
        Button prepare = (Button) mapView.findViewById(R.id.prepare);

        //add click listeners
        locate.setOnClickListener(this);
        config.setOnClickListener(this);
        add.setOnClickListener(this);
        clear.setOnClickListener(this);
        prepare.setOnClickListener(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mapDialog = new AlertDialog.Builder(this)
                .setTitle("")
                .setView(mapView)
                .setPositiveButton("Finish", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .create();
                //.show();
    }

    private void showSettingDialog() {
        if (settingDialog == null) {
            initSettingDialog();
        }
        settingDialog.show();
    }

    private void initSettingDialog() {
        LinearLayout wayPointSettings = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_waypointsetting, null);
        final TextView wpAltitude_TV = (TextView) wayPointSettings.findViewById(R.id.altitude);

        //radio groups
        RadioGroup speed_RG = (RadioGroup) wayPointSettings.findViewById(R.id.speed);
        RadioGroup actionAfterFinished_RG = (RadioGroup) wayPointSettings.findViewById(R.id.actionAfterFinished);
        RadioGroup heading_RG = (RadioGroup) wayPointSettings.findViewById(R.id.heading);

        speed_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.lowSpeed) {
                    mSpeed = 3.0f;
                } else if (checkedId == R.id.MidSpeed) {
                    mSpeed = 5.0f;
                } else if (checkedId == R.id.HighSpeed) {
                    mSpeed = 10.0f;
                }
            }
        });

        actionAfterFinished_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "Select finish action");
                if (checkedId == R.id.finishNone) {
                    mFinishedAction = DJIWaypointMission.DJIWaypointMissionFinishedAction.NoAction;
                } else if (checkedId == R.id.finishGoHome) {
                    mFinishedAction = DJIWaypointMission.DJIWaypointMissionFinishedAction.GoHome;
                } else if (checkedId == R.id.finishAutoLanding) {
                    mFinishedAction = DJIWaypointMission.DJIWaypointMissionFinishedAction.AutoLand;
                } else if (checkedId == R.id.finishToFirst) {
                    mFinishedAction = DJIWaypointMission.DJIWaypointMissionFinishedAction.GoFirstWaypoint;
                }
            }
        });

        heading_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "Select heading");
                if (checkedId == R.id.headingNext) {
                    mHeadingMode = DJIWaypointMission.DJIWaypointMissionHeadingMode.Auto;
                } else if (checkedId == R.id.headingInitDirec) {
                    mHeadingMode = DJIWaypointMission.DJIWaypointMissionHeadingMode.UsingInitialDirection;
                } else if (checkedId == R.id.headingRC) {
                    mHeadingMode = DJIWaypointMission.DJIWaypointMissionHeadingMode.ControlByRemoteController;
                } else if (checkedId == R.id.headingWP) {
                    mHeadingMode = DJIWaypointMission.DJIWaypointMissionHeadingMode.UsingWaypointHeading;
                }
            }
        });

        settingDialog = new AlertDialog.Builder(this)
                .setTitle("")
                .setView(wayPointSettings)
                .setPositiveButton("Finish", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String altitudeString = wpAltitude_TV.getText().toString();
                        altitude = Integer.parseInt(nulltoIntegerDefault(altitudeString));
                        Log.e(TAG, "altitude " + altitude);
                        Log.e(TAG, "speed " + mSpeed);
                        Log.e(TAG, "mFinishedAction " + mFinishedAction);
                        Log.e(TAG, "mHeadingMode " + mHeadingMode);
                        configWayPointMission();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .create();
                //.show();
    }

    private synchronized void configWayPointMission() {
        if (mWaypointMission != null) {

            //set waypoint variables
            mWaypointMission.finishedAction = mFinishedAction;
            mWaypointMission.headingMode = mHeadingMode;
            mWaypointMission.autoFlightSpeed = mSpeed;

            if (mWaypointMission.waypointsList.size() > 0) {
                for (int i = 0; i < mWaypointMission.waypointsList.size(); i++) {
                    mWaypointMission.getWaypointAtIndex(i).altitude = altitude;
                }
                setResultToToast("Set Waypoint altitude success");
            }
        }
    }

    protected synchronized void initMissionManager() {
        if (mProduct == null || !mProduct.isConnected()) {

            setResultToToast("Product Not Connected");
            mMissionManager = null;
            return;

        } else {

            setResultToToast("Product Connected");
            mMissionManager = mProduct.getMissionManager();
            //mMissionManager.setMissionProgressStatusCallback(this);
            //mMissionManager.setMissionExecutionFinishedCallback(this);

        }
        //mWaypointMission = new DJIWaypointMission();
    }

    public void missionProgressStatus(DJIMission.DJIMissionProgressStatus progressStatus) {}

    public void onResult(DJIError error) {
        setResultToToast("Execution finished: " + (error == null ? "Success" : error.getDescription()));
    }

    protected String nulltoIntegerDefault(String value) {
        if (!isIntValue(value)) value = "0";
        return value;
    }

    protected boolean isIntValue(String val) {
        try {
            val = val.replace(" ", "");
            Integer.parseInt(val);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Initializing Amap object
        uiConsolePrint("Calling Map Ready\n");

        if (gMap == null) {
            gMap = googleMap;
            uiConsolePrint("Map Initialized: " + gMap + "\n");
            setUpMap();
        }

        uiConsolePrint("Map Status: " + gMap + "\n");
        LatLng Shenzhen = new LatLng(22.5500, 114.1000);
        gMap.addMarker(new MarkerOptions().position(Shenzhen).title("Marker in Shenzhen"));
        gMap.moveCamera(CameraUpdateFactory.newLatLng(Shenzhen));
    }

    private synchronized void setUpMap() {
        uiConsolePrint("Setting up Map\n");
        gMap.setOnMapClickListener(this);// add the listener for click for a map object
    }

    private void setResultToToast(final String string) {
        MainUI.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainUI.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapClick(LatLng point) {
        if (isAdd) {

            uiConsolePrint("Added waypoint at " + point + "\n");
            markWaypoint(point);
            DJIWaypoint mWaypoint = new DJIWaypoint(point.latitude, point.longitude, altitude);
            //Add waypoints to Waypoint arraylist;

            if (mWaypointMission == null) {
                mWaypointMission = new DJIWaypointMission();
            }

            if (mWaypointMission != null) {
                mWaypointMission.addWaypoint(mWaypoint);
                setResultToToast("AddWaypoint");
            }

        } else {

            uiConsolePrint("Unable to place waypoint at " + point + "\n");
            setResultToToast("Cannot add waypoint");

        }
    }

    private synchronized void markWaypoint(LatLng point) {
        //Create MarkerOptions object
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        Marker marker = gMap.addMarker(markerOptions);
        mMarkers.put(mMarkers.size(), marker);
    }

    private synchronized void prepareWayPointMission() {
        if (mMissionManager != null && mWaypointMission != null) {
            DJIMission.DJIMissionProgressHandler progressHandler = new DJIMission.DJIMissionProgressHandler() {
                @Override
                public void onProgress(DJIMission.DJIProgressType type, float progress) {
                }
            };
            mMissionManager.prepareMission(mWaypointMission, progressHandler, new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    setResultToToast(error == null ? "Success" : error.getDescription());
                }
            });
        }
    }

    private synchronized void startWaypointMission() {
        if (mMissionManager != null) {
            mMissionManager.startMissionExecution(new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    setResultToToast("Start: " + (error == null ? "Success" : error.getDescription()));
                }
            });
        }
    }

    private synchronized void stopWaypointMission() {
        if (mMissionManager != null) {
            mMissionManager.stopMissionExecution(new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    setResultToToast("Stop: " + (error == null ? "Success" : error.getDescription()));
                }
            });
            if (mWaypointMission != null) {
                mWaypointMission.removeAllWaypoints();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.map_v: {
                showMapDialog();//TODO can only instantiate once
            }
            case R.id.prepare: {
                prepareWayPointMission();
                break;
            }
            case R.id.add: {
                enableDisableAdd();
                break;
            }
            case R.id.clear: {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        gMap.clear();
                    }
                });
                if (mWaypointMission != null) {
                    mWaypointMission.removeAllWaypoints(); // Remove all the waypoints added to the task
                }
                break;
            }
            case R.id.locate: {
                //TODO Maybe error with camera update is here?
                updateDroneLocation();
                cameraUpdate();
                break;
            }
            case R.id.config: {
                showSettingDialog();
                break;
            }
            case R.id.start: {
                startWaypointMission();
                break;
            }
            case R.id.stop: {
                stopWaypointMission();
                break;
            }
            default:
                break;
        }
    }

    private void enableDisableAdd() {
        if (isAdd == false) {
            isAdd = true;
            add.setText("Exit");
        } else {
            isAdd = false;
            add.setText("Add");
        }
    }

    private synchronized void cameraUpdate() {
        uiConsolePrint("Camera Update Reached\n");
        LatLng pos = new LatLng(lat, lon);
        uiConsolePrint("Position: (" + lat + ", " + lon + ") (" + pos + ")\n");

        float zoomlevel = (float) 18.0;
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(pos, zoomlevel);
        uiConsolePrint("Status of map: " + gMap + "\n");
        //TODO Figure out where this is being called before onMapReady()

        if (gMap != null) {
            gMap.moveCamera(cu);
        }
    }

    private DJISDKManager.DJISDKManagerCallback mDJISDKManagerCallback = new DJISDKManager.DJISDKManagerCallback() {
        @Override
        public void onGetRegisteredResult(DJIError error) {
            Log.d(TAG, error == null ? "success" : error.getDescription());
            if (error == DJISDKError.REGISTRATION_SUCCESS) {
                sdkManager.startConnectionToProduct();
                uiConsolePrint("App Registered Successfully\n");
                /*
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Register App Successful", Toast.LENGTH_LONG).show();
                    }
                });*/
            } else {
                uiConsolePrint("App Failed to Register!\n");
                /*
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Register App Failed! Please enter your App Key and check the network.", Toast.LENGTH_LONG).show();
                    }
                });
                */
            }
            Log.e("TAG", error.getDescription());
        }

        @Override
        public void onProductChanged(DJIBaseProduct oldProduct, DJIBaseProduct newProduct) {
            if (newProduct == oldProduct) {
                return;
            }
            mProduct = newProduct;
            if (mProduct != null) {
                mProduct.setDJIBaseProductListener(mDJIBaseProductListener);
            }
            notifyStatusChange();
        }
    };

    private DJIBaseProduct.DJIBaseProductListener mDJIBaseProductListener = new DJIBaseProduct.DJIBaseProductListener() {
        @Override
        public void onComponentChange(DJIBaseProduct.DJIComponentKey key, DJIBaseComponent oldComponent, DJIBaseComponent newComponent) {
            if (newComponent == oldComponent) {
                return;
            }
            if (newComponent != null) {
                newComponent.setDJIComponentListener(mDJIComponentListener);
            }
            notifyStatusChange();
        }

        @Override
        public void onProductConnectivityChanged(boolean isConnected) {
            uiConsolePrint((isConnected ? "Connected" : "Disconnected") + "\n");
            notifyStatusChange();
        }
    };

    private DJIBaseComponent.DJIComponentListener mDJIComponentListener = new DJIBaseComponent.DJIComponentListener() {
        @Override
        public void onComponentConnectivityChanged(boolean isConnected) {
            notifyStatusChange();
        }
    };

    protected synchronized void notifyStatusChange() {
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable, 500);
    }

    private Runnable updateRunnable = new Runnable() {

        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            sendBroadcast(intent);
        }
    };

    public synchronized DJIMissionManager getMissionManager() {
        if (mProduct == null) {
            System.out.println("Mission manager is null!");
            return null;
        }
        return mProduct.getMissionManager();
    }

    public synchronized DJIBaseProduct getBaseProduct() {
        return mProduct;
    }
}

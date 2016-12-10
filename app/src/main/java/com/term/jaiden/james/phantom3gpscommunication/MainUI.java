package com.term.jaiden.james.phantom3gpscommunication;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.multidex.MultiDex;
import android.support.v4.app.ActivityCompat;
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

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.flightcontroller.DJIFlightControllerCurrentState;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.flightcontroller.DJIFlightController;
import dji.sdk.flightcontroller.DJIFlightControllerDelegate;
import dji.sdk.missionmanager.DJIMission;
import dji.sdk.missionmanager.DJIMissionManager;
import dji.sdk.missionmanager.DJIWaypoint;
import dji.sdk.missionmanager.DJIWaypointMission;
import dji.sdk.products.DJIAircraft;
import dji.sdk.sdkmanager.DJISDKManager;

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
        System.out.println("onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        System.out.println("onPause");
        //unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.out.println("onDestroy");
        unregisterReceiver(mReceiver);
    }

    public void onReturn(View view) {
        System.out.println("onReturn");
        //this.finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        System.out.println("onCreate");

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
                        Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
                        Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                        Manifest.permission.READ_PHONE_STATE,
                }, 1);


        setContentView(R.layout.activity_main_ui);
        mHandler = new Handler(Looper.getMainLooper());

        //init map
        MultiDex.install(getApplicationContext());
        MapsInitializer.initialize(getApplicationContext());

        //set up vout
        vout = ((TextView) findViewById(R.id.textView4));
        vout.setGravity(Gravity.BOTTOM);
        vout.setMovementMethod(new ScrollingMovementMethod());

        //TODO fix instructions
        uiConsolePrint("DJI Phantom 3 Standard Aircraft Controller by:\nJames Norris and Jaiden Ferraccioli\n\nTo use this controller:\n1. Start with your device connected to a wireless signal other than that of the P3 remote.\n2. Start the remote, make sure the S1 switch is down all the way.\n3. Start the DJI Phantom 3 Standard aircraft and wait until the remote's power LED turns green.\n4. Connect your device to the P3 remote signal using your device's WiFi control.\n5. Wait until the flight controller has been initialized, then begin using the app.\n\n");

        Button flight = (Button) findViewById(R.id.toggleButton2);
        Button map = (Button) findViewById(R.id.map_v);
        System.out.println("onCreate 0.1");
        flight.setOnClickListener(new FlightStatusHandler(this, (ToggleButton) flight));
        System.out.println("onCreate 0.2");
        map.setOnClickListener(this);

        try {
            //register for GPS updates
            System.out.println("onCreate 0.3");
            GPSFollowHandler fh = new GPSFollowHandler(this);
            System.out.println("onCreate 0.4");
            LocationManager lm = ((LocationManager) getSystemService(Context.LOCATION_SERVICE));
            //lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0/*GPSFollowHandler.UPDATE_FREQUENCY_MS, .5F/*meter*/, fh);
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0/*GPSFollowHandler.UPDATE_FREQUENCY_MS, .5F/*meter*/, fh);

            System.out.println("onCreate 0.5");
        } catch (SecurityException ex) {
            ex.printStackTrace();
            uiConsolePrint(ex.toString() + "\n");
        }

        System.out.println("onCreate 1");

        sdkManager = DJISDKManager.getInstance();
        sdkManager.initSDKManager(this, mDJISDKManagerCallback);

        System.out.println("onCreate 2");

        startReceiver();
    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (getResultCode() == Activity.RESULT_OK) {
                System.out.println("mReciever");
                onProductConnectionChange();
            }
        }
    };

    private synchronized void onProductConnectionChange() {
        System.out.println("onProductConnectionChange");
        initFlightController();
        initMissionManager();
    }

    protected synchronized void initFlightController() {

        System.out.println("initFC");

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

                private long last = -1;

                @Override
                public void onResult(DJIFlightControllerCurrentState state) {
                    if (last != -1 || System.currentTimeMillis() - last < GPSFollowHandler.UPDATE_FREQUENCY_MS) {
                        return;//only update at the usual GPS update frequency
                    }

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

                    last = System.currentTimeMillis();
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
        System.out.println("updateDroneLoc");
        if (!checkGpsCoordinates(lat, lon)) {
            //already checked, they shouldn't be invalid
            uiConsolePrint("Internal location error!\n");
            return;
        }

        LatLng pos = new LatLng(lat, lon);
        //uiConsolePrint("Update drone location to " + pos + "\n");//this got annoying

        //Create MarkerOptions object
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(pos);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.mipmap.drone_aircraft));

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
        System.out.println("initMissionMan");
        if (mProduct == null || !mProduct.isConnected()) {

            setResultToToast("Product Not Connected");
            mMissionManager = null;

        } else {

            setResultToToast("Product Connected");
            mMissionManager = mProduct.getMissionManager();
            //mMissionManager.setMissionProgressStatusCallback(this);
            //mMissionManager.setMissionExecutionFinishedCallback(this);

        }
        System.out.println("initMissionMan 1");
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
        System.out.println("onClick");
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
            System.out.println("mDJISDKMC");
            Log.d(TAG, error == null ? "success" : error.getDescription());
            if (error == DJISDKError.REGISTRATION_SUCCESS) {
                sdkManager.startConnectionToProduct();
                uiConsolePrint("App Registered Successfully\n");
            } else {
                uiConsolePrint("App Failed to Register!\n");
            }
            Log.e("TAG", error.getDescription());
        }

        @Override
        public void onProductChanged(DJIBaseProduct oldProduct, DJIBaseProduct newProduct) {
            System.out.println("onProductChanged");
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
            System.out.println("onCompChanged");
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
            System.out.println("onProdConnChanged");
            uiConsolePrint((isConnected ? "Connected" : "Disconnected") + "\n");
            notifyStatusChange();
        }
    };

    private DJIBaseComponent.DJIComponentListener mDJIComponentListener = new DJIBaseComponent.DJIComponentListener() {
        @Override
        public void onComponentConnectivityChanged(boolean isConnected) {
            System.out.println("onCompConnChanged");
            notifyStatusChange();
        }
    };

    protected synchronized void notifyStatusChange() {
        System.out.println("notifyStatusChange");
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable, 500);
    }

    private Runnable updateRunnable = new Runnable() {

        @Override
        public void run() {
            System.out.println("updRunnable");
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

    public synchronized DJISDKManager getDJISDKManager() {
        return sdkManager;
    }

    protected synchronized void refreshReceiver() {
        unregisterReceiver(mReceiver);
        startReceiver();
    }

    protected synchronized void startReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);
    }

    protected synchronized void restartConnection() {
        sdkManager.stopConnectionToProduct();
        sdkManager.startConnectionToProduct();
    }
}

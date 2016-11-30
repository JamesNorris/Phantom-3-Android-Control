package com.term.jaiden.james.phantom3gpscommunication;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import dji.common.error.DJIError;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.common.error.DJISDKError;
import dji.sdk.missionmanager.DJIMissionManager;
import dji.sdk.sdkmanager.DJISDKManager;

public class MainUI extends AppCompatActivity {
    private static final String TAG = MainUI.class.getName();
    public static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";
    private static DJIBaseProduct mProduct;
    private Handler mHandler;
    private TextView vout;

    /*
    TODO implement return-home button
     */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_ui);
        //Initialize DJI SDK Manager
        System.out.println("Debug 1");
        mHandler = new Handler(Looper.getMainLooper());
        DJISDKManager.getInstance().initSDKManager(this, mDJISDKManagerCallback);
        System.out.println("Debug 2");

        vout = ((TextView) findViewById(R.id.textView4));
        vout.setMovementMethod(new ScrollingMovementMethod());
        System.out.println("Debug 3");

        ((Button) findViewById(R.id.toggleButton2)).setOnClickListener(new FlightStatusHandler(this));
        System.out.println("Debug 4");
        try {
            ((LocationManager) getSystemService(Context.LOCATION_SERVICE)).requestLocationUpdates(LocationManager.GPS_PROVIDER, GPSFollowHandler.UPDATE_FREQUENCY_MS, 1, new GPSFollowHandler(this));
        }catch (SecurityException ex) {
            ex.printStackTrace();
            vout.append(ex.getLocalizedMessage());
        }
    }

    private DJISDKManager.DJISDKManagerCallback mDJISDKManagerCallback = new DJISDKManager.DJISDKManagerCallback() {
        @Override
        public void onGetRegisteredResult(DJIError error) {
            Log.d(TAG, error == null ? "success" : error.getDescription());
            if(error == DJISDKError.REGISTRATION_SUCCESS) {
                DJISDKManager.getInstance().startConnectionToProduct();
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Register App Successful", Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Register App Failed! Please enter your App Key and check the network.", Toast.LENGTH_LONG).show();
                    }
                });
            }
            Log.e("TAG", error.toString());
        }

        @Override
        public void onProductChanged(DJIBaseProduct oldProduct, DJIBaseProduct newProduct) {
            mProduct = newProduct;
            if(mProduct != null) {
                mProduct.setDJIBaseProductListener(mDJIBaseProductListener);
            }
            notifyStatusChange();
        }
    };

    private DJIBaseProduct.DJIBaseProductListener mDJIBaseProductListener = new DJIBaseProduct.DJIBaseProductListener() {
        @Override
        public void onComponentChange(DJIBaseProduct.DJIComponentKey key, DJIBaseComponent oldComponent, DJIBaseComponent newComponent) {
            if(newComponent != null) {
                newComponent.setDJIComponentListener(mDJIComponentListener);
            }
            notifyStatusChange();
        }
        @Override
        public void onProductConnectivityChanged(boolean isConnected) {
            notifyStatusChange();
        }
    };
    private DJIBaseComponent.DJIComponentListener mDJIComponentListener = new DJIBaseComponent.DJIComponentListener() {
        @Override
        public void onComponentConnectivityChanged(boolean isConnected) {
            notifyStatusChange();
        }
    };
    private void notifyStatusChange() {
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

    protected static DJIMissionManager getMissionManager() {
        return mProduct.getMissionManager();
    }
}

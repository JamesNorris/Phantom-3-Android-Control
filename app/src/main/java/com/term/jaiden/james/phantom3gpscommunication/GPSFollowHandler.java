package com.term.jaiden.james.phantom3gpscommunication;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.widget.ToggleButton;

import dji.common.error.DJIError;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.missionmanager.DJIFollowMeMission;
import dji.sdk.missionmanager.DJIMissionManager;

//import android.os.health.PackageHealthStats;

/**
 * Created by James on 11/20/2016.
 */

public class GPSFollowHandler implements LocationListener {
    public static final long UPDATE_FREQUENCY_MS = 500;
    private long last_send = -1;
    private MainUI dialog;
    private DJIFollowMeMission mission;
    private ToggleButton button, button2;

    public GPSFollowHandler(MainUI dialog) {
        this.dialog = dialog;
        button = (ToggleButton) dialog.findViewById(R.id.toggleButton);
        button2 = (ToggleButton) dialog.findViewById(R.id.toggleButton2);
    }

    @Override
    public void onLocationChanged(Location location) {
        if ((!button2.isChecked()) || (!button.isChecked())) {
            mission = null;
            last_send = -1;
            return;//will only run when both buttons are toggled on
        }

        long cur_time = System.currentTimeMillis();

        if (last_send != -1 && (cur_time - last_send) < UPDATE_FREQUENCY_MS) {
            return;
        }

        final double lat = location.getLatitude();
        final double lon = location.getLongitude();
        final double alt = location.getAltitude();
        final float ber = location.getBearing();//0.0 to 360.0

        System.out.println("1");

        if (mission == null) {
            System.out.println("2");

            //init
            mission = new DJIFollowMeMission(lat, lon, (float) alt);

            System.out.println("3");

            //prepare
            DJIMissionManager manager = dialog.getMissionManager();

            if (manager == null) {
                dialog.uiConsolePrint("NULL MANAGER FOR GPS FOLLOW\n");
                return;
            }

            System.out.println("4");

            manager.prepareMission(mission, null, new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        dialog.uiConsolePrint(djiError.getDescription() + "\n");
                        return;
                    }
                    dialog.uiConsolePrint("Flight Command: Follow - Initialized\n");
                }
            });

            System.out.println("5");

            //execute
            manager.startMissionExecution(new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        dialog.uiConsolePrint(djiError.getDescription() + "\n");
                        return;
                    }
                    dialog.uiConsolePrint("Flight Command: Started\n");
                }
            });

            System.out.println("6");

        } else {

            System.out.println("7");

            mission.updateFollowMeCoordinate(lat, lon, (float) alt, new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        dialog.uiConsolePrint(djiError.getDescription() + "\n");
                        return;
                    }
                    dialog.uiConsolePrint("Following: <" + lat + ", " + lon + ", " + alt + ", " + ber + ">\n");
                }
            });

            System.out.println("8");

        }

        last_send = cur_time;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}
}

package com.term.jaiden.james.phantom3gpscommunication;

import android.app.Dialog;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.health.PackageHealthStats;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileWriter;

import dji.common.error.DJIError;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.missionmanager.DJIFollowMeMission;
import dji.sdk.missionmanager.DJIMissionManager;

/**
 * Created by James on 11/20/2016.
 */

public class GPSFollowHandler implements LocationListener {
    public static final long UPDATE_FREQUENCY_MS = 500;
    private long last_send = -1;
    private MainUI dialog;
    private DJIFollowMeMission mission;
    private ToggleButton button, button2;
    private TextView vout;

    public GPSFollowHandler(MainUI dialog) {
        this.dialog = dialog;
        button = (ToggleButton) dialog.findViewById(R.id.toggleButton);
        button2 = (ToggleButton) dialog.findViewById(R.id.toggleButton2);
        vout = ((TextView) dialog.findViewById(R.id.textView4));
    }

    @Override
    public void onLocationChanged(Location location) {
        long cur_time = System.currentTimeMillis();

        vout.append("1, ");

        if (button.isChecked()) {
            vout.append("2, ");
        }

        if (last_send != -1 && (cur_time - last_send) < UPDATE_FREQUENCY_MS) {
            return;
        }

        if ((!button2.isChecked()) || (!(button).isChecked())) {
            mission = null;
            last_send = -1;
            return;//will only run when both buttons are toggled on
        }

        final double lat = location.getLatitude();
        final double lon = location.getLongitude();
        final double alt = location.getAltitude();
        final float ber = location.getBearing();//0.0 to 360.0

        if (mission == null) {
            vout.append("3, ");

            //init
            mission = new DJIFollowMeMission(lat, lon, (float) alt);

            //prepare
            DJIMissionManager manager = dialog.getMissionManager();

            if (manager == null) {
                vout.append("NULL MANAGER FOR GPS FOLLOW\n");
            }

            manager.prepareMission(mission, null, new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        vout.append(djiError.getDescription() + "\n");
                        return;
                    }
                    vout.append("Flight Command: Follow - Initialized\n");
                }
            });

            //execute
            manager.startMissionExecution(new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        vout.append(djiError.getDescription() + "\n");
                        return;
                    }
                    vout.append("Flight Command: Started\n");
                }
            });

        } else {

            mission.updateFollowMeCoordinate(lat, lon, (float) alt, new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        ((TextView) dialog.findViewById(R.id.textView4)).append(djiError.getDescription() + "\n");
                        return;
                    }
                    vout.append("Following: <" + lat + ", " + lon + ", " + alt + ", " + ber + ">\n");
                }
            });

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

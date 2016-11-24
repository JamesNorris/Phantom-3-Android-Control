package com.term.jaiden.james.phantom3gpscommunication;

import android.app.Dialog;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.health.PackageHealthStats;
import android.widget.Button;

import java.io.File;
import java.io.FileWriter;

import dji.sdk.missionmanager.DJIFollowMeMission;

/**
 * Created by James on 11/20/2016.
 */

public class FollowMission implements LocationListener {
    private static final long UPDATE_FREQUENCY_MS = 500;
    private long last_send = -1;
    private Dialog dialog;
    private DJIFollowMeMission mission;
    private Button button;

    public FollowMission(Dialog dialog) {
        this.dialog = dialog;
        button = (Button) dialog.findViewById(R.id.toggleButton);
    }

    @Override
    public void onLocationChanged(Location location) {
        long cur_time = System.currentTimeMillis();
        if (last_send != -1 && cur_time - last_send < UPDATE_FREQUENCY_MS) {
            return;
        }

        if (!(button).isActivated()) {
            return;//will only run when the button is toggled on
        }

        double lat = location.getLatitude();
        double lon = location.getLongitude();
        double alt = location.getAltitude();
        float ber = location.getBearing();//0.0 to 360.0

        if (mission == null) {
            mission = new DJIFollowMeMission(lat, lon, (float) alt);
        } else {
            mission.updateFollowMeCoordinate(lat, lon, (float) alt, null);
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

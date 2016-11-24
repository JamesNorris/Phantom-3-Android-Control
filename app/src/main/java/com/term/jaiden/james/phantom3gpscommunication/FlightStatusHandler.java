package com.term.jaiden.james.phantom3gpscommunication;

import android.view.View;

import dji.sdk.base.DJIBaseComponent;
import dji.sdk.flightcontroller.DJIFlightController;
import dji.sdk.flightcontroller.DJIFlightControllerDelegate;


/**
 * Created by James on 11/22/2016.
 */

public class FlightStatusHandler implements View.OnClickListener {
    private boolean enabled = false;
    private DJIFlightController controller;

    public FlightStatusHandler() {
    }

    @Override
    public void onClick(View v) {
        enabled = !enabled;
        if (enabled) {
            controller.takeOff(null);
        } else {
            controller.autoLanding(null);
        }
    }
}

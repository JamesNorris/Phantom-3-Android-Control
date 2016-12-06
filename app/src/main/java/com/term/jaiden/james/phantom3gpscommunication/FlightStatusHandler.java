package com.term.jaiden.james.phantom3gpscommunication;

import android.app.Dialog;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

import dji.common.error.DJIError;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.flightcontroller.DJIFlightController;
import dji.sdk.flightcontroller.DJIFlightControllerDelegate;
import dji.sdk.missionmanager.DJICustomMission;
import dji.sdk.missionmanager.DJIMission;
import dji.sdk.missionmanager.DJIMissionManager;
import dji.sdk.missionmanager.DJIWaypointMission;
import dji.sdk.missionmanager.missionstep.DJIMissionStep;
import dji.sdk.missionmanager.missionstep.DJITakeoffStep;
import dji.sdk.products.DJIAircraft;


/**
 * Created by James on 11/22/2016.
 */

public class FlightStatusHandler implements View.OnClickListener {
    private boolean enabled = false;
    private MainUI dialog;
    private DJIMission mission;
    private TextView vout;

    public FlightStatusHandler(MainUI dialog) {
        this.dialog = dialog;
        vout = ((TextView) dialog.findViewById(R.id.textView4));
    }

    @Override
    public void onClick(View v) {
        DJIBaseProduct product = dialog.getBaseProduct();
        if (product == null) {
            vout.append("Product null!\n");
            return;
        }

        DJIFlightController fc = ((DJIAircraft) product).getFlightController();
        if (fc == null) {
            vout.append("Flight Controller null!\n");
            return;
        }

        //check connectivity
        if (!dialog.getMissionManager().isConnected()) {
            System.out.println("NOT CONNECTED");
            return;
        }

        //toggle
        enabled = !enabled;

        //init
        if (enabled) {

            //take off
            System.out.println("[Flight Handler] Taking Off");
            vout.append("[Flight Handler] Take Off\n");

            fc.takeOff(new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        vout.append(djiError.getDescription() + "\n");
                        return;
                    }
                    vout.append("Flight Command: Take Off - Initialized\n");
                }
            });

        } else {

            //land
            System.out.println("[Flight Handler] Landing");
            vout.append("[Flight Handler] Landing\n");

            fc.autoLanding(new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        vout.append(djiError.getDescription() + "\n");
                        return;
                    }
                    vout.append("Flight Command: Land - Initialized\n");
                }
            });
        }
    }
}

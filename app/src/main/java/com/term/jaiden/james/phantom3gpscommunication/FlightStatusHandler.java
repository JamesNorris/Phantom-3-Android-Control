package com.term.jaiden.james.phantom3gpscommunication;

import android.app.Dialog;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

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
    private ToggleButton button;

    public FlightStatusHandler(MainUI dialog, ToggleButton button) {
        this.dialog = dialog;
        this.button = button;
    }

    @Override
    public void onClick(View v) {
        DJIBaseProduct product = dialog.getBaseProduct();
        if (product == null) {
            dialog.uiConsolePrint("Product null!\n");
            button.setChecked(enabled);
            return;
        }

        DJIFlightController fc = ((DJIAircraft) product).getFlightController();
        if (fc == null) {
            dialog.uiConsolePrint("Flight Controller null! Trying init...\n");
            dialog.initFlightController();
            button.setChecked(enabled);
            return;
        }

        //check connectivity
        if (!dialog.getMissionManager().isConnected()) {
            System.out.println("Mission Manager null! Trying init...");
            dialog.initMissionManager();
            button.setChecked(enabled);
            return;
        }

        //toggle
        enabled = !enabled;

        //init
        if (enabled) {

            //take off
            System.out.println("[Flight Handler] Taking Off");
            dialog.uiConsolePrint("[Flight Handler] Take Off\n");

            fc.takeOff(new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        dialog.uiConsolePrint(djiError.getDescription() + "\n");
                        return;
                    }
                    dialog.uiConsolePrint("Flight Command: Take Off - Initialized\n");
                }
            });

        } else {

            //land
            System.out.println("[Flight Handler] Landing");
            dialog.uiConsolePrint("[Flight Handler] Landing\n");

            fc.autoLanding(new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        dialog.uiConsolePrint(djiError.getDescription() + "\n");
                        return;
                    }
                    dialog.uiConsolePrint("Flight Command: Land - Initialized\n");
                }
            });

        }
    }
}

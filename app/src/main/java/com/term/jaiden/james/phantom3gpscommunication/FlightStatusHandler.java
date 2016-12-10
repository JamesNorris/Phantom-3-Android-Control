package com.term.jaiden.james.phantom3gpscommunication;

import android.view.View;
import android.widget.ToggleButton;

import dji.common.error.DJIError;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.flightcontroller.DJIFlightController;
import dji.sdk.products.DJIAircraft;


/**
 * Created by James on 11/22/2016.
 */

public class FlightStatusHandler implements View.OnClickListener {
    private boolean enabled = false;
    private MainUI dialog;
    private ToggleButton button;

    private final DJICommonCallbacks.DJICompletionCallback toggleFlyCallback = new DJICommonCallbacks.DJICompletionCallback() {
        @Override
        public void onResult(DJIError djiError) {
            if (djiError == null) {
                return;//TODO would the button ever be wrong?
            }

            //fix broken connection
            String desc = djiError.getDescription().trim();
            if (desc.equals("Not Support")) {
                dialog.uiConsolePrint("Broken connection!\nPerhaps the drone is at low battery?");
                dialog.refreshReceiver();
                return;
            }

            //prevent mistoggled button
            dialog.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    boolean check = false;
                    DJIAircraft ac = ((DJIAircraft) dialog.getBaseProduct());
                    DJIFlightController fc = null;
                    if (ac != null) {
                        fc = ac.getFlightController();
                    }
                    if (fc != null) {
                        check = fc.getCurrentState().areMotorsOn();
                    }
                    button.setChecked(check);
                    enabled = check;
                }
            });

            dialog.uiConsolePrint(desc + "\n");
        }
    };

    public FlightStatusHandler(MainUI dialog, ToggleButton button) {
        this.dialog = dialog;
        this.button = button;
    }

    @Override
    public void onClick(View v) {
        DJIBaseProduct product = dialog.getBaseProduct();
        if (product == null) {
            dialog.uiConsolePrint("Product null!\nAre you sure the devices are connected?\n");
            //dialog.getSDKManager().startConnectionToProduct();
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
            System.out.println("[Flight Handler] Take Off");
            dialog.uiConsolePrint("Flight Command: Take Off\n");

            fc.takeOff(toggleFlyCallback);

        } else {

            //land
            System.out.println("[Flight Handler] Land");
            dialog.uiConsolePrint("Flight Command: Land\n");

            fc.autoLanding(toggleFlyCallback);

        }

        button.setChecked(enabled);
    }
}

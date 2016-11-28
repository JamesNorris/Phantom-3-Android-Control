package com.term.jaiden.james.phantom3gpscommunication;

import android.app.Dialog;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

import dji.common.error.DJIError;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.flightcontroller.DJIFlightController;
import dji.sdk.flightcontroller.DJIFlightControllerDelegate;
import dji.sdk.missionmanager.DJICustomMission;
import dji.sdk.missionmanager.missionstep.DJIMissionStep;
import dji.sdk.missionmanager.missionstep.DJITakeoffStep;


/**
 * Created by James on 11/22/2016.
 */

public class FlightStatusHandler implements View.OnClickListener {
    private boolean enabled = false;
    private Dialog dialog;
    private DJICustomMission mission;

    public FlightStatusHandler(Dialog dialog) {
        this.dialog = dialog;
    }

    @Override
    public void onClick(View v) {
        enabled = !enabled;
        if (enabled) {
            mission = new DJICustomMission(new ArrayList<DJIMissionStep>() {{new DJITakeoffStep(new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError == null) {
                        ((TextView) dialog.findViewById(R.id.textView4)).append("Flight Command: Take Off - Completed\n");
                    }
                }
            });}});
            mission.addMissionToTheQueue();
            //controller.takeOff(null);
        } else {
            //controller.autoLanding(null);
        }
    }
}

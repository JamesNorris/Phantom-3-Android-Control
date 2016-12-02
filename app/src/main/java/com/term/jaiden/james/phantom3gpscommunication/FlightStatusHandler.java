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
import dji.sdk.missionmanager.DJIMission;
import dji.sdk.missionmanager.DJIMissionManager;
import dji.sdk.missionmanager.DJIWaypointMission;
import dji.sdk.missionmanager.missionstep.DJIMissionStep;
import dji.sdk.missionmanager.missionstep.DJITakeoffStep;


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
        if(enabled) {
            System.out.println("[Flight Handler] Landing");
            vout.append("[Flight Handler] Landing\n");
        }
        if(!enabled){
            System.out.println("[Flight Handler] Landing");
            vout.append("[Flight Handler] Take Off\n");
        }
        enabled = !enabled;
        //init
        if (enabled) {


            mission = new DJICustomMission(new ArrayList<DJIMissionStep>() {{new DJITakeoffStep(new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        vout.append(djiError.getDescription() + "\n");
                        return;
                    }
                    vout.append("Flight Command: Take Off - Initialized\n");
                }
            });}});
            ((DJICustomMission) mission).addMissionToTheQueue();
            //controller.takeOff(null);


        } else {


            mission = new DJIWaypointMission() {
                {
                    finishedAction = DJIWaypointMission.DJIWaypointMissionFinishedAction.AutoLand;
                }};
            vout.append("Flight Command: Land - Initialized\n");
            //controller.autoLanding(null);


        }

        //prepare
        DJIMissionManager manager = MainUI.getMissionManager();
        manager.prepareMission(mission, null, new DJICommonCallbacks.DJICompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError != null) {
                    vout.append(djiError.getDescription() + "\n");
                    return;
                }
                vout.append("Flight Command: Prepared\n");
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
    }
}

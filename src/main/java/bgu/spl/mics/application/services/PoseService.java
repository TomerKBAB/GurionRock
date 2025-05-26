package bgu.spl.mics.application.services;

import bgu.spl.mics.application.messages.*;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.objects.*;


/**
 * PoseService is responsible for maintaining the robot's current pose (position and orientation)
 * and broadcasting PoseEvents at every tick.
 */
public class PoseService extends MicroService {
    private final GPSIMU gpsimu;
    Pose currentPose;
    /**
     * Constructor for PoseService.
     *
     * @param gpsimu The GPSIMU object that provides the robot's pose data.
     */
    public PoseService(GPSIMU gpsimu) {
        super("GPSIMU-MicroService");
        this.gpsimu = gpsimu;
    }

    /**
     * Initializes the PoseService.
     * Subscribes to TickBroadcast and sends PoseEvents at every tick based on the current pose.
     */
    @Override
    protected void initialize() {
        subscribeBroadcast(TickBroadcast.class, (TickBroadcast tick) -> {
            gpsimu.setCurrentTick(tick.getTick());
            currentPose = gpsimu.getCurrentPose();
            if (currentPose != null){
                sendEvent(new PoseEvent(currentPose));
            }
        });
        subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast crashed) -> terminate());
        subscribeBroadcast(TerminatedBroadcast.class, terminatedBroadcast -> terminate());

    }
}
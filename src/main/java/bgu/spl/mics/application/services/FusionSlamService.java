package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.*;
import bgu.spl.mics.application.objects.FusionSlam;
import bgu.spl.mics.application.objects.Pose;
import bgu.spl.mics.application.objects.TrackedObject;

import java.util.List;
import java.util.logging.Level;

/**
 * FusionSlamService integrates data from multiple sensors to build and update
 * the robot's global map.
 * 
 * This service receives TrackedObjectsEvents from LiDAR workers and PoseEvents from the PoseService,
 * transforming and updating the map with new landmarks.
 */
public class FusionSlamService extends MicroService {
    private final FusionSlam fusionSlam;

    /**
     * Constructor for FusionSlamService.
     *
     * @param fusionSlam The FusionSLAM object responsible for managing the global map.
     */
    public FusionSlamService(FusionSlam fusionSlam) {
        super("FusionSlamService");
        this.fusionSlam = fusionSlam;
    }

    /**
     * Initializes the FusionSlamService.
     * Registers the service to handle TrackedObjectsEvents, PoseEvents, and TickBroadcasts,
     * and sets up callbacks for updating the global map.
     */
    @Override
    protected void initialize() {
        subscribeEvent(TrackedObjectsEvent.class, trackedObjectsEvent -> {
            List<TrackedObject> trackedObjects = trackedObjectsEvent.getTrackedObjects();
            for (TrackedObject object : trackedObjects) {
                fusionSlam.updateLandMark(object);
            }
        });

        subscribeEvent(PoseEvent.class, poseEvent -> {
            fusionSlam.addPose(poseEvent.getPose());
        });

        subscribeBroadcast(TickBroadcast.class, tick -> {
            fusionSlam.tickEvent();
        });

        subscribeBroadcast(TerminatedBroadcast.class, terminated -> {
            terminate();
        });

        subscribeBroadcast(CrashedBroadcast.class, crashed -> {
            terminate();
        });
    }
}

package bgu.spl.mics.application.services;

import java.util.List;
import java.util.concurrent.Future;

import javax.sound.midi.Track;

import bgu.spl.mics.Event;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.*;
import bgu.spl.mics.application.objects.*;

/**
 * LiDarService is responsible for processing data from the LiDAR sensor and
 * sending TrackedObjectsEvents to the FusionSLAM service.
 * 
 * This service interacts with the LiDarTracker object to retrieve and process
 * cloud point data and updates the system's StatisticalFolder upon sending its
 * observations.
 */
public class LiDarService extends MicroService {


    private final LiDarWorkerTracker liDarWorkerTracker;
    private final LiDarDataBase liDarDataBase;
    private final StatisticalFolder statisticalFolder;
    private int currentTick;

    /**
     * Constructor for LiDarService.
     *
     * @param liDarWorkerTracker The LiDAR tracker object that this service will use to process data.
     */
    public LiDarService(LiDarWorkerTracker liDarWorkerTracker) {
        super("LiDarService-" + liDarWorkerTracker.getId());
        this.liDarWorkerTracker = liDarWorkerTracker;
        this.liDarDataBase = LiDarDataBase.getInstance();
        this.statisticalFolder = StatisticalFolder.getInstance();
        this.currentTick = 0;
        SensorMonitor.getInstance().addSensor("LiDarWorkerTracker " + liDarWorkerTracker.getId());
    }

    /**
     * Initializes the LiDarService.
     * Registers the service to handle DetectObjectsEvents and TickBroadcasts,
     * and sets up the necessary callbacks for processing data.
     */
    @Override
    protected void initialize() {
            subscribeEvent(DetectObjectsEvent.class, event -> {
            List<DetectedObject> detectedObjects = event.getDetectedObjects();
            List<TrackedObject> trackedObjects = liDarWorkerTracker.processDetectedObjects(detectedObjects, liDarDataBase, event.getTick(), currentTick);
            statisticalFolder.incrementTrackedObjects(detectedObjects.size());
            if (!trackedObjects.isEmpty()) {
                sendTrackedEvent(trackedObjects);
            }
        });

         subscribeBroadcast(TickBroadcast.class, tickBroadcast -> {
            currentTick += 1;
            List<TrackedObject> trackedObjects = liDarWorkerTracker.handleTick(tickBroadcast.getTick());
            if(liDarWorkerTracker.getStatus() == STATUS.ERROR) {
                statisticalFolder.setError(true);
                statisticalFolder.setErrorDescription("Lidar " + liDarWorkerTracker.getId() + " Disconnected");
                statisticalFolder.setFaultySensor("LidarWorkerTracker");
                CrashedBroadcast crashedBroadcast = new CrashedBroadcast("Lidar " + liDarWorkerTracker.getId() + " Disconnected");
                sendBroadcast(crashedBroadcast);
            }
            else if (!trackedObjects.isEmpty()) {
                sendTrackedEvent(trackedObjects);
            }
            else if (liDarWorkerTracker.getStatus() == STATUS.DOWN) {
                SensorMonitor.getInstance().removeSensor("LiDarWorkerTracker " + liDarWorkerTracker.getId());
                terminate();
        }});

        subscribeBroadcast(TerminatedBroadcast.class, terminatedBroadcast -> {
            terminate();
        });

        subscribeBroadcast(CrashedBroadcast.class, crashedBroadcast -> {
            terminate();
        });

    }

    private void sendTrackedEvent(List<TrackedObject> trackedObjects) {
        TrackedObjectsEvent trackedObjectsEvent = new TrackedObjectsEvent(trackedObjects);
        sendEvent(trackedObjectsEvent);
    }
}

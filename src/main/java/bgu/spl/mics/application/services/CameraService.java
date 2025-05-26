package bgu.spl.mics.application.services;

import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.SensorMonitor;
import bgu.spl.mics.Future;
import java.util.ArrayList;
import bgu.spl.mics.Event;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.*;
import bgu.spl.mics.application.messages.*;
import bgu.spl.mics.application.objects.DetectedObject;
import bgu.spl.mics.application.objects.Camera;
import bgu.spl.mics.application.objects.StampedDetectedObjects;
import bgu.spl.mics.application.objects.StatisticalFolder;
import java.util.HashMap;

/**
 * CameraService is responsible for processing data from the camera and
 * sending DetectObjectsEvents to LiDAR workers.
 * 
 * This service interacts with the Camera object to detect objects and updates
 * the system's StatisticalFolder upon sending its observations.
 */
public class CameraService extends MicroService {
    
    private final Camera camera;
    private final StatisticalFolder statisticalFolder;

    /**
     * Constructor for CameraService.
     *
     * @param camera The Camera object that this service will use to detect objects.
     */
    public CameraService(Camera camera) {
        super("CameraService-" + camera.getId());
        this.camera = camera;
        SensorMonitor.getInstance().addSensor("Camera " + camera.getId());
        this.statisticalFolder = StatisticalFolder.getInstance();
    }

    /**
     * Initializes the CameraService.
     * Registers the service to handle TickBroadcasts and sets up callbacks for sending
     * DetectObjectsEvents.
     */
    @Override
    protected void initialize() {
        subscribeBroadcast(TickBroadcast.class, (TickBroadcast tick) -> {
            processDetections(tick.getTick());
            checkFinished();
        });
        subscribeBroadcast(TerminatedBroadcast.class, (TerminatedBroadcast terminated) -> {
            terminate();
        });
        subscribeBroadcast(CrashedBroadcast.class, (CrashedBroadcast crashed) -> {
            terminate();
        });
    }

    private void processDetections(int tick) {
        String errorMSG = camera.checkForError(tick);
        if (errorMSG != null) {
            camera.setStatus(STATUS.ERROR);
            statisticalFolder.setError(true);
            statisticalFolder.setErrorDescription(errorMSG);
            statisticalFolder.setFaultySensor("Camera " + camera.getId());
            sendBroadcast(new CrashedBroadcast(errorMSG));
        }
        else {
            int numDetections = camera.setToBeSentObjects(tick);
            if(numDetections > 0) 
                statisticalFolder.incrementDetectedObjects(numDetections);
            ArrayList<DetectedObject> detections = camera.getDetectedObjectsToSend(tick);
            if (detections.size() > 0) {
                DetectObjectsEvent event = new DetectObjectsEvent(detections, tick - camera.getFrequency());
                sendEvent(event);
            } 
        }
    }
    private void checkFinished() {
        if(camera.getRemainingObjects() == 0) {
            camera.setStatus(STATUS.DOWN);
            SensorMonitor.getInstance().removeSensor("Camera " + camera.getId());
            terminate();
        }
    }

}






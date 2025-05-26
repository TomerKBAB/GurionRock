package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * LiDarWorkerTracker is responsible for managing a LiDAR worker.
 * It processes DetectObjectsEvents and generates TrackedObjectsEvents by using data from the LiDarDataBase.
 * Each worker tracks objects and sends observations to the FusionSlam service.
 */
public class LiDarWorkerTracker {

    private int id;
    private int frequency;
    private STATUS status;
    private int trackedObjectsCounter;
    //Not clear what is the lastTrackedObjct in case of error. last sent to fusion? or last received from camera? asked in forum with no asnwer.
    private ArrayList<TrackedObject> lastTrackedObjects;
    private List<PendingDetection> toBeTrackedObjects;
    public LiDarWorkerTracker(int id, int frequency) {
        this.id = id;
        this.frequency = frequency;
        this.trackedObjectsCounter = 0; 
        this.lastTrackedObjects = new ArrayList<>();
        this.toBeTrackedObjects = new ArrayList<>();
        this.status = STATUS.UP;
    }
    public List<TrackedObject> processDetectedObjects(List<DetectedObject> detectedObjects, LiDarDataBase liDarDataBase, int timeOfDetection, int currentTick) {
        List<TrackedObject> trackedObjects = new ArrayList<>();
        if (status == STATUS.UP) {
            int requiredTick = timeOfDetection + frequency;
            if (currentTick >= requiredTick) {
                for (DetectedObject detectedObject : detectedObjects) {
                    if (detectedObject.getId().equals("ERROR")) {
                        status = STATUS.ERROR;
                        throw new IllegalStateException("LiDAR encountered an ERROR object. Status set to ERROR.");
                    }
                }
                trackedObjects.addAll(processDetectedObjectHelper(detectedObjects, liDarDataBase, timeOfDetection));
                lastTrackedObjects.clear();
                lastTrackedObjects.addAll(trackedObjects);
                trackedObjectsCounter += trackedObjects.size();
            } 
            else if (!isAlreadyTracked(detectedObjects, timeOfDetection)) {
                    toBeTrackedObjects.add(new PendingDetection(detectedObjects, timeOfDetection));
            }
        }
        return trackedObjects;
    }

    public List<TrackedObject> handleTick(int currentTick) {
        List<TrackedObject> trackedObjects = new ArrayList<>();
        if (status == STATUS.UP) {
            if (checkForErrorInDatabase(currentTick)) {
                status = STATUS.ERROR;
            }

            else if(checkIfDone(currentTick)) {
                status = STATUS.DOWN;
            }

            else {
                Iterator<PendingDetection> iterator = toBeTrackedObjects.iterator();
                while (iterator.hasNext()) {
                    PendingDetection pending = iterator.next();
                    List<TrackedObject> tmpTrackedObjects = processDetectedObjects(pending.getDetectedObject(), LiDarDataBase.getInstance(), pending.getTimeOfDetection(), currentTick);
                    if (!tmpTrackedObjects.isEmpty()) {
                        trackedObjects = tmpTrackedObjects;
                        lastTrackedObjects.clear();
                        lastTrackedObjects.addAll(trackedObjects);
                        iterator.remove();
                        break;
                    }
                }
            }
        }
        return trackedObjects;
    }

    private boolean checkForErrorInDatabase(int currentTick) {
        LiDarDataBase database = LiDarDataBase.getInstance();
        List<CloudPoint> errorCloudPoints = database.getCloudPoints("ERROR", currentTick);
        boolean hasError = errorCloudPoints != null && !errorCloudPoints.isEmpty();
        return hasError;
    }

    private List<TrackedObject> processDetectedObjectHelper(List<DetectedObject> detectedObjects, LiDarDataBase liDarDataBase, int timeOfDetection) {
        List<TrackedObject> result = new ArrayList<>();
        for (DetectedObject detectedObject : detectedObjects) {
            if (detectedObject.getId().equals("ERROR")) {
                status = STATUS.ERROR;
                throw new IllegalStateException("LiDAR encountered an ERROR object. Status set to ERROR.");
            }
            List<CloudPoint> cloudPoints = liDarDataBase.getCloudPoints(detectedObject.getId(), timeOfDetection);
            result.add(new TrackedObject(detectedObject.getId(), timeOfDetection, detectedObject.getDescription(), cloudPoints));
        }
        return result;
    }

    private boolean isAlreadyTracked(List<DetectedObject> detectedObjects, int timeOfDetection) {
        for (PendingDetection pending : toBeTrackedObjects) {
            if (pending.getTimeOfDetection() == timeOfDetection && pending.getDetectedObject().equals(detectedObjects)) {
                return true;
            }
        }
        return false;
    }

    public int getId() {
        return id;
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

    public STATUS getStatus() {
        return status;
    }

    public List<TrackedObject> getLastTrackedObjects() {
        return new ArrayList<>(lastTrackedObjects);
    }
    public boolean checkIfDone(int currentTick) {
        return (LiDarDataBase.getInstance().getSize() == trackedObjectsCounter);
    }

    private static class PendingDetection {
        private final List<DetectedObject> detectedObject;
        private final int timeOfDetection;

        public PendingDetection(List<DetectedObject> detectedObject, int timeOfDetection) {
            this.detectedObject = detectedObject;
            this.timeOfDetection = timeOfDetection;
        }

        public List<DetectedObject> getDetectedObject() {
            return detectedObject;
        }

        public int getTimeOfDetection() {
            return timeOfDetection;
        }
    }
}

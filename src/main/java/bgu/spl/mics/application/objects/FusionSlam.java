package bgu.spl.mics.application.objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;




/**
 * Manages the fusion of sensor data for simultaneous localization and mapping (SLAM).
 * Combines data from multiple sensors (e.g., LiDAR, camera) to build and update a global map.
 * Implements the Singleton pattern to ensure a single instance of FusionSlam exists.
 */
public class FusionSlam {
    

    private final List<LandMark> landMarks;
    private final List<Pose> poses;      
    private final List<TrackedObject> toBeTrackedObjects; 


    // Singleton instance holder
    private static class FusionSlamHolder { 
        private static final FusionSlam fusionSlam = new FusionSlam();
    }

    private FusionSlam() {
        this.landMarks = new ArrayList<>(); 
        this.poses = new ArrayList<>(); 
        this.toBeTrackedObjects = new ArrayList<>();
    }
    public synchronized void updateLandMark(TrackedObject object) {
        Pose pose = findPoseByTime(object.getTime());
        if (pose == null) {
            toBeTrackedObjects.add(object);
        } else {
            LandMark existingLandMark = findLandMarkById(object.getId());
            if (existingLandMark == null) {
                LandMark newLandMark = new LandMark(object.getId(), object.getDescription(), transformCoordinates(object.getCoordinates(), pose));
                landMarks.add(newLandMark);
                StatisticalFolder.getInstance().incrementLandmarks(1);
            } else {
                List<CloudPoint> updatedCoordinates = averageCoordinates(existingLandMark.getCoordinates(), transformCoordinates(object.getCoordinates(), pose));
                existingLandMark.setCoordinates(updatedCoordinates);
            }
        }
    }

    public void tickEvent() {
        synchronized (toBeTrackedObjects) {
            if (!toBeTrackedObjects.isEmpty()) {
                Iterator<TrackedObject> it = toBeTrackedObjects.iterator();
                while (it.hasNext()) {
                    TrackedObject object = it.next();
                    if (findPoseByTime(object.getTime()) != null) {
                        it.remove(); // Safe removal via Iterator
                        updateLandMark(object);
                    }
                }
            }
        }
    }
    private List<CloudPoint> transformCoordinates(List<CloudPoint> coordinates, Pose pose) {
        List<CloudPoint> transformed = new ArrayList<>();
        double yawRadians = Math.toRadians(pose.getYaw());
        for (CloudPoint point : coordinates) {
            // 2D rotation matrix formula
            double transformedX = pose.getX() + (point.getX() * Math.cos(yawRadians)) - (point.getY() * Math.sin(yawRadians));
            double transformedY = pose.getY() + (point.getX() * Math.sin(yawRadians)) + (point.getY() * Math.cos(yawRadians));
            transformed.add(new CloudPoint(transformedX, transformedY));
    }
    return transformed;
    }
    private List<CloudPoint> averageCoordinates(List<CloudPoint> existingCoordinates, List<CloudPoint> newCoordinates) {
        List<CloudPoint> averaged = new ArrayList<>();
        if(existingCoordinates.size() != newCoordinates.size()) {
            padCoordinates(existingCoordinates, newCoordinates);  // pad coordinates if needed
        }
        for (int i = 0; i < existingCoordinates.size(); i++) {
            CloudPoint existing = existingCoordinates.get(i);
            CloudPoint newPoint = newCoordinates.get(i);
            double avgX = (existing.getX() + newPoint.getX()) / 2;
            double avgY = (existing.getY() + newPoint.getY()) / 2;
            averaged.add(new CloudPoint(avgX, avgY));
        }
        return averaged;
    }
    private void padCoordinates(List<CloudPoint> existingCoordinates, List<CloudPoint> newCoordinates) {
        int existListSize = existingCoordinates.size();
        int newListSize = newCoordinates.size();
        if (existListSize > newListSize) {  
            for (int i = 0; i < existListSize - newListSize; i++)  {
                newCoordinates.add(new CloudPoint(0, 0));
            }
        }
        else {
            for (int i = 0; i < newListSize - existListSize; i++)  {
                existingCoordinates.add(new CloudPoint(0, 0));
            }
        }
    }

    public synchronized void writeToJson(String outputPath) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        StatisticalFolder stats = StatisticalFolder.getInstance();

        Map<String, Object> output= new LinkedHashMap<>();
        output.put("systemRuntime", stats.getSystemRuntime());
        output.put("numDetectedObjects", stats.getNumDetectedObjects());
        output.put("numTrackedObjects", stats.getNumTrackedObjects());
        output.put("numLandmarks", stats.getNumLandmarks());
        List<String> landmarksAsStrings = landMarks.stream().map(LandMark::toString).collect(Collectors.toList()); //Made to maked output in 1 line as exemple files
        output.put("landMarks", landmarksAsStrings);
        try (FileWriter writer = new FileWriter(outputPath)) {
            gson.toJson(output, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void writeErrorToJson(String outputPath, Map<String, List<StampedDetectedObjects>> lastCameraFrames,  Map<String, List<TrackedObject>> lastLiderFrames) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        StatisticalFolder stats = StatisticalFolder.getInstance();

        Map<String, Object> statistics = new LinkedHashMap<>();
        statistics.put("systemRuntime", stats.getSystemRuntime());
        statistics.put("numDetectedObjects", stats.getNumDetectedObjects());
        statistics.put("numTrackedObjects", stats.getNumTrackedObjects());
        statistics.put("numLandmarks", stats.getNumLandmarks());
            

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("error", stats.getErrorDescription());
        output.put("faultySensor", stats.getFaultySensor());
        output.put("lastCameraFrames", lastCameraFrames);
        output.put("lastLidarFrames", lastLiderFrames);
        output.put("poses", poses);
        output.put("statistics", statistics);
        List<String> landmarksAsStrings = landMarks.stream().map(LandMark::toString).collect(Collectors.toList()); //Made to maked output in 1 line as exemple files
        output.put("landMarks", landmarksAsStrings);
        try (FileWriter writer = new FileWriter(outputPath)) {
            gson.toJson(output, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // ClearData only used for testing purposes
    public synchronized void clearData() {
        landMarks.clear();
        poses.clear();
        toBeTrackedObjects.clear();
    }
    public List<TrackedObject> getPendingTrackedObjects() {
        return toBeTrackedObjects;
    }
    public static FusionSlam getInstance() {
        return FusionSlamHolder.fusionSlam;
    }

    public synchronized void updateCurrentPose(Pose pose) {
        poses.add(pose);
    }

    public synchronized Pose getCurrentPose() {
        if (poses.isEmpty()) {
            return null;
        }
        return poses.get(poses.size() - 1);
    }

    public synchronized List<Pose> getPoses() {
        return new ArrayList<>(poses);
    }


    

    private LandMark findLandMarkById(String id) {
        for (LandMark landMark : landMarks) {
            if (landMark.getId().equals(id)) {
                return landMark;
            }
        }
        return null;
    }

    private Pose findPoseByTime(int time) {
        for (Pose pose : poses) {
            if (pose.getTime() == time) {
                return pose;
            }
        }
        return null;
    }



    public synchronized List<LandMark> getLandMarks() {
        return new ArrayList<>(landMarks);
    }

    public void addPose(Pose pose) {
        this.poses.add(pose);
    }


}

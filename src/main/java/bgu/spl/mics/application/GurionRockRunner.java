package bgu.spl.mics.application;

import bgu.spl.mics.application.objects.*;
import bgu.spl.mics.application.services.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.io.FileReader;
import java.util.Map;
import java.util.LinkedHashMap;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GurionRockRunner {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Error: Configuration file path is required as the first argument.");
            return;
        }
        String configFilePath = args[0];
        System.out.println("Starting GurionRock Simulation...");

        List<Thread> threads = new ArrayList<>();
        try {
            Path configPath = Paths.get(configFilePath);
            Path directoryPath = configPath.getParent();
            
            // Parse configuration
            JsonObject config = parseConfiguration(configFilePath);

            // Initialize components
            SystemComponents components = initializeComponents(config, directoryPath.toString());

            // Initialize and start services
            initializeServices(components, threads);

            // Wait for threads to complete
            waitForThreads(threads);

            // Write output JSON
            if (!StatisticalFolder.getInstance().isError()) {
                FusionSlam.getInstance().writeToJson(directoryPath + "/output_file.json");
                System.out.println("Simulation completed successfully.");
            }
            else {
                handleError(directoryPath + "/outputError.json", components);
                System.out.println("Simulation ended with error.");
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
            handleError(e, configFilePath);
        }
    }

    private static JsonObject parseConfiguration(String configFilePath) throws Exception {
        try (FileReader reader = new FileReader(configFilePath)) {
            Gson gson = new Gson();
            return gson.fromJson(reader, JsonObject.class);
        }
    }

    private static SystemComponents initializeComponents(JsonObject config, String directoryPath) {
        SystemComponents components = new SystemComponents();

        // Initialize Cameras
        JsonObject camerasJson = config.getAsJsonObject("Cameras");
        JsonArray camerasConfigurationsArray = camerasJson.getAsJsonArray("CamerasConfigurations");
        String cameraDataPath = directoryPath + "/" + camerasJson.get("camera_datas_path").getAsString();

        for (JsonElement cameraElement : camerasConfigurationsArray) {
            JsonObject cameraConfig = cameraElement.getAsJsonObject();
            int id = cameraConfig.get("id").getAsInt();
            int frequency = cameraConfig.get("frequency").getAsInt();
            String description = cameraConfig.get("camera_key").getAsString();

            components.cameras.add(new Camera(frequency, id, description, cameraDataPath));
        }

        // Initialize LiDAR DataBase
        components.lidarDataBase = LiDarDataBase.getInstance();
        String lidarDataPath = config.getAsJsonObject("LiDarWorkers").get("lidars_data_path").getAsString();
        components.lidarDataBase.loadFromJson(directoryPath + "/" + lidarDataPath);

        // Initialize GPS
        components.gpsIMU = new GPSIMU();
        String gpsImuPath = config.get("poseJsonFile").getAsString();
        components.gpsIMU.readFromJson(directoryPath + "/" + gpsImuPath);

        // Initialize LiDarWorkers
        JsonArray lidarWorkersArray = config.getAsJsonObject("LiDarWorkers").getAsJsonArray("LidarConfigurations");
        for (JsonElement lidarElement : lidarWorkersArray) {
            JsonObject lidarConfig = lidarElement.getAsJsonObject();
            int id = lidarConfig.get("id").getAsInt();
            int frequency = lidarConfig.get("frequency").getAsInt();

            LiDarWorkerTracker lidarWorker = new LiDarWorkerTracker(id, frequency);
            components.lidarWorkers.add(lidarWorker);
        }

        // Initialize other components
        components.fusionSlam = FusionSlam.getInstance();
        components.timeService = new TimeService(config.get("TickTime").getAsInt(), config.get("Duration").getAsInt());
        components.poseService = new PoseService(components.gpsIMU);
        components.fusionSlamService = new FusionSlamService(components.fusionSlam);

        return components;
    }

    private static void initializeServices(SystemComponents components, List<Thread> threads) {
        for (Camera camera : components.cameras) {
            Thread cameraThread = new Thread(new CameraService(camera));
            threads.add(cameraThread);
            cameraThread.start();
        }

        for (LiDarWorkerTracker lidarWorker : components.lidarWorkers) {
            Thread lidarThread = new Thread(new LiDarService(lidarWorker));
            threads.add(lidarThread);
            lidarThread.start();
        }


        Thread poseThread = new Thread(components.poseService);
        Thread timeThread = new Thread(components.timeService);
        Thread fusionThread = new Thread(components.fusionSlamService);

        threads.add(poseThread);
        threads.add(timeThread);
        threads.add(fusionThread);

        poseThread.start();
        fusionThread.start();
        try {
            Thread.sleep(1000); // Sleep for 1 second before starting the time service
        }
        catch (InterruptedException e) {}
        timeThread.start();
    }

    private static void waitForThreads(List<Thread> threads) {
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                System.err.println("Thread interrupted: " + thread.getName());
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void handleError(Exception e, String configFilePath) {
        System.err.println("Error during simulation setup: " + e.getMessage());
        e.printStackTrace();

        FusionSlam fusionSlam = FusionSlam.getInstance();
        Path outputPath = Paths.get(configFilePath).getParent().resolve("output_file.json");
        fusionSlam.writeToJson(outputPath.toString());
    }

    private static void handleError(String outputPath, SystemComponents components) {
        System.err.println("Error during simulation setup: "); 

        FusionSlam fusionSlam = FusionSlam.getInstance();

        // Collect last frames from sensors
        Map<String, List<StampedDetectedObjects>> lastCamerasFrames = collectCameraFrames(components.cameras);
        Map<String, List<TrackedObject>> lastLidersFrames= collectLidarFrames(components.lidarWorkers);

        fusionSlam.writeErrorToJson(outputPath, lastCamerasFrames, lastLidersFrames);
}

    private static Map<String, List<StampedDetectedObjects>> collectCameraFrames(List<Camera> cameras) {
        Map<String, List<StampedDetectedObjects>> cameraFrames = new LinkedHashMap<>();
        for (Camera camera : cameras) {
            cameraFrames.put("Camera " + camera.getId(), camera.getLastDetectedObjects());
        }
        return cameraFrames;
    }

    private static Map<String, List<TrackedObject>> collectLidarFrames(List<LiDarWorkerTracker> lidarWorkers) {
        Map<String, List<TrackedObject>> lidarFrames = new LinkedHashMap<>();
        for (LiDarWorkerTracker lidarWorker : lidarWorkers) {
            lidarFrames.put("LiDarTrackerWorker" + lidarWorker.getId(), lidarWorker.getLastTrackedObjects());
        }
        return lidarFrames;
    }
}

class SystemComponents {
    List<Camera> cameras = new ArrayList<>();
    List<LiDarWorkerTracker> lidarWorkers = new ArrayList<>();
    LiDarDataBase lidarDataBase;
    GPSIMU gpsIMU;
    FusionSlam fusionSlam;
    TimeService timeService;
    PoseService poseService;
    FusionSlamService fusionSlamService;
}

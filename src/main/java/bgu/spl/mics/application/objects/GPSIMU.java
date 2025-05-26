package bgu.spl.mics.application.objects;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the robot's GPS and IMU system.
 * Provides information about the robot's position and movement.
 */
public class GPSIMU {

    private int currentTick;
    private STATUS status;
    private final List<Pose> poseList;

    public GPSIMU() {
        this.currentTick = 0;
        this.status = STATUS.UP;
        this.poseList = new ArrayList<>();
    }

    public int getCurrentTick() {
        return this.currentTick;
    }

    public void setCurrentTick(int time) {
        this.currentTick = time;
    }

    public Pose getCurrentPose() {
        for (Pose pose : poseList) {
            if (pose.getTime() == currentTick) {
                return pose;
            }
        }
        return null; 
    }

    /**
     * Reads pose data from a JSON file and populates the pose list.
     * @param filePath The path to the JSON file.
     */
    public void readFromJson(String filePath) {
        try (FileReader reader = new FileReader(filePath)) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Pose>>() {}.getType();
            List<Pose> poses = gson.fromJson(reader, listType);
            synchronized (poseList) {
                poseList.clear();
                poseList.addAll(poses);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
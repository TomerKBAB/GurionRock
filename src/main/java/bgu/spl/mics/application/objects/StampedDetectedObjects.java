package bgu.spl.mics.application.objects;
import java.util.ArrayList;
/**
 * Represents objects detected by the camera at a specific timestamp.
 * Includes the time of detection and a list of detected objects.
 */
public class StampedDetectedObjects {
    private int time;
    private ArrayList<DetectedObject> detectedObjects;

    public StampedDetectedObjects(int time, ArrayList<DetectedObject> detectedObjects) {
        this.time = time;
        this.detectedObjects = detectedObjects;
    }

    public int getTime() {
        return time;
    }

    public ArrayList<DetectedObject> getDetectedObjects() {
        return detectedObjects;
    }
}

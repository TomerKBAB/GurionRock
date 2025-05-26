package bgu.spl.mics.application.objects;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
/**
 * Represents a camera sensor on the robot.
 * Responsible for detecting objects in the environment.
 */
public class Camera {
    private int id;
    private int frequency; 
    private String description;
    private STATUS status;
    private ArrayList<StampedDetectedObjects> lastDetectedObjects;
    private ArrayList<StampedDetectedObjects> toBeSentDetectedObjects; 
    private ArrayList<StampedDetectedObjects> detectedObjectList;
    private ArrayList<StampedDetectedObjects> allObjects;

    public Camera(int freq, int id, String description, String jsonFilePath) {
        this.frequency = freq;
        this.id = id;
        this.description = description;
        this.detectedObjectList = new ArrayList<>();
        this.lastDetectedObjects = new ArrayList<>();
        this.allObjects = new ArrayList<>();
        this.toBeSentDetectedObjects = new ArrayList<>();
        this.status = STATUS.UP;
        loadDetectedObjectsFromJson(jsonFilePath);
    }

    public ArrayList<DetectedObject> getDetectedObjectsToSend(int tick) {
        ArrayList<DetectedObject> detectedObjectsToSend = new ArrayList<>();
        if(status == STATUS.UP) {
            boolean found = false;
            for (int i = 0; i < toBeSentDetectedObjects.size() && !found; i++) {
                if (toBeSentDetectedObjects.get(i).getTime() == tick - frequency) {
                    detectedObjectsToSend.addAll(toBeSentDetectedObjects.get(i).getDetectedObjects());
                    detectedObjectList.add(toBeSentDetectedObjects.get(i));
                    toBeSentDetectedObjects.remove(i);
                    found = true;
                }
            }
        }
        return detectedObjectsToSend;
    }

    public int setToBeSentObjects(int tick) {
        int numToBeSent = 0;
        boolean found = false;
        for (int i = 0; i < allObjects.size() && !found; i++) { 
            if (allObjects.get(i).getTime() == tick) {
                numToBeSent = allObjects.get(i).getDetectedObjects().size();
                toBeSentDetectedObjects.add(allObjects.get(i));
                //save last Detected
                lastDetectedObjects.clear();
                lastDetectedObjects.add(allObjects.get(i));
                //for efficiency reasons, remove objects that we are done with
                allObjects.remove(i);
                found = true;
            }
        }
        return numToBeSent;
    }

    public void loadDetectedObjectsFromJson(String filePath) {
        try (FileReader reader = new FileReader(filePath)) {
            Gson gson = new Gson();
            
            // Parse the root JSON object
            JsonObject rootObject = gson.fromJson(reader, JsonObject.class);
            
            // Extract the array that belongs the "camera" id 
            JsonArray cameraObjectsArray = rootObject.getAsJsonArray("camera" + id);
            
            // Parse the array into a list of StampedDetectedObjects
            Type listType = new TypeToken<List<StampedDetectedObjects>>() {}.getType();
            List<StampedDetectedObjects> stampedObjects = gson.fromJson(cameraObjectsArray, listType);
            
            // Update the allObjects list
            allObjects.clear();
            allObjects.addAll(stampedObjects);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addDetectedObject(StampedDetectedObjects stampedObject) {
        this.detectedObjectList.add(stampedObject);
    }
       public int getId() {
        return id;
    }

    public STATUS getStatus() {
        return status;
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

    public ArrayList<StampedDetectedObjects> getDetectedObjectsList() {
        return detectedObjectList;
    }
    
    public String checkForError(int tick) {
        for (StampedDetectedObjects stampedObject : allObjects) {
            if (stampedObject.getTime() == tick) {
                for(DetectedObject object: stampedObject.getDetectedObjects()) {
                    if (object.getId().equals("ERROR")) {
                        return object.getDescription();
                    }
                }
                return null;
            }
        }
        return null;
    }

    public ArrayList<StampedDetectedObjects> getLastDetectedObjects() {
        return lastDetectedObjects;
    }

    public int getRemainingObjects() {
        return allObjects.size();
    }
    public int getFrequency() {
        return frequency;
    }
}
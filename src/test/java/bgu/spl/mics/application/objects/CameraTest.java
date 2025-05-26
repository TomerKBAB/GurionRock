package bgu.spl.mics.application.objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CameraTest {
    private Camera camera;

    @BeforeEach
    void setUp() {
        //Used example_input_with_error camera json
        String testFilePath = "src/test/java/bgu/spl/mics/test_input/camera_data.json";
        // Simulated JSON data file path

        // Initialize the Camera with a frequency, ID, description, and test file
        camera = new Camera(1, 1, "TestCamera", testFilePath);
    }
    /**
     * Class Invariant:
     * Each {@code DetectedObject} is detected at time == tick + frequency.
     *
     * Precondition:
     * The {@code STATUS} field of the camera is set to {@code STATUS.UP}.
     *
     * Postcondition:
     * Returns a list of {@code DetectedObject}s that were detected at {@code time == tick + frequency}.
     * If no objects were detected at the given tick, the returned list is empty.
     */
    @Test
    void testDetectObjects_ValidTick() {
        // object at time T should be detected at T + freq
        int tick = 1;
        camera.setToBeSentObjects(tick);
        ArrayList<DetectedObject> detectedObjects = camera.getDetectedObjectsToSend(tick);
        assertEquals(0, detectedObjects.size(), "Detected objects should be empty");

        tick = 2;
        camera.setToBeSentObjects(tick);
        detectedObjects = camera.getDetectedObjectsToSend(tick);
        assertEquals(0, detectedObjects.size(), "Detected objects should be empty");

        tick = 3;
        camera.setToBeSentObjects(tick);
        detectedObjects = camera.getDetectedObjectsToSend(tick);
        assertEquals(1, detectedObjects.size(), "One object should be detected");

        // Verify the detected object properties
        DetectedObject detectedObject = detectedObjects.get(0);
        assertEquals("Wall_1", detectedObject.getId());
        assertEquals("Wall", detectedObject.getDescription());
    }
    /**
     * Invariant:
     * The camera sends no DetectedObjects when no new items exist in the database at tick.
     * Precondition:
     * No detected objects are available at the given tick.
     * Postcondition:
     * Returns an empty list, indicating that no objects were detected at this tick.
     */
    @Test
    void testDetectObjects_NoObjectsAtTick() {
        // Simulate a tick with no objects
        int tick = 10;

        // Act
        camera.setToBeSentObjects(tick);
        ArrayList<DetectedObject> detectedObjects = camera.getDetectedObjectsToSend(tick);

        // Assert
        assertNotNull(detectedObjects, "Detected objects should not be null");
        assertTrue(detectedObjects.isEmpty(), "Detected objects should be empty at this tick");
    }

    @Test

    /**
     * Precondition:
     * All previously received DetectedObjects did not contain an error.
     * The current tick contains a DetectedObject marked as an error.
     * Postcondition:
     * The camera identifies the object containing the error and returns its ID.
     */
    void testCheckForError_WithError() {
        // Both checkForError and detectObjects works for  preparing data before sending, but separated for calrity.
        // Simulate tick with error
        int tick = 14; // Assuming tick 2 contains "ERROR"
        // Act
        String NameOfErrorObject = camera.checkForError(tick);

        // Assert
        assertNotNull(NameOfErrorObject, "Error should be detected");
        assertEquals("Camera Disconnected", NameOfErrorObject); 
    }

}


package bgu.spl.mics.application.objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FusionSlamTest {
    private FusionSlam fusionSlam;

    @BeforeEach
    void setUp() {
        fusionSlam = FusionSlam.getInstance();
        fusionSlam.clearData();

        // Initialize poses required for transformation
        Pose initialPose = new Pose(1, 0, 0, 0); 
        Pose secondPose = new Pose(2, 1, 1, 90); 
        fusionSlam.addPose(initialPose);
        fusionSlam.addPose(secondPose);
    }

    
    /**
     * Precondition:
     * A tracked object has been sent by a LiDAR sensor to the FusionSLAM system.
     *
     * Postcondition:
     * The FusionSLAM system successfully receives the tracked object and creates a new landmark 
     * in the global map based on the object's information.
     */
    @Test
    void testUpdateLandMark_newLandmark() {
        // Simulate a tracked object detected at time = 1
        TrackedObject trackedObject = new TrackedObject("Wall_1", 1, "Wall", Arrays.asList(new CloudPoint(1.0, 2.0)));
        fusionSlam.updateLandMark(trackedObject);

        // Verify that a new landmark is created
        List<LandMark> landMarks = fusionSlam.getLandMarks();
        assertEquals(1, landMarks.size());
        assertEquals("Wall_1", landMarks.get(0).getId());

        // Verify the transformed coordinates
        List<CloudPoint> coordinates = landMarks.get(0).getCoordinates();
        assertEquals(1.0, coordinates.get(0).getX());
        assertEquals(2.0, coordinates.get(0).getY());
    }

    
    /**
     * Precondition:
     * The {@code landMarks} object already exists within the FusionSLAM system and represents
     * a previously detected landmark.
     *
     * Postcondition:
     * The {@code landMarks} object is updated to include the newly received tracked object,
     * and its coordinates are averaged with the previous values to refine the landmark's position.
     */
    @Test
    void testUpdateLandMark_existingLandmark() {
        // Simulate a tracked object detected at time = 1
        TrackedObject trackedObject1 = new TrackedObject("Wall_2", 1, "Wall", Arrays.asList(new CloudPoint(1.0, 2.0)));
        fusionSlam.updateLandMark(trackedObject1);

        // Simulate another tracked object detected at time = 2 for the same landmark
        TrackedObject trackedObject2 = new TrackedObject("Wall_2", 2, "Wall", Arrays.asList(new CloudPoint(2.0, 3.0)));
        fusionSlam.updateLandMark(trackedObject2);

        // Verify that only one landmark exists
        List<LandMark> landMarks = fusionSlam.getLandMarks();
        assertEquals(1, landMarks.size());
        assertEquals("Wall_2", landMarks.get(0).getId());

        // Verify the averaged coordinates
        List<CloudPoint> coordinates = landMarks.get(0).getCoordinates();
        double x = coordinates.get(0).getX();
        double y = coordinates.get(0).getY();


        assertEquals(-0.500000, x, 0.000001); 
        assertEquals(2.500000, y, 0.000001);
    }
}

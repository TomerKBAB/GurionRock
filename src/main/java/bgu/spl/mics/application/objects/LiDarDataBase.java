package bgu.spl.mics.application.objects;
import java.util.List;
import java.util.ArrayList;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;

/**
 * LiDarDataBase is a singleton class responsible for managing LiDAR data.
 * It provides access to cloud point data and other relevant information for tracked objects.
 */
public class LiDarDataBase {
    private List<StampedCloudPoints> cloudPoints;

	private static class LiDarDataBaseHolder {
		private static final LiDarDataBase liDarDataBase = new LiDarDataBase();
	}

    private LiDarDataBase() {
        cloudPoints = new ArrayList<>();
    }

    /**
     * Returns the singleton instance of LiDarDataBase.
     *
     * @param filePath The path to the LiDAR data file.
     * @return The singleton instance of LiDarDataBase.
     */
    // Private constructor for singleton
    public static synchronized LiDarDataBase getInstance() {
		return LiDarDataBaseHolder.liDarDataBase;
    }

    /**
     * Adds new cloud points data to the database.
     * @param stampedCloudPoints The stamped cloud points to be added.
     */
    public synchronized void addCloudPoints(StampedCloudPoints stampedCloudPoints) {
        cloudPoints.add(stampedCloudPoints);
    }

    public void loadFromJson(String filePath) {
        try (FileReader reader = new FileReader(filePath)) {
            Gson gson = new Gson();
            // Parse raw data as a list of JSON-matching objects
            Type listType = new TypeToken<List<RawStampedCloudPoints>>() {}.getType();
            List<RawStampedCloudPoints> rawData = gson.fromJson(reader, listType);
            List<StampedCloudPoints> parsedData = new ArrayList<>();
            for (RawStampedCloudPoints raw : rawData) {
                List<CloudPoint> cloudPoints = new ArrayList<>();
                for (List<Double> coordinates : raw.cloudPoints) {
                    // Create CloudPoint with only x and y (ignore z)
                    cloudPoints.add(new CloudPoint(coordinates.get(0), coordinates.get(1)));
                }
                // Convert raw object into a fully constructed StampedCloudPoints
                parsedData.add(new StampedCloudPoints(raw.id, raw.time, cloudPoints));
            }
            // Add all parsed data to the database
            synchronized (this) {
                cloudPoints.addAll(parsedData);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<CloudPoint> getCloudPoints(String objectId, int time) {
        for (StampedCloudPoints stamped : cloudPoints) {
            if (stamped.getId().equals(objectId) && stamped.getTime() == time) {
                return stamped.getCloudPoints();
            }
        }
        return null;
    }

    public int getSize() {
        return cloudPoints.size();
    }

    // Helper class to parse JSON format
    private static class RawStampedCloudPoints {
        String id;
        int time;
        List<List<Double>> cloudPoints; // Matches the JSON structure
    }
}
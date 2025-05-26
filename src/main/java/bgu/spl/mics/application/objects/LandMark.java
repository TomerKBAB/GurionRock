package bgu.spl.mics.application.objects;
import java.util.List;

/**
 * Represents a landmark in the environment map.
 * Landmarks are identified and updated by the FusionSlam service.
 */
public class LandMark {

    private final String id;
    private final String description;
    private List<CloudPoint> coordinates;

    public LandMark(String id, String description, List<CloudPoint> coordinates) {
        this.id = id;
        this.description = description;
        this.coordinates = coordinates;
    }
    @Override
    public String toString(){
        return ("id: "+ new String(this.id) + "   description: " + description + coordinates.toString());
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public List<CloudPoint> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<CloudPoint> coordinates) {
        this.coordinates = coordinates;
    }
}

package bgu.spl.mics.application.messages;
import java.util.List;
import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.TrackedObject;

public class TrackedObjectsEvent implements Event<Void> {

    private final List<TrackedObject> trackedObjects;

    public TrackedObjectsEvent(List<TrackedObject> trackedObjects) {
        this.trackedObjects = trackedObjects;
    }

    /**
     * Retrieves the list of tracked objects associated with this event.
     *
     * @return A list of tracked objects.
     */
    public List<TrackedObject> getTrackedObjects() {
        return trackedObjects;
    } 
}

package bgu.spl.mics.application.messages;
import java.util.List;
import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.DetectedObject;
import bgu.spl.mics.Future;

public class DetectObjectsEvent implements Event<Boolean> {
    private final List<DetectedObject> detectedObjects;
    private final int tick;
    public  Future<Boolean> future;

    public DetectObjectsEvent(List<DetectedObject> detectedObjects, int tick) {
        this.detectedObjects = detectedObjects;
        this.tick = tick;
        this.future = null;
    }
    public void updateFuture(Future<Boolean> future){
        this.future = future;

    }

    public List<DetectedObject> getDetectedObjects() {
        return detectedObjects;
    }

    public int getTick() {
        return tick;
    } 
}

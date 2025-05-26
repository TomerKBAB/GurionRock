package bgu.spl.mics.application.objects;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;;

// this class represents sensor monitor, and keeps track of all active sensors. used for stopping when all sensors are finished.
public class SensorMonitor {
    private final Set<String> activeSensors = ConcurrentHashMap.newKeySet();

    // Singleton instance holder
    private static class SensorMonitorHolder {  
        private static final SensorMonitor instance = new SensorMonitor();
    }
    private SensorMonitor() {}

    public static SensorMonitor getInstance() {
        return SensorMonitorHolder.instance;
    }

    public synchronized void addSensor(String sensorId) {
        activeSensors.add(sensorId);
    }

    public synchronized void removeSensor(String sensorId) {
        activeSensors.remove(sensorId);
    }

    public synchronized boolean areAllSensorsFinished() {
        return activeSensors.isEmpty();
    }
}

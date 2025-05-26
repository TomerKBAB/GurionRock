package bgu.spl.mics.application.objects;

/**
 * Represents the robot's pose (position and orientation) in the environment.
 * Includes x, y coordinates and the yaw angle relative to a global coordinate system.
 */
public class Pose {
    public int time;
    public double x;
    public double y;
    public double yaw;
    
    public Pose(int time, double x, double y, double yaw) {
        this.time = time;
        this.x = x;
        this.y = y;
        this.yaw = yaw;
    }

    public int getTime() {
        return time;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getYaw() {
        return yaw;
    }
}

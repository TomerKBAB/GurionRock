package bgu.spl.mics.application.messages;
import bgu.spl.mics.Broadcast;

public class CrashedBroadcast implements Broadcast  {

private final String reason;

    public CrashedBroadcast(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.*;
import bgu.spl.mics.application.objects.StatisticalFolder;
import bgu.spl.mics.application.objects.SensorMonitor;
/**
 * TimeService acts as the global timer for the system, broadcasting TickBroadcast messages
 * at regular intervals and controlling the simulation's duration.
 */
public class TimeService extends MicroService {
 
    private final int TickTime;
    private final int Duration;
    private int tickCounter;
    private SensorMonitor monitor;
    private StatisticalFolder statFolder;
 
    public TimeService(int TickTime, int Duration) {
        super("timer");
        this.TickTime = TickTime;
        this.Duration = Duration;
        tickCounter = 1;
        this.monitor = SensorMonitor.getInstance();
        this.statFolder = StatisticalFolder.getInstance();
    }

    /*
     * Initializes the TimeService.
     * Starts broadcasting TickBroadcast messages and terminates after the specified duration.
     */
    @Override
    protected void initialize() {

        subscribeBroadcast(CrashedBroadcast.class, crashedBroadcast -> {
            statFolder.changeTickTime(tickCounter - 1);
            terminate();
        });
        
        subscribeBroadcast(TickBroadcast.class, tickBroadCast -> {
            tickCounter++; 
            if (tickCounter <= Duration) {
                 if (monitor.areAllSensorsFinished()) {
                    statFolder.changeTickTime(tickCounter - 1);
                    sendBroadcast(new TerminatedBroadcast());
                    terminate();
                    sendBroadcast(new TickBroadcast(tickCounter));
                }
                else {
                    TickBroadcast newTickBroadcast = new TickBroadcast(tickCounter);
                    sendBroadcast(newTickBroadcast);
                    try {
                        Thread.sleep(TickTime*1000);
                    } catch (Exception e) {}
                }
            }
            else {
                statFolder.changeTickTime(tickCounter - 1);
                sendBroadcast(new TerminatedBroadcast());
                terminate();
            }
        });
        sendBroadcast(new TickBroadcast(tickCounter));
    }
}

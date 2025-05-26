package bgu.spl.mics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import bgu.spl.mics.application.messages.*;
import bgu.spl.mics.application.objects.DetectedObject;
import bgu.spl.mics.application.objects.Pose;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import bgu.spl.mics.*;

import static org.junit.jupiter.api.Assertions.*;

class MessageBusImplTest {
    private MessageBusImpl messageBus;
    private MicroService microServiceA;
    private MicroService microServiceB;
    private MicroService microServiceC;

    @BeforeEach
    void setUp() {
        messageBus = MessageBusImpl.getInstance();
        messageBus.clearData();

        microServiceA = new MicroService("MicroServiceA") {
            @Override
            protected void initialize() {
            }
        };

        microServiceB = new MicroService("MicroServiceB") {
            @Override
            protected void initialize() {
            }
        };

        microServiceC = new MicroService("MicroServiceC") {
            @Override
            protected void initialize() {
            }
        };

        messageBus.register(microServiceA);
        messageBus.register(microServiceB);
        messageBus.register(microServiceC);
    }
    
    /**
     * Precondition:
     * A singleton instance of `MessageBusImpl` must already exist. Additionally, three microservices 
     * (`microServiceA`, `microServiceB`, and `microServiceC`) are registered with the message bus 
     * and subscribed to handle `PoseEvent`.
     *
     * Postcondition:
     * Each event is dispatched to a microservice according to the round-robin distribution policy. 
     * All events that are sent are successfully processed, with no messages being lost.
     */
    @Test 
    void testRoundRobinEventDispatching() throws InterruptedException {
    //single thread case. 
        messageBus.subscribeEvent(PoseEvent.class, microServiceA);
        messageBus.subscribeEvent(PoseEvent.class, microServiceB);
        messageBus.subscribeEvent(PoseEvent.class, microServiceC);
        for(int i = 0 ; i<20 ; i++){
            messageBus.sendEvent(new PoseEvent(new Pose(i, i, i, i))); //Events are distinguished by i
        }
        //threads A B C should only get Events sent in constant i%3 locations.
        for(int i = 0 ; i<20 ; i++){
            switch (i%3) {
                case (0):
                assertEquals(0,((PoseEvent)messageBus.awaitMessage(microServiceA)).getPose().getX()%3);
                continue;
                case(1):
                assertEquals(1,((PoseEvent)messageBus.awaitMessage(microServiceB)).getPose().getX()%3);
                continue;
                case(2):
                assertEquals(2,((PoseEvent)messageBus.awaitMessage(microServiceC)).getPose().getX()%3);
                continue;
            }
        }
        //multipile thread Test Case
        long deadline = 20000; //test should never take more than 10 seconds.
        long startTime = System.currentTimeMillis();
        Thread T1 = new Thread(microServiceA);
        Thread T2 = new Thread(microServiceB);
        Thread T3 = new Thread(microServiceC);
        Thread Tsender = new Thread(() -> {
            for(int i = 0 ; i < 99 ; i++){
                messageBus.sendEvent(new PoseEvent(new Pose(i, i, i, i)));
            }
        });
        T1.start();
        T2.start();
        T3.start();
        Tsender.start();
        Tsender.join();
        while(!this.messageBus.getMessegeQueue(microServiceC).isEmpty()){
            try {
                Thread.sleep(300); 
                if(System.currentTimeMillis()-startTime>deadline){
                    T3.interrupt();
                    T2.interrupt();
                    T1.interrupt();
                    fail("Test timeout reached, messeges are not being pulled from queue.");
                    break;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Thread.sleep(1000);
        T3.interrupt();
        T2.interrupt();
        T1.interrupt();
        //make sure each thread got the same amout of Events.
        int a = microServiceA.GetNumEventReceived();
        int b = microServiceB.GetNumEventReceived();
        int c = microServiceC.GetNumEventReceived();
        assertEquals(a, b);
        assertEquals(b, c);
        //make sure all messeges was received
        assertEquals(a+b+c, 99);
    }

    
    /**
     * Precondition:
     * no MicroService has been registered to any broadcast yet.
     * Postcondition:
     * MicroService registered and unregistered successfully
     */
    @Test
    void testRegisterAndUnregister() {
        CrashedBroadcast crashedBroadcast = new CrashedBroadcast("AAA");
        messageBus.subscribeBroadcast(CrashedBroadcast.class, microServiceA);
        messageBus.sendBroadcast(crashedBroadcast);
        assertDoesNotThrow(() -> messageBus.awaitMessage(microServiceA));
        // Unregister one microservice and verify
        messageBus.unregister(microServiceA);
        assertThrows(IllegalStateException.class, () -> messageBus.awaitMessage(microServiceA));
    }
    /**
     * Precondition:
     * A singleton instance of `MessageBusImpl` has been created.
     * Three microservices (`microServiceA`, `microServiceB`, and `microServiceC`) have been registered.
     *
     * Postcondition:
     * Each microservice can subscribe and unsubscribe to events and broadcasts.
     * Messages are delivered in the correct order (FIFO for events).
     * essages are received only by subscribed microservices.
     * @throws InterruptedException
     */
    @Test
    void testSubscribeAndSendMesseges() throws InterruptedException {
        // Subscribe & send events
        Pose pose = new Pose(0, 0, 0, 0);
        PoseEvent event = new PoseEvent(pose);
        messageBus.subscribeEvent(PoseEvent.class, microServiceA);
        messageBus.sendEvent(event);

        // Verify that the event is received (by the first thread registered)

        Message receivedMessage = messageBus.awaitMessage(microServiceA);
        assertEquals(event, receivedMessage);

        // // Subscribe & send broadcasts

        CrashedBroadcast broadcast = new CrashedBroadcast("AAA");
        messageBus.subscribeBroadcast(CrashedBroadcast.class, microServiceA);
        messageBus.subscribeBroadcast(CrashedBroadcast.class, microServiceB);
        messageBus.sendBroadcast(broadcast);

        // Verify both microservices receive the broadcast

        assertEquals(broadcast, messageBus.awaitMessage(microServiceA));
        assertEquals(broadcast, messageBus.awaitMessage(microServiceB));

        // Create an event with no subscribers

        DetectObjectsEvent eventToUnsubcrive = new DetectObjectsEvent(new ArrayList<DetectedObject>(), 0);
        Future<?> future = messageBus.sendEvent(eventToUnsubcrive);
        assertNull(future);
        //case: Event sent to unsubscribed microservice

        messageBus.unregister(microServiceA);
        assertThrows(IllegalStateException.class, () -> messageBus.awaitMessage(microServiceA));

        //case: Messeges being received in orrect order (FIFO)

        Pose pose1 = new Pose(1, 1, 1, 1);
        Pose pose2 = new Pose(2, 2, 2, 2);
        //Sending Events
        PoseEvent event1 = new PoseEvent(pose1);
        PoseEvent event2 = new PoseEvent(pose2);
        messageBus.subscribeEvent(PoseEvent.class, microServiceB);
        messageBus.sendEvent(event1);
        messageBus.sendEvent(event2);
        // Ensure events are received in order
        assertEquals(event1, messageBus.awaitMessage(microServiceB));
        assertEquals(event2, messageBus.awaitMessage(microServiceB));
    }
}

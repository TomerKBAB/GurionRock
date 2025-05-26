package bgu.spl.mics;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The {@link MessageBusImpl class is the implementation of the MessageBus interface.
 * Write your implementation here!
 * Only private fields and methods can be added to this class.
 */
public class MessageBusImpl implements MessageBus {
    

    private static class MessageBusHolder {
        private static final MessageBusImpl busInstance = new MessageBusImpl();
    }

    private Map<MicroService, LinkedBlockingQueue<Message>> MessagesQueue;
    private Map<Class<? extends Event<?>>, LinkedBlockingQueue<MicroService>> eventMap;
    private Map<Class<? extends Broadcast>, List<MicroService>> broadcastMap;
    private final Map<Event<?>, Future<?>> eventFutures;

    private MessageBusImpl() {
        MessagesQueue = new ConcurrentHashMap<>();
        eventMap = new ConcurrentHashMap<>();
        broadcastMap = new ConcurrentHashMap<>();
        eventFutures = new ConcurrentHashMap<>();
    }

    public <T> void subscribeEvent(Class<? extends Event<T>> type, MicroService m) {
        eventMap.computeIfAbsent(type, key -> new LinkedBlockingQueue<>()).add(m);
    }

    @Override
    public void subscribeBroadcast(Class<? extends Broadcast> type, MicroService m) {
        broadcastMap.computeIfAbsent(type, key -> new CopyOnWriteArrayList<>()).add(m);
    }

    @Override
    public <T> void complete(Event<T> e, T result) {
        Future<T> future = (Future<T>) eventFutures.get(e);
        if (future != null) {
            future.resolve(result);
        }
    }

    @Override
    public void sendBroadcast(Broadcast b) {
        List<MicroService> registered = broadcastMap.get(b.getClass()); 
        if (registered != null) {
            for (MicroService m : registered) {
                LinkedBlockingQueue<Message> q = MessagesQueue.get(m);
                if (q != null) {
                    try {
                        q.put(b); // Blocking add if the queue is full
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); 
                    }
                }
            }
        }
    }

    @Override
    public <T> Future<T> sendEvent(Event<T> e) {
        Queue<MicroService> subscribers = eventMap.get(e.getClass());
        if (subscribers == null || subscribers.isEmpty()) {
            return null; 
        }
        synchronized (subscribers) {
            MicroService receiving = subscribers.poll();
            if (receiving != null) {
                LinkedBlockingQueue<Message> q = MessagesQueue.get(receiving);
                if (q != null) {
                    try {
                        q.put(e); 
                    } catch (InterruptedException err) {
                        Thread.currentThread().interrupt(); 
                    }
                }
                subscribers.add(receiving); // Round-robin
            }
            subscribers.notifyAll();
        }
        Future<T> future = new Future<>(); 
        eventFutures.put(e, future);
        return future;
    }

    @Override
    public void register(MicroService m) {
        MessagesQueue.computeIfAbsent(m, key -> new LinkedBlockingQueue<>());
    }

    @Override
    public synchronized void unregister(MicroService m) {
        eventMap.values().forEach(queue -> queue.remove(m));
        broadcastMap.values().forEach(list -> list.remove(m));
        MessagesQueue.remove(m);
    }

    @Override
    public Message awaitMessage(MicroService m) throws InterruptedException {
        LinkedBlockingQueue<Message> myQueue = MessagesQueue.get(m); 
        if (myQueue == null) {
            throw new IllegalStateException("MicroService " + m + " is not registered.");
        }
        Message ret = myQueue.take();
        return ret;
    }

    public static MessageBusImpl getInstance() {
        return MessageBusHolder.busInstance;
    }

    //ClearData only for testing purposes
    public void clearData() {
        MessagesQueue.clear();
        eventMap.clear();
        broadcastMap.clear();
        eventFutures.clear();
    }
    //Usage Only in test
    public LinkedBlockingQueue<Message> getMessegeQueue(MicroService serivce){ 
        return this.MessagesQueue.get(serivce);
    }
}

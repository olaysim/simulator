package dk.orda.overlaynetwork.net.rpc;

import com.google.protobuf.MessageLite;

import java.util.UUID;
import java.util.concurrent.*;

public class MessageQueue {
    private final BlockingQueue<MessageLite> queue;
    private final ConcurrentMap<UUID, BlockingQueue<MessageLite>> map;

    public MessageQueue() {
        queue = new ArrayBlockingQueue<>(10);
        map = new ConcurrentHashMap<>();
    }

    public void put(MessageLite msg) throws InterruptedException {
        queue.put(msg);
    }

    public boolean add(MessageLite msg) {
        return queue.add(msg);
    }

    public boolean offer(MessageLite msg) {
        return queue.offer(msg);
    }

    public boolean offer(MessageLite msg, long timeout, TimeUnit unit) throws InterruptedException {
        return queue.offer(msg, timeout, unit);
    }

    public boolean remove(MessageLite msg) {
        return queue.remove(msg);
    }

    public MessageLite take() throws InterruptedException {
        return queue.take();
    }

    public MessageLite poll() {
        return queue.poll();
    }

    public MessageLite poll(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        MessageLite msg = queue.poll(timeout, unit);
        if (msg == null) throw new TimeoutException();
        return msg;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }


    // This is a kind of blocking map
    public void put(UUID key, MessageLite value) throws InterruptedException {
        synchronized (map) {
            if (!map.containsKey(key)) {
                map.put(key, new ArrayBlockingQueue<>(1));
            }
        }
        map.get(key).put(value);
    }

    public boolean add(UUID key, MessageLite msg) {
        synchronized (map) {
            if (!map.containsKey(key)) {
                map.put(key, new ArrayBlockingQueue<>(1));
            }
        }
        return map.get(key).add(msg);
    }

    public boolean offer(UUID key, MessageLite msg) {
        synchronized (map) {
            if (!map.containsKey(key)) {
                map.put(key, new ArrayBlockingQueue<>(1));
            }
        }
        return map.get(key).offer(msg);
    }

    public boolean offer(UUID key, MessageLite msg, long timeout, TimeUnit unit) throws InterruptedException {
        synchronized (map) {
            if (!map.containsKey(key)) {
                map.put(key, new ArrayBlockingQueue<>(1));
            }
        }
        return map.get(key).offer(msg, timeout, unit);
    }

    public boolean remove(UUID key, MessageLite msg) {
        return map.containsKey(key) && map.get(key).remove(msg);
    }

    public MessageLite take(UUID key) throws InterruptedException {
        synchronized (map) {
            if (!map.containsKey(key)) {
                map.put(key, new ArrayBlockingQueue<>(1));
            }
        }
        return map.get(key).take();
    }

    public MessageLite poll(UUID key) throws InterruptedException {
        synchronized (map) {
            if (!map.containsKey(key)) {
                map.put(key, new ArrayBlockingQueue<>(1));
            }
        }
        return map.get(key).poll();
    }

    public MessageLite poll(UUID key, long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        synchronized (map) {
            if (!map.containsKey(key)) {
                map.put(key, new ArrayBlockingQueue<>(1));
            }
        }
        MessageLite msg = map.get(key).poll(timeout, unit);
        if (msg == null) throw new TimeoutException();
        return msg;
    }

    public boolean isEmpty(UUID key) {
        synchronized (map) {
            if (!map.containsKey(key)) {
                map.put(key, new ArrayBlockingQueue<>(1));
            }
        }
        return map.get(key).isEmpty();
    }

    public boolean removeQueue(UUID key) {
        synchronized (map) {
            if (!map.containsKey(key)) return false;
            else {
                map.remove(key);
                return true;
            }
        }
    }
}

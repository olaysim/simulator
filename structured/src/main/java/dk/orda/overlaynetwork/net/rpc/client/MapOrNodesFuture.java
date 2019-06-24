package dk.orda.overlaynetwork.net.rpc.client;

import dk.orda.overlaynetwork.net.rpc.MessageQueue;
import dk.orda.overlaynetwork.net.rpc.protobuf.RpcMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MapOrNodesFuture implements Future<RpcMessages.MapOrNodes> {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final MessageQueue queue;
    private UUID uuid;

    public MapOrNodesFuture(MessageQueue queue) {
        this.queue = queue;
        this.uuid = UUID.randomUUID();
    }

    public MapOrNodesFuture(MessageQueue queue, MapOrNodesFuture f) {
        this.queue = queue;
        if (f != null) {
            this.uuid = f.getUUID();
        } else {
            this.uuid = UUID.randomUUID();
        }
    }

    public MapOrNodesFuture(MessageQueue queue, UUID uuid) {
        this.queue = queue;
        this.uuid = uuid;
    }

    public void set(RpcMessages.MapOrNodes msg) {
        try {
            queue.put(uuid, msg);
        } catch (InterruptedException e) {
            log.error("Unable to add message wrapper to future");
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return !queue.isEmpty(uuid);
    }

    @Override
    public RpcMessages.MapOrNodes get() throws InterruptedException, ExecutionException {
        RpcMessages.MapOrNodes msg = (RpcMessages.MapOrNodes)queue.take(uuid);
        return msg;
    }

    @Override
    public RpcMessages.MapOrNodes get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        RpcMessages.MapOrNodes msg = (RpcMessages.MapOrNodes)queue.poll(uuid, timeout, unit);
        if (msg == null) throw new TimeoutException();
        return msg;
    }

    public UUID getUUID() {
        return uuid;
    }

    public void markDone() {
        queue.removeQueue(uuid);
    }
}

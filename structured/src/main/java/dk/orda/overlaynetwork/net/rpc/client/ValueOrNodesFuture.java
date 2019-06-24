package dk.orda.overlaynetwork.net.rpc.client;

import dk.orda.overlaynetwork.net.rpc.MessageQueue;
import dk.orda.overlaynetwork.net.rpc.protobuf.RpcMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.*;

public class ValueOrNodesFuture implements Future<RpcMessages.ValueOrNodes> {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final MessageQueue queue;
    private UUID uuid;

    public ValueOrNodesFuture(MessageQueue queue) {
        this.queue = queue;
        this.uuid = UUID.randomUUID();
    }

    public ValueOrNodesFuture(MessageQueue queue, ValueOrNodesFuture f) {
        this.queue = queue;
        if (f != null) {
            this.uuid = f.getUUID();
        } else {
            this.uuid = UUID.randomUUID();
        }
    }

    public ValueOrNodesFuture(MessageQueue queue, UUID uuid) {
        this.queue = queue;
        this.uuid = uuid;
    }

    public void set(RpcMessages.ValueOrNodes msg) {
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
    public RpcMessages.ValueOrNodes get() throws InterruptedException, ExecutionException {
        RpcMessages.ValueOrNodes msg = (RpcMessages.ValueOrNodes)queue.take(uuid);
        return msg;
    }

    @Override
    public RpcMessages.ValueOrNodes get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        RpcMessages.ValueOrNodes msg = (RpcMessages.ValueOrNodes)queue.poll(uuid, timeout, unit);
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

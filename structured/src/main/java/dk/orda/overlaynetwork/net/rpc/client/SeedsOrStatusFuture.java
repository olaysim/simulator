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

public class SeedsOrStatusFuture implements Future<RpcMessages.SeedsOrStatus> {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final MessageQueue queue;
    private UUID uuid;

    public SeedsOrStatusFuture(MessageQueue queue) {
        this.queue = queue;
        this.uuid = UUID.randomUUID();
    }

    public SeedsOrStatusFuture(MessageQueue queue, SeedsOrStatusFuture f) {
        this.queue = queue;
        if (f != null) {
            this.uuid = f.getUUID();
        } else {
            this.uuid = UUID.randomUUID();
        }
    }

    public SeedsOrStatusFuture(MessageQueue queue, UUID uuid) {
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
    public RpcMessages.SeedsOrStatus get() throws InterruptedException, ExecutionException {
        RpcMessages.SeedsOrStatus msg = (RpcMessages.SeedsOrStatus)queue.take(uuid);
        return msg;
    }

    @Override
    public RpcMessages.SeedsOrStatus get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        RpcMessages.SeedsOrStatus msg = (RpcMessages.SeedsOrStatus)queue.poll(uuid, timeout, unit);
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

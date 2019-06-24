package dk.orda.overlaynetwork.net.broadcast;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.*;

public class NodeEventBroadcaster implements Runnable {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final EventLoopGroup group;
    private final Bootstrap bootstrap;
    private ExecutorService executor;
    private Channel channel;

    public NodeEventBroadcaster(InetSocketAddress address) {
        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(group).channel(NioDatagramChannel.class)
            .option(ChannelOption.SO_BROADCAST, true)
            .handler(new NodeEventEncoder(address));
        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void run() {
        try {
            channel = bootstrap.bind(0).sync().channel();
        } catch (InterruptedException e) {
            log.error("Unable to start broadcaster!, closing broadcaster.", e);
        }
    }

    public void sendEvent(NodeEvent event) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(event);
        }
    }

    public NodeEventBroadcaster start() {
        executor.submit(this);
        return this;
    }

    @SuppressWarnings("Duplicates")
    public void stop() {
        group.shutdownGracefully();

        try {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            log.debug("Node broadcaster took too long to shut down, interrupting thread.");
        } finally {
            if (!executor.isTerminated()) {
                executor.shutdownNow();
            }
        }
    }
}

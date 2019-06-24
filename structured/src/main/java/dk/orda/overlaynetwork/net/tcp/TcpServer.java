package dk.orda.overlaynetwork.net.tcp;

import dk.orda.overlaynetwork.net.Address;
import dk.orda.overlaynetwork.net.NetworkServer;
import dk.orda.overlaynetwork.net.Port;
import dk.orda.overlaynetwork.net.rpc.*;
import dk.orda.overlaynetwork.net.seed.Seeds;
import dk.orda.overlaynetwork.overlay.OverlayNetwork;
import dk.orda.overlaynetwork.overlay.RequestOverlay;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TcpServer implements Runnable {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final EventLoopGroup bossGroup, workerGroup;
    private final ServerBootstrap bootstrap;
    private final Address address;
    private final ExecutorService executor;
    private final SslContext sslContext;

    public TcpServer(Address address, SuperNode sn) throws InterruptedException {
        this(address, null, 0L, 0L, sn, null);
    }
    public TcpServer(Address address, long readLimit, long writeLimit, SuperNode sn) throws InterruptedException {
        this(address, null, readLimit, writeLimit, sn, null);
    }
    public TcpServer(Address address, SslContext sslContext, SuperNode sn) throws InterruptedException {
        this(address, null, 0L, 0L, sn, null);
    }

    public TcpServer(Address address, SslContext sslContext, long readLimit, long writeLimit, SuperNode sn, Seeds seeds) throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
            .handler(new LoggingHandler(LogLevel.INFO))
            .childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pl = ch.pipeline();
                pl.addLast("sslinit", new SslInitializer(sslContext, sn, seeds));
                if (readLimit != 0L && writeLimit != 0L) {
                    pl.addLast("traffic", new ChannelTrafficShapingHandler(readLimit, writeLimit));
                }
            }
        });
        this.address = address;
        this.sslContext = sslContext;
        this.executor = Executors.newSingleThreadExecutor();
    }

    // for use with seed server
    public TcpServer(Address address, SslContext sslContext, long readLimit, long writeLimit, Seeds seeds) throws InterruptedException {
        this(address, sslContext, readLimit, writeLimit, null, seeds);
    }

    @Override
    public void run() {
        try {
            bootstrap.bind(address.getInetSocketAddress()).sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("Unable to start Tcp Server!", e);
        }
    }

    public TcpServer start() {
        executor.submit(this);
        return this;
    }

    public void stop() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();

        try {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            log.debug("Tcp Server took too long to shut down, interrupting thread.");
        } finally {
            if (!executor.isTerminated()) {
                executor.shutdownNow();
            }
        }
    }
}

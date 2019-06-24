package dk.orda.overlaynetwork.net.broadcast;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class NodeEventMonitor {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final EventLoopGroup group;
    private final Bootstrap bootstrap;
    private Channel channel;

    public NodeEventMonitor(InetSocketAddress address) {
        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(group).channel(NioDatagramChannel.class)
            .option(ChannelOption.SO_BROADCAST, true)
            .handler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) throws Exception {
                    ChannelPipeline pl = ch.pipeline();
                    pl.addLast(new NodeEventDecoder());
                    pl.addLast(new NodeEventHandler());
                }
            })
            .localAddress(address);
    }

    public void bind() throws InterruptedException {
        channel = bootstrap.bind().sync().channel();
    }

    public void stop() {
        group.shutdownGracefully();
    }

    public void addNodeEventListener(NodeEventListener listener) {
        channel.pipeline().get(NodeEventHandler.class).addListener(listener);
    }

    public void removeNodeEventListener(NodeEventListener listener) {
        channel.pipeline().get(NodeEventHandler.class).removeListener(listener);
    }
}

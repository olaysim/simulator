package dk.orda.overlaynetwork.net.tcp;

import com.google.protobuf.MessageLite;
import dk.orda.overlaynetwork.net.rpc.MessageQueue;
import dk.orda.overlaynetwork.net.rpc.MessageType;
import dk.orda.overlaynetwork.net.rpc.Protocol;
import dk.orda.overlaynetwork.net.rpc.client.*;
import dk.orda.overlaynetwork.net.rpc.protobuf.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.ssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

public class TcpClient {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final EventLoopGroup group;
    private final Bootstrap bootstrap;
    private final SslContext sslContext;
    private final ConcurrentMap<Channel, MessageQueue> queueMap;

    public TcpClient() {
        this(null);
    }

    public TcpClient(SslContext sslContext) {
        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(group).channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) throws Exception {
                    ChannelPipeline pl = ch.pipeline();
                    if (sslContext != null) {
                        pl.addLast("ssl", sslContext.newHandler(ch.alloc()));
                    }
                }
            });
        this.sslContext = sslContext;
        this.queueMap = new ConcurrentHashMap<>();
    }

    public Channel connect(InetSocketAddress address) {
        return connect(address, Protocol.PROTOBUF); // default to protobuf protocol
    }
    public Channel connect(InetSocketAddress address, Protocol protocol) {
        try {
            Channel channel = bootstrap.connect(address).sync().channel();
            queueMap.put(channel, new MessageQueue());
            setProtocol(channel, protocol);
            return  channel;
        } catch (InterruptedException e) {
            return null;
        }
    }

    public void setProtocol(Channel channel, Protocol protocol) {
        ChannelPipeline p = channel.pipeline();
        removeHandlers(channel);
        p.addLast("protocol", new ProtocolEncoder());
        switch (protocol) {
            case PROTOBUF:
//                p.addLast("encoder", new ProtobufEncoder());
//                p.addLast("decoder", new ProtobufDecoder());
                p.addLast("framedecoder", new LengthFieldBasedFrameDecoder(1048576*10, 0 , 4, 0 , 4)); // max message size = 10MB
                p.addLast("decoder", new ProtobufDecoder2());
                p.addLast("frameencoder", new LengthFieldPrepender(4));
                p.addLast("encoder", new ProtobufEncoder2());
                p.addLast("valuehandler", new ValueOrNodesHandler(queueMap.get(channel)));
                p.addLast("maphandler", new MapOrNodesHandler(queueMap.get(channel)));
                p.addLast("pinghandler", new PingHandler(queueMap.get(channel)));
                break;
            case SEEDS:
                p.addLast("framedecoder", new LengthFieldBasedFrameDecoder(1048576*10, 0 , 4, 0 , 4)); // max message size = 10MB
                p.addLast("decoder", new ProtobufDecoder2());
                p.addLast("frameencoder", new LengthFieldPrepender(4));
                p.addLast("encoder", new ProtobufEncoder2());
                p.addLast("seedhandler", new SeedsOrStatusHandler(queueMap.get(channel)));
                break;
        }
        channel.writeAndFlush(protocol); // select protocol on server
    }

    public void ping(Channel channel) {
        throw new UnsupportedOperationException();
    }

    public <T extends MessageLite, V extends Future> V sendMessage(Channel channel, T msg, MessageType type, UUID uuid) {
        return sendMessage(channel, msg, type, null, uuid);
    }

    @SuppressWarnings("unchecked")
    public <T extends MessageLite, V extends Future> V sendMessage(Channel channel, T msg, MessageType type, ValueOrNodesFuture existingQueue, UUID uuid) {
//    public <T extends MessageLite, V extends Future> V sendMessage(Channel channel, T msg, MessageType type, Class<V> cls) {
        if (channel != null && channel.isActive()) {
            ValueOrNodesFuture f = new ValueOrNodesFuture(queueMap.get(channel), uuid);
            ProtoMessageWrapper wrapper = new ProtoMessageWrapper(type, msg);
            channel.writeAndFlush(wrapper);
//            return cls.cast(f);
            return (V)f;
        }
        return null;
    }

    public <T extends MessageLite, V extends Future> V sendMessageForMap(Channel channel, T msg, MessageType type, UUID uuid) {
        return sendMessageForMap(channel, msg, type, null, uuid);
    }

    @SuppressWarnings("unchecked")
    public <T extends MessageLite, V extends Future> V sendMessageForMap(Channel channel, T msg, MessageType type, MapOrNodesFuture existingQueue, UUID uuid) {
        if (channel != null && channel.isActive()) {
            MapOrNodesFuture f = new MapOrNodesFuture(queueMap.get(channel), uuid);
            ProtoMessageWrapper wrapper = new ProtoMessageWrapper(type, msg);
            channel.writeAndFlush(wrapper);
            return (V)f;
        }
        return null;
    }

    public <T extends MessageLite, V extends Future> V sendPing(Channel channel, T msg, MessageType type, UUID uuid) {
        return sendPing(channel, msg, type, null, uuid);
    }
    @SuppressWarnings("unchecked")
    public <T extends MessageLite, V extends Future> V sendPing(Channel channel, T msg, MessageType type, PingFuture existingQueue, UUID uuid) {
        if (channel != null && channel.isActive()) {
            PingFuture f = new PingFuture(queueMap.get(channel), uuid);
            ProtoMessageWrapper wrapper = new ProtoMessageWrapper(type, msg);
            channel.writeAndFlush(wrapper);
            return (V)f;
        }
        return null;
    }

    public <T extends MessageLite, V extends Future> V sendSeedRequest(Channel channel, T msg, MessageType type, UUID uuid) {
        return sendSeedRequest(channel, msg, type, null, uuid);
    }
    @SuppressWarnings("unchecked")
    public <T extends MessageLite, V extends Future> V sendSeedRequest(Channel channel, T msg, MessageType type, SeedsOrStatusFuture existingQueue, UUID uuid) {
        if (channel != null && channel.isActive()) {
            SeedsOrStatusFuture f = new SeedsOrStatusFuture(queueMap.get(channel), uuid);
            ProtoMessageWrapper wrapper = new ProtoMessageWrapper(type, msg);
            channel.writeAndFlush(wrapper);
            return (V)f;
        }
        return null;
    }

    private void removeHandlers(Channel channel) {
        ChannelPipeline p = channel.pipeline();
        synchronized (channel.pipeline()) {
            for (String name : p.names()) {
                if (!name.equalsIgnoreCase("ssl") && !name.startsWith("DefaultChannelPipeline")) {
                    p.remove(name);
                }
            }
        }
    }

    public void close(Channel channel) {
        if (channel != null) {
//            try {
//                channel.closeFuture().addListener(ChannelFutureListener.CLOSE);
                channel.close().addListener(ChannelFutureListener.CLOSE);
                queueMap.remove(channel);
//            } catch (InterruptedException e) {
//                log.debug("Unable to close channel", e);
//            }
        }
    }

    public void shutdown() {
        try {
            for (Channel ch : queueMap.keySet()) {
//                ch.closeFuture().sync();
                ch.close().addListener(ChannelFutureListener.CLOSE);
            }
        }
//        } catch (InterruptedException e) {
//            log.debug("Failed to close channel", e);
//        }
        finally {
            group.shutdownGracefully();
        }
    }

    public boolean isConnected(Channel channel) {
        return channel != null && channel.isActive();
    }
}

package dk.orda.overlaynetwork.net.rpc.server;

import dk.orda.overlaynetwork.net.rpc.Protocol;
import dk.orda.overlaynetwork.net.rpc.SuperNode;
import dk.orda.overlaynetwork.net.rpc.protobuf.ProtobufDecoder;
import dk.orda.overlaynetwork.net.rpc.protobuf.ProtobufDecoder2;
import dk.orda.overlaynetwork.net.rpc.protobuf.ProtobufEncoder;
import dk.orda.overlaynetwork.net.rpc.protobuf.ProtobufEncoder2;
import dk.orda.overlaynetwork.net.seed.Seeds;
import dk.orda.overlaynetwork.overlay.RequestOverlay;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ProtocolMultiplexer extends ByteToMessageDecoder {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final SuperNode snReference;
    private final Seeds seeds;

    public ProtocolMultiplexer(SuperNode reference, Seeds seeds) {
        this.snReference = reference;
        this.seeds = seeds;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 1) {
            return;
        }

        if (ctx.pipeline().names().contains("reset")) ctx.pipeline().remove("reset");

        try {
//            final int protocol = (int) in.getByte(in.readerIndex());
            final int protocol = (int) in.readByte();
            in.clear(); // clear out protocol selector
            switch (Protocol.find(protocol)) {
                case PROTOBUF:
                    switchToProtobuf(ctx);
                    break;
                case SEEDS:
                    switchToSeed(ctx);
                    break;
                default:
                    in.clear();
                    ctx.close();
            }
        } catch (Exception e) {
            log.error("Unable to parse incoming data in decoder selector", e);
            in.clear();
            ctx.close();
        }
    }

    private void switchToSeed(ChannelHandlerContext ctx) {
        ChannelPipeline p = ctx.pipeline();
        p.addLast("framedecoder", new LengthFieldBasedFrameDecoder(1048576*10, 0 , 4, 0 , 4)); // max message size = 10MB
        p.addLast("decoder", new ProtobufDecoder2());
        p.addLast("frameencoder", new LengthFieldPrepender(4));
        p.addLast("encoder", new ProtobufEncoder2());
        p.addLast("seedhandler", new SeedHandler(seeds));
        p.remove(this);
    }

    private void switchToProtobuf(ChannelHandlerContext ctx) {
        ChannelPipeline p = ctx.pipeline();
        p.addLast("framedecoder", new LengthFieldBasedFrameDecoder(1048576*10, 0 , 4, 0 , 4)); // max message size = 10MB
        p.addLast("decoder", new ProtobufDecoder2());
        p.addLast("frameencoder", new LengthFieldPrepender(4));
        p.addLast("encoder", new ProtobufEncoder2());
        p.addLast("nodehandler", new FindNodeHandler(snReference));
        p.addLast("valuehandler", new FindValueHandler(snReference));
        p.addLast("storehandler", new StoreValueHandler(snReference));
        p.addLast("addhandler", new AddValueHandler(snReference));
        p.addLast("maphandler", new FindMapHandler(snReference));
        p.addLast("pinghandler", new PingHandler());
        p.remove(this);
    }
}

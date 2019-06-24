package dk.orda.overlaynetwork.net.rpc;

import dk.orda.overlaynetwork.net.rpc.server.ProtocolMultiplexer;
import dk.orda.overlaynetwork.net.seed.Seeds;
import dk.orda.overlaynetwork.overlay.RequestOverlay;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class SslInitializer extends ByteToMessageDecoder {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final SslContext sslContext;
    private final SuperNode sn;
    private final Seeds seeds;

    public SslInitializer(SslContext sslContext, SuperNode reference, Seeds seeds) {
        this.sslContext = sslContext;
        this.sn = reference;
        this.seeds = seeds;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 5) {
            return;
        }

        try {
            // enable SSL
            if (SslHandler.isEncrypted(in)) {
                // if the request is encrypted and this server has not been setup for encryption, then close the connection.
                if (sslContext == null) {
                    log.error("SSL is NOT enabled for this server!");
                    in.clear();
                    ctx.close();
                    return;
                } else {
                    ctx.pipeline().addLast("ssl", sslContext.newHandler(ctx.alloc()));
                }
            }
            ctx.pipeline().addLast("selector", new ProtocolMultiplexer(sn, seeds));
            ctx.pipeline().remove(this);
        }
        catch (Exception e) {
            log.error("Unable to initialize connection", e);
            in.clear();
            ctx.close();
        }
    }
}

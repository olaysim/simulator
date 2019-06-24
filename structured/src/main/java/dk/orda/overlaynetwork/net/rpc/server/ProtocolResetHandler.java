package dk.orda.overlaynetwork.net.rpc.server;

import dk.orda.overlaynetwork.net.rpc.SuperNode;
import dk.orda.overlaynetwork.net.seed.Seeds;
import dk.orda.overlaynetwork.overlay.RequestOverlay;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Sharable
public class ProtocolResetHandler extends ChannelOutboundHandlerAdapter {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final SuperNode snReference;
    private final Seeds seeds;

    public ProtocolResetHandler(SuperNode reference, Seeds seeds) {
        this.snReference = reference;
        this.seeds = seeds;
    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        super.flush(ctx);
        removeHandlers(ctx);
    }

    private void removeHandlers(ChannelHandlerContext ctx) {
        synchronized (ctx.pipeline()) {
            final ChannelPipeline p = ctx.pipeline();
//            if (p.names().contains("framedecoder")) p.remove("framedecoder");
//            if (p.names().contains("frameencoder")) p.remove("frameencoder");
//            if (p.names().contains("encoder")) p.remove("encoder");
//            if (p.names().contains("decoder")) p.remove("decoder");
//            if (p.names().contains("handler")) p.remove("handler");
            for (String name : p.names()) {
                if (!name.equalsIgnoreCase("ssl") && !name.startsWith("DefaultChannelPipeline")) {
                    p.remove(name);
                }
            }
            if (!p.names().contains("selector")) p.addLast("selector", new ProtocolMultiplexer(snReference, seeds));
        }
        System.out.println("ResetDecoder: " + ctx.pipeline().names());
    }
}

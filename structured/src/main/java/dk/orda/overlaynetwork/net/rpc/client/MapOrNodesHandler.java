package dk.orda.overlaynetwork.net.rpc.client;

import dk.orda.overlaynetwork.net.rpc.MessageQueue;
import dk.orda.overlaynetwork.net.rpc.protobuf.RpcMessages;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@ChannelHandler.Sharable
public class MapOrNodesHandler extends SimpleChannelInboundHandler<RpcMessages.MapOrNodes> {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private MessageQueue queue;

    public MapOrNodesHandler(MessageQueue queue) {
        this.queue = queue;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessages.MapOrNodes msg) throws Exception {
        if (msg.getHeader().getCorrelationId() != null && !msg.getHeader().getCorrelationId().isEmpty()) {
            queue.offer(UUID.fromString(msg.getHeader().getCorrelationId()), msg);
        } else {
            queue.offer(msg);
            log.warn("No correlation ID set in received message");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Error in receiving RPC Reply", cause);
        ctx.close();
    }
}

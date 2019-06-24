package dk.orda.overlaynetwork.net.rpc.client;

import dk.orda.overlaynetwork.net.rpc.Protocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public class ProtocolHandler extends MessageToByteEncoder<ByteBuf> {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final Protocol protocol;

    public ProtocolHandler(Protocol protocol) {
        this.protocol = protocol;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        out.writeByte(protocol.getNo());
        out.writeBytes(msg);
        ctx.pipeline().remove(this);
    }
}

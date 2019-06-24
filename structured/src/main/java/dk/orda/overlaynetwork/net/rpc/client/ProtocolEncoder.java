package dk.orda.overlaynetwork.net.rpc.client;

import dk.orda.overlaynetwork.net.rpc.Protocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Sharable
public class ProtocolEncoder extends MessageToByteEncoder<Protocol> {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    protected void encode(ChannelHandlerContext ctx, Protocol protocol, ByteBuf out) throws Exception {
        byte[] msg = { protocol.getNo(), 0, 0, 0, 0 }; // ssl initializer requires 5 bytes minimum
        out.writeBytes(msg);
    }
}

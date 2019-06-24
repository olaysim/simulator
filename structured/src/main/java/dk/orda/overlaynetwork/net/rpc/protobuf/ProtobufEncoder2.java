package dk.orda.overlaynetwork.net.rpc.protobuf;

import com.google.protobuf.MessageLite;
import dk.orda.overlaynetwork.net.rpc.Protocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.netty.buffer.Unpooled.wrappedBuffer;

@Sharable
public class ProtobufEncoder2 extends MessageToMessageEncoder<ProtoMessageWrapper> {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private static final byte PROTOCOL = Protocol.PROTOBUF.getNo();

    @Override
    protected void encode(ChannelHandlerContext ctx, ProtoMessageWrapper msg, List<Object> out) throws Exception {
        byte[] buf;
        if (msg.getMsg() instanceof MessageLite) {
            buf = ((MessageLite) msg.getMsg()).toByteArray();
        }
        else if (msg.getMsg() instanceof MessageLite.Builder) {
            buf = ((MessageLite.Builder) msg.getMsg()).build().toByteArray();
        }
        else {
            return;
        }
        byte[] protocols = { PROTOCOL, msg.getType().getNo() };
        out.add(wrappedBuffer(protocols, buf));
    }
}

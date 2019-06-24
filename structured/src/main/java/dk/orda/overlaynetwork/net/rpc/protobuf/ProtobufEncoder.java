package dk.orda.overlaynetwork.net.rpc.protobuf;

import com.google.protobuf.MessageLite;
import dk.orda.overlaynetwork.net.rpc.Protocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.buffer.Unpooled.wrappedBuffer;

@SuppressWarnings("Duplicates")
@Sharable
public class ProtobufEncoder extends MessageToByteEncoder<ProtoMessageWrapper> {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private static final byte PROTOCOL = Protocol.PROTOBUF.getNo();

    @Override
    protected void encode(ChannelHandlerContext ctx, ProtoMessageWrapper msg, ByteBuf out) throws Exception {
        ByteBuf buf;
        if (msg.getMsg() instanceof MessageLite) {
            buf = wrappedBuffer(((MessageLite) msg.getMsg()).toByteArray());
        }
        else if (msg.getMsg() instanceof MessageLite.Builder) {
            buf = wrappedBuffer(((MessageLite.Builder) msg.getMsg()).build().toByteArray());
        }
        else {
            return;
        }
        out.writeByte(PROTOCOL); // add protocol to header
        int bodyLen = buf.readableBytes();
        int headerLen = computeRawVarint32Size(bodyLen);
        out.ensureWritable(headerLen + bodyLen +1);
        out.writeByte(msg.getType().getNo()); // add custom header
        writeRawVarint32(out, bodyLen);
        out.writeBytes(buf, buf.readerIndex(), bodyLen);
    }

    /**
     * Writes protobuf varint32 to (@link ByteBuf).
     * @param out to be written to
     * @param value to be written
     */
    static void writeRawVarint32(ByteBuf out, int value) {
        while (true) {
            if ((value & ~0x7F) == 0) {
                out.writeByte(value);
                return;
            } else {
                out.writeByte((value & 0x7F) | 0x80);
                value >>>= 7;
            }
        }
    }

    /**
     * Computes size of protobuf varint32 after encoding.
     * @param value which is to be encoded.
     * @return size of value encoded as protobuf varint32.
     */
    static int computeRawVarint32Size(final int value) {
        if ((value & (0xffffffff <<  7)) == 0) {
            return 1;
        }
        if ((value & (0xffffffff << 14)) == 0) {
            return 2;
        }
        if ((value & (0xffffffff << 21)) == 0) {
            return 3;
        }
        if ((value & (0xffffffff << 28)) == 0) {
            return 4;
        }
        return 5;
    }


}

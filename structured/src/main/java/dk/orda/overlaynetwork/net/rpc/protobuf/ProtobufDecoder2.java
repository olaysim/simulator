package dk.orda.overlaynetwork.net.rpc.protobuf;

import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import dk.orda.overlaynetwork.net.rpc.MessageType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@SuppressWarnings("Duplicates")
@ChannelHandler.Sharable
public class ProtobufDecoder2 extends MessageToMessageDecoder<ByteBuf> {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private static final boolean HAS_PARSER;
    private final ExtensionRegistryLite extensionRegistry;

    static {
        boolean hasParser = false;
        try {
            // MessageLite.getParserForType() is not available until protobuf 2.5.0.
            MessageLite.class.getDeclaredMethod("getParserForType");
            hasParser = true;
        } catch (Throwable t) {
            // Ignore
        }

        HAS_PARSER = hasParser;
    }

    public ProtobufDecoder2() {
        extensionRegistry = null;
    }

    public ProtobufDecoder2(MessageLite prototype, ExtensionRegistryLite extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    private MessageLite getPrototype(MessageType type) {
        switch (type) {
            case PING:
                return RpcMessages.Ping.getDefaultInstance();
            case FIND_NODE:
                return RpcMessages.FindNode.getDefaultInstance();
            case FIND_VALUE:
                return RpcMessages.FindValue.getDefaultInstance();
            case STORE_VALUE:
                return RpcMessages.StoreValue.getDefaultInstance();
            case FIND_MAP:
                return RpcMessages.FindMap.getDefaultInstance();
            case GET_SEEDS:
                return RpcMessages.GetSeeds.getDefaultInstance();
            case ADD_VALUE:
                return RpcMessages.AddValue.getDefaultInstance();
            case REPLY_VALUE_OR_NODES:
                return RpcMessages.ValueOrNodes.getDefaultInstance();
            case REPLY_MAP_OR_NODES:
                return RpcMessages.MapOrNodes.getDefaultInstance();
            case REPLY_SEEDS_OR_STATUS:
                return RpcMessages.SeedsOrStatus.getDefaultInstance();
            default:
                return null;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        super.exceptionCaught(ctx, cause);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        in.readByte(); // remove protocol header
        int id = in.readByte();
//        System.out.println("message type: " + id);
        MessageType messageType = MessageType.find(id);

        final byte[] array;
        final int offset;
        final int length = in.readableBytes();
        if (in.hasArray()) {
            array = in.array();
            offset = in.arrayOffset() + in.readerIndex();
        } else {
            array = new byte[length];
            in.getBytes(in.readerIndex(), array, 0, length);
            offset = 0;
        }

        MessageLite prototype = getPrototype(messageType);
        if (prototype == null) {
            throw new NullPointerException("prototype");
        }

        if (extensionRegistry == null) {
            if (HAS_PARSER) {
                out.add(prototype.getParserForType().parseFrom(array, offset, length));
            } else {
                out.add(prototype.newBuilderForType().mergeFrom(array, offset, length).build());
            }
        } else {
            if (HAS_PARSER) {
                out.add(prototype.getParserForType().parseFrom(
                    array, offset, length, extensionRegistry));
            } else {
                out.add(prototype.newBuilderForType().mergeFrom(
                    array, offset, length, extensionRegistry).build());
            }
        }
    }
}

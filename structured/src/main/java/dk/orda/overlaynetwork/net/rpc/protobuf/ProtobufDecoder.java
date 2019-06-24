package dk.orda.overlaynetwork.net.rpc.protobuf;

import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.MessageLite;
import dk.orda.overlaynetwork.net.rpc.MessageType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@SuppressWarnings("Duplicates")
public class ProtobufDecoder extends ByteToMessageDecoder {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private static final boolean HAS_PARSER;
    private final ExtensionRegistryLite extensionRegistry;
    private MessageType messageType = null;

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

    public ProtobufDecoder() {
        extensionRegistry = null;
    }

    public ProtobufDecoder(MessageLite prototype, ExtensionRegistryLite extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    private MessageLite getPrototype(MessageType type) {
        switch (type) {
            case FIND_NODE:
                return RpcMessages.FindNode.getDefaultInstance();
            case FIND_VALUE:
                return RpcMessages.FindValue.getDefaultInstance();
            case STORE_VALUE:
                return RpcMessages.StoreValue.getDefaultInstance();
            case REPLY_VALUE_OR_NODES:
                return RpcMessages.ValueOrNodes.getDefaultInstance();
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
        if (messageType == null) {
            in.readByte(); // remove protocol header
            int id = in.readByte();
            System.out.println("message type: " + id);
            messageType = MessageType.find(id);
        }

        in.markReaderIndex();
        int preIndex = in.readerIndex();
        int length = readRawVarint32(in);
        if (preIndex == in.readerIndex()) {
            return;
        }
        if (length < 0) {
            throw new CorruptedFrameException("negative length: " + length);
        }

        if (in.readableBytes() < length) {
            in.resetReaderIndex();
        } else {
            ByteBuf buf = in.readRetainedSlice(length);

            final byte[] array;
            final int offset;
            final int blength = buf.readableBytes();
            if (buf.hasArray()) {
                array = buf.array();
                offset = buf.arrayOffset() + buf.readerIndex();
            } else {
                array = new byte[blength];
                buf.getBytes(buf.readerIndex(), array, 0, blength);
                offset = 0;
            }

            MessageLite prototype = getPrototype(messageType);
            messageType = null;
            if (prototype == null) {
                throw new NullPointerException("prototype");
            }

            if (extensionRegistry == null) {
                if (HAS_PARSER) {
                    out.add(prototype.getParserForType().parseFrom(array, offset, blength));
                } else {
                    out.add(prototype.newBuilderForType().mergeFrom(array, offset, blength).build());
                }
            } else {
                if (HAS_PARSER) {
                    out.add(prototype.getParserForType().parseFrom(
                        array, offset, blength, extensionRegistry));
                } else {
                    out.add(prototype.newBuilderForType().mergeFrom(
                        array, offset, blength, extensionRegistry).build());
                }
            }
        }
    }

    /**
     * Reads variable length 32bit int from buffer
     *
     * @return decoded int if buffers readerIndex has been forwarded else nonsense value
     */
    private static int readRawVarint32(ByteBuf buffer) {
        if (!buffer.isReadable()) {
            return 0;
        }
        buffer.markReaderIndex();
        byte tmp = buffer.readByte();
        if (tmp >= 0) {
            return tmp;
        } else {
            int result = tmp & 127;
            if (!buffer.isReadable()) {
                buffer.resetReaderIndex();
                return 0;
            }
            if ((tmp = buffer.readByte()) >= 0) {
                result |= tmp << 7;
            } else {
                result |= (tmp & 127) << 7;
                if (!buffer.isReadable()) {
                    buffer.resetReaderIndex();
                    return 0;
                }
                if ((tmp = buffer.readByte()) >= 0) {
                    result |= tmp << 14;
                } else {
                    result |= (tmp & 127) << 14;
                    if (!buffer.isReadable()) {
                        buffer.resetReaderIndex();
                        return 0;
                    }
                    if ((tmp = buffer.readByte()) >= 0) {
                        result |= tmp << 21;
                    } else {
                        result |= (tmp & 127) << 21;
                        if (!buffer.isReadable()) {
                            buffer.resetReaderIndex();
                            return 0;
                        }
                        result |= (tmp = buffer.readByte()) << 28;
                        if (tmp < 0) {
                            throw new CorruptedFrameException("malformed varint.");
                        }
                    }
                }
            }
            return result;
        }
    }
}

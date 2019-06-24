package dk.orda.overlaynetwork.net.rpc.protobuf;

import com.google.protobuf.MessageLite;
import dk.orda.overlaynetwork.net.rpc.MessageType;

public class ProtoMessageWrapper {
    private final MessageType type;
    private final MessageLite msg;

    public ProtoMessageWrapper(MessageType type, MessageLite msg) {
        this.type = type;
        this.msg = msg;
    }

    public MessageType getType() {
        return type;
    }

    public MessageLite getMsg() {
        return msg;
    }
}

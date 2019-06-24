package dk.orda.overlaynetwork.net.rpc;

// DO NOT CHANGE THE ORDER IN THE ENUMMERATION !
// DO NOT (as a rule) REMOVE ITEMS FROM THE ENUMMERATION !
// In order to be cross-platform compatible, the order and items must remain the same. (items can be added)

public enum MessageType {
    NOT_INITIALIZED,
    PING,
    FIND_NODE,
    FIND_VALUE,
    STORE_VALUE,
    ADD_VALUE,
    FIND_HUB_NODE,
    FIND_MAP,
    FIND_LIST,
    GET_SEEDS,
    REPLY_VALUE_OR_NODES,
    REPLY_MAP_OR_NODES,
    REPLY_LIST_OR_NODES,
    REPLY_SEEDS_OR_STATUS,
    OTHER;

    public byte getNo() {
        return (byte) ordinal();
    }

    public static MessageType find(int no) {
        return values()[no];
    }

    public static String toString(int no) {
        return values()[no].name();
    }

    @Override
    public String toString() {
        return values()[ordinal()].name();
    }
}

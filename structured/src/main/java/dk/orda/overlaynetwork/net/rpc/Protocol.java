package dk.orda.overlaynetwork.net.rpc;

// DO NOT CHANGE THE ORDER IN THE ENUMMERATION !
// DO NOT (as a rule) REMOVE ITEMS FROM THE ENUMMERATION !
// In order to be cross-platform compatible, the order and items must remain the same. (items can be added)

public enum Protocol {
    NOT_INITIALIZED,
    PROTOBUF,
    SEEDS,
    FILE,
    OTHER;

    public byte getNo() {
        return (byte) ordinal();
    }

    public static Protocol find(int no) {
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

package dk.orda.overlaynetwork.overlay.dht;

public enum DistributedHashTableType {
    DEFAULT (0),
    GEO (1);

    private final int type;
    DistributedHashTableType(int type) {
        this.type = type;
    }

}

package dk.orda.overlaynetwork.overlay;

import dk.orda.overlaynetwork.overlay.dht.Node;
import dk.orda.overlaynetwork.overlay.dht.Number160;
import dk.orda.overlaynetwork.statistics.RoutingStatistic;
import dk.orda.overlaynetwork.statistics.StatLogger;

import java.util.List;
import java.util.Map;

public interface OverlayNetwork {

    void start();
    void stop();

    Node findNode(Number160 key);

    void storeValue(Number160 key, byte[] value, int nodestorecount);
    byte[] findValue(Number160 key);

    void storeValueInMap(Number160 key, String index, byte[] value);
    Map<String, byte[]> findMap(Number160 key);


    // TODO: Until connection bug is fixed
    void warmupConnection(String address);
    void resetDHT();

    void saveDhtState();
    void reloadDhtState();

    RequestOverlay getRequestOverlay();

    StatLogger getStatLogger();
    void logDht();

    Node findNodeLocal(Node target, double lat, double lon);
}

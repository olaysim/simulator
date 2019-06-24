package dk.orda.overlaynetwork.overlay;

import dk.orda.overlaynetwork.net.Address;
import dk.orda.overlaynetwork.net.NetworkConfiguration;
import dk.orda.overlaynetwork.net.NetworkServer;
import dk.orda.overlaynetwork.net.rpc.SuperNode;
import dk.orda.overlaynetwork.overlay.dht.DistributedHashTableConfiguration;
import dk.orda.overlaynetwork.overlay.dht.RoutingProtocol;
import dk.orda.overlaynetwork.overlay.peer.Peer;
import dk.orda.overlaynetwork.overlaygeo.GeoKademliaOverlay;
import dk.orda.overlaynetwork.overlaygeo.GeoRoutingProtocol;
import dk.orda.overlaynetwork.statistics.StatConfiguration;
import dk.orda.overlaynetwork.statistics.StatLogger;
import dk.orda.overlaynetwork.storage.MemoryStorage;
import dk.orda.overlaynetwork.storage.StorageLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("Duplicates")
public class OverlayFactory {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public static OverlayNetwork createKademlia(String id) {
        return createKademlia(id, null, null, null, null);
    }
    public static OverlayNetwork createKademlia(String id, NetworkConfiguration netConf, DistributedHashTableConfiguration dhtConf, SuperNode superNode, StatLogger logger) {
        // create layers
        if (netConf == null) netConf = new NetworkConfiguration();
        if (dhtConf == null) dhtConf = new DistributedHashTableConfiguration();

        // TODO: USE FIXED PORT UNTIL CONNECTION BUG IS FIXED
//        Address address = new Address( true);
        Address address = new Address("127.0.0.1", 65487, true);
        Peer peer = new Peer();
        peer.setAddress(address); // prefer ipv4

        StatLogger statLogger = logger;
        if (statLogger == null) {
            StatConfiguration statConfiguration = new StatConfiguration(null, null);
            statLogger = new StatLogger(statConfiguration);
        }

        NetworkServer srv = new NetworkServer(netConf, statLogger);
        SuperNode sn;
        if (superNode != null) {
            sn = superNode;
        } else {
            sn = new SuperNode(address, srv, statLogger);
        }
        RoutingProtocol rp = new RoutingProtocol(sn);
        StorageLayer storage = new MemoryStorage();

        // create kadelia overlay network
        KademliaOverlay overlay = KademliaOverlay.newBuilder()
            .distributedHashTable(id, dhtConf, statLogger)
            .routingProtocol(rp)
            .superNode(sn)
            .storageLayer(storage)
            .peer(peer)
            .statLogger(statLogger)
            .build();

        // setup final cross references
        rp.setNetworkOverlay(overlay);
        sn.registerOverlay(id, overlay);

        return overlay;
    }





    public static OverlayNetwork createGeoKademlia(String id, double lat, double lon, NetworkConfiguration netConf, DistributedHashTableConfiguration dhtConf, SuperNode superNode, StatLogger logger) {
        // create layers
        if (netConf == null) netConf = new NetworkConfiguration();
        if (dhtConf == null) dhtConf = new DistributedHashTableConfiguration();

        // TODO: USE FIXED PORT UNTIL CONNECTION BUG IS FIXED
//        Address address = new Address( true);
        Address address = new Address("127.0.0.1", 65487, true);
        Peer peer = new Peer();
        peer.setAddress(address); // prefer ipv4

        StatLogger statLogger = logger;
        if (statLogger == null) {
            StatConfiguration statConfiguration = new StatConfiguration(null, null);
            statLogger = new StatLogger(statConfiguration);
        }

        NetworkServer srv = new NetworkServer(netConf, statLogger);
        SuperNode sn;
        if (superNode != null) {
            sn = superNode;
        } else {
            sn = new SuperNode(address, srv, statLogger);
        }
        GeoRoutingProtocol rp = new GeoRoutingProtocol(sn);
        StorageLayer storage = new MemoryStorage();

        // create kadelia overlay network
        GeoKademliaOverlay overlay = GeoKademliaOverlay.newBuilder()
            .distributedHashTable(id, lat, lon, dhtConf, statLogger)
            .routingProtocol(rp)
            .superNode(sn)
            .storageLayer(storage)
            .peer(peer)
            .statLogger(statLogger)
            .build();

        // setup final cross references
        rp.setNetworkOverlay(overlay);
        sn.registerOverlay(id, overlay);

        return overlay;
    }


}

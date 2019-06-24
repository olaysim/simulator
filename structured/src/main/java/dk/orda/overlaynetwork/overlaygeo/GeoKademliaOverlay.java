package dk.orda.overlaynetwork.overlaygeo;

import dk.orda.overlaynetwork.net.rpc.SuperNode;
import dk.orda.overlaynetwork.overlay.OverlayNetwork;
import dk.orda.overlaynetwork.overlay.RequestOverlay;
import dk.orda.overlaynetwork.overlay.dht.*;
import dk.orda.overlaynetwork.overlay.peer.Peer;
import dk.orda.overlaynetwork.statistics.DhtStates;
import dk.orda.overlaynetwork.statistics.StatConfiguration;
import dk.orda.overlaynetwork.statistics.StatLogger;
import dk.orda.overlaynetwork.storage.StorageLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

public class GeoKademliaOverlay implements OverlayNetwork, RequestOverlay {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private GeoRoutingProtocol routingProtocol;
    private SuperNode superNode;
    private DistributedHashTable distributedHashTable;
    private StorageLayer storageLayer;
    private Node self;
    private StatLogger statLogger;
    private final Object lock = new Object();

    private GeoKademliaOverlay(Builder builder) {
        routingProtocol = builder.routingProtocol;
        superNode = builder.superNode;
        distributedHashTable = builder.distributedHashTable;
        storageLayer = builder.storageLayer;
        statLogger = builder.statLogger;

        GeoDistributedHashTableImpl dht = (GeoDistributedHashTableImpl)distributedHashTable;
        this.self = new Node(dht.getNodeId(), dht.getNodeIdStr());
        this.self.setPeer(builder.peer);

        if (this.statLogger == null) { this.statLogger = new StatLogger(new StatConfiguration()); }
        this.statLogger.setVirtualid(this.getSelf().getId().toString());
    }

    @Override
    public void start() {
        superNode.start();
    }

    @Override
    public void stop() {
        superNode.stop();
    }

    @Override
    public void resetDHT() {
        distributedHashTable = DistributedHashTableFactory.createGeoDistributedHashTable(self.getId(), Double.parseDouble(self.getDict().get("x")), Double.parseDouble(self.getDict().get("y")), self.getIdStr(), statLogger);
        superNode.reset();
    }

    @Override
    public void saveDhtState() {
        ((GeoDistributedHashTableImpl)this.distributedHashTable).saveState();
    }

    @Override
    public void reloadDhtState() {
        ((GeoDistributedHashTableImpl)this.distributedHashTable).reloadState();
    }

    @Override
    public Node findNode(Number160 key) {
        return routingProtocol.iterativeFindNode(key, 0, 0, (GeoDistributedHashTableImpl)distributedHashTable);
    }

    public Node findNode(Number160 key, double lat, double lon) {
        return routingProtocol.iterativeFindNode(key, lat, lon, (GeoDistributedHashTableImpl)distributedHashTable);
    }

    @Override
    public Node findNodeLocal(Node target, double lat, double lon) {
        return routingProtocol.iterativeFindNodeLocal(target, lat, lon, (GeoDistributedHashTableImpl)distributedHashTable);
    }

    @Override
    public void storeValue(Number160 key, byte[] value, int nodestorecount) {
        routingProtocol.iterativeStore(key, value, (GeoDistributedHashTableImpl)distributedHashTable, nodestorecount);
    }

    @Override
    public byte[] findValue(Number160 key) {
        return routingProtocol.iterativeFindValue(key, (GeoDistributedHashTableImpl)distributedHashTable);
    }

    @Override
    public SortedSet<Node> getNodes(Number160 id, Map<String, String> ext) {
        synchronized (lock) {
            return ((GeoDistributedHashTableImpl) distributedHashTable).getClosestNodes(id, ext);
        }
    }

    @Override
    public SortedSet<Node> getNodes(Node targetNode) {
        synchronized (lock) {
            return ((GeoDistributedHashTableImpl) distributedHashTable).getClosestNodesLocal(targetNode);
        }
    }

    @Override
    public void storeValueInMap(Number160 key, String index, byte[] value) {
        routingProtocol.iterativeStoreMapValue(key, index, value, (GeoDistributedHashTableImpl)distributedHashTable);
    }

    @Override
    public Map<String, byte[]> findMap(Number160 key) {
        return routingProtocol.iterativeFindMap(key, (GeoDistributedHashTableImpl)distributedHashTable);
    }

    @Override
    @Deprecated
    public void warmupConnection(String address) {
        superNode.warmupConnection(address);
    }

    @Override
    public byte[] getValue(Number160 key) {
        if (storageLayer.hasKey(key)) {
            return storageLayer.getValue(key);
        }
        else {
            return null;
        }
    }

    @Override
    public boolean hasValue(Number160 key) {
        return storageLayer.hasKey(key);
    }

    @Override
    public void setValue(Number160 key, byte[] value) {
        storageLayer.setValue(key, value);
    }

    @Override
    public void addValueToList(Number160 key, byte[] value) {
        storageLayer.addValueToList(key, value);
    }

    @Override
    public boolean removeValueFromList(Number160 key, byte[] value) {
        return storageLayer.removeValueFromList(key, value);
    }

    @Override
    public boolean listsHasKey(Number160 key) {
        return storageLayer.listsHasKey(key);
    }

    @Override
    public boolean listHasValue(Number160 key, byte[] value) {
        return storageLayer.listHasValue(key, value);
    }

    @Override
    public List<byte[]> getList(Number160 key) {
        return storageLayer.getList(key);
    }

    @Override
    public void putValueInMap(Number160 key, String index, byte[] value) {
        storageLayer.putValueInMap(key, index, value);
    }

    @Override
    public boolean removeIndexFromMap(Number160 key, String index) {
        return storageLayer.removeIndexFromMap(key, index);
    }

    @Override
    public boolean mapsHasKey(Number160 key) {
        return storageLayer.mapsHasKey(key);
    }

    @Override
    public boolean mapHasIndex(Number160 key, String index) {
        return storageLayer.mapHasIndex(key, index);
    }

    @Override
    public Map<String, byte[]> getMap(Number160 key) {
        return storageLayer.getMap(key);
    }

    @Override
    public byte[] getMapValue(Number160 key, String index) {
        return storageLayer.getMapValue(key, index);
    }

    @Override
    public DistributedHashTable getDHT() {
        return distributedHashTable;
    }

    @Override
    public Node getSelf() {
        return self;
    }

    @Override
    public boolean updateNode(Node node) {
        synchronized (lock) {
            return ((GeoDistributedHashTableImpl) distributedHashTable).updateNode(node);
        }
    }

    @Override
    public boolean updateRandom(Node node) {
        synchronized (lock) {
            return ((GeoDistributedHashTableImpl) distributedHashTable).updateRandomNode(node);
        }
    }

    @Override
    public boolean updateHub(Node node) {
        synchronized (lock) {
            return ((GeoDistributedHashTableImpl) distributedHashTable).updateHubNode(node);
        }
    }

    public SuperNode getSuperNode() {
        return superNode;
    }

    public void setInitialization(boolean underInitialization) {
        ((GeoDistributedHashTableImpl)distributedHashTable).setUnderInitialization(underInitialization);
    }

// Builder pattern

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private GeoRoutingProtocol routingProtocol;
        private SuperNode superNode;
        private DistributedHashTable distributedHashTable;
        private StorageLayer storageLayer;
        private Peer peer;
        private StatLogger statLogger;

        private Builder() {
        }

        public Builder routingProtocol(GeoRoutingProtocol val) {
            routingProtocol = val;
            return this;
        }

        public Builder superNode(SuperNode val) {
            superNode = val;
            return this;
        }

        public Builder distributedHashTable(String id, double lat, double lon, StatLogger statLogger) {
            distributedHashTable = DistributedHashTableFactory.createGeoDistributedHashTable(Number160.createHash(statLogger, id), lat, lon, id, statLogger);
            return this;
        }

        public Builder distributedHashTable(String id, double lat, double lon, DistributedHashTableConfiguration configuration, StatLogger statLogger) {
            distributedHashTable = DistributedHashTableFactory.createGeoDistributedHashTable(Number160.createHash(statLogger, id), lat, lon, id, configuration, statLogger);
            return this;
        }

        public Builder storageLayer(StorageLayer val) {
            storageLayer = val;
            return this;
        }

        public Builder peer(Peer val) {
            peer = val;
            return this;
        }

        public Builder statLogger(StatLogger val) {
            statLogger = val;
            return this;
        }

        public GeoKademliaOverlay build() {
            return new GeoKademliaOverlay(this);
        }
    }

    public int getKBucketCount() {
        return ((GeoDistributedHashTableImpl)distributedHashTable).valuesInBuckets();
    }

    public Map<String, List<List<String>>> getFormattedKBuckets() {
        return ((GeoDistributedHashTableImpl)distributedHashTable).getFormattedKBuckets();
    }



    @Override
    public RequestOverlay getRequestOverlay() {
        return (RequestOverlay)this;
    }

    @Override
    public StatLogger getStatLogger() {
        return this.statLogger;
    }


    public DhtStates dhtStates = null;
    @Override
    public void logDht() {
        if (dhtStates == null) dhtStates = new DhtStates(statLogger);
        dhtStates.add(getFormattedKBuckets());
        dhtStates.addToStatLogger(statLogger.getConfig(), "manual_dhtstates");
    }
}

package dk.orda.overlaynetwork.overlaygeo;

import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;
import dk.orda.overlaynetwork.overlay.dht.DistributedHashTable;
import dk.orda.overlaynetwork.overlay.dht.DistributedHashTableConfiguration;
import dk.orda.overlaynetwork.overlay.dht.Node;
import dk.orda.overlaynetwork.overlay.dht.Number160;
import dk.orda.overlaynetwork.statistics.StatLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@SuppressWarnings("Duplicates")
public class GeoDistributedHashTableImpl implements DistributedHashTable {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final Number160 nodeId;
    private final String idStr;
    private final DistributedHashTableConfiguration config;
    private StatLogger statLogger;

    private double lat;
    private double lon;
//    private SortedMultiset<Node> geobucket;
//    private SortedSet<Node> geobucket;
    private SmallWorldBucket smallBucket;
    private boolean underInitialization; // not used anymore

    private HashMap<Number160, Object> storage;

	public GeoDistributedHashTableImpl(final Number160 nodeId, final double lat, final double lon, final String idStr, DistributedHashTableConfiguration config, StatLogger statLogger) {
	    this.nodeId = nodeId;
	    this.idStr = idStr;
        this.config = config;

	    this.statLogger = statLogger;
	    this.storage = new HashMap<>();

        this.lat = lat;
        this.lon = lon;

//        this.geobucket = TreeMultiset.create(getPythagoranNodeComparator(lat, lon));
//        this.geobucket = new TreeSet<>(getGeoNodeComparator(lat, lon));
        this.smallBucket = new SmallWorldBucket(nodeId, lat, lon);
    }

    public void saveState() {
	    smallBucket.saveState();
    }

    public void reloadState() {
	    smallBucket.reloadState();
    }

    public boolean isUnderInitialization() {
        return underInitialization;
    }

    public void setUnderInitialization(boolean underInitialization) {
        this.underInitialization = underInitialization;
    }

    public Number160 getNodeId() {
        return nodeId;
    }

    public String getNodeIdStr() { return idStr; }

    public DistributedHashTableConfiguration getConfiguration() {
        return config;
    }

    public boolean updateNode(Node node) {
	    return smallBucket.updateNode(node, !underInitialization);

//	    // don't add self to buckets
//	    if (nodeId.equals(node.getId())) return false;
//
//
//
//	    boolean exists = false;
//        for (Node n : geobucket) {
//            if (n.getId().toString().equalsIgnoreCase(node.getId().toString())) {
//                exists = true;
//                n.setPeer(node.getPeer());
//            }
//        }
//
//        if (!exists) {
//            int before = geobucket.size();
//            geobucket.add(node);
//            if (before == geobucket.size()) {
//                log.error("GEO DHT DID NOT LEARN ABOUT " + node.getIdStr() + " IN UPDATENODE()");
//                geobucket.add(node);
//            }
//        }
//        return true;
    }

    public boolean updateRandomNode(Node node) {
        return smallBucket.addRandomNode(node);
    }

    public boolean updateHubNode(Node node) {
        return smallBucket.addHubNode(node);
    }

    public boolean failNode(Node node) {
        return true;
    }

    public boolean removeNode(Node node) {
	    return false;
    }

    public boolean containsNode(final Node node) {
//        final int classMember = classMember(peerAddress.getPeerId());
//        if (classMember == -1) {
//            // -1 means we searched for ourself and we never are our neighbor
//            return false;
//        }
//        final Map<Number160, PeerStatistic> tmp = peerMapVerified.get(classMember);
//        synchronized (tmp) {
//            return tmp.containsKey(peerAddress.getPeerId());
//        }
        return false;
    }

    public SortedSet<Node> getClosestNodes(final Number160 id, Map<String, String> ext) {
    	return getClosestNodes(nodeId, id, config.getKR(), smallBucket.getGeobucket(), ext);
    }

    public static SortedSet<Node> getClosestNodes(final Number160 locId, final Number160 targetId, final int count, SortedSet<Node> list, Map<String, String> ext) {
	    double xlat = Double.parseDouble(ext.get("x"));
	    double xlon = Double.parseDouble(ext.get("y"));
        Comparator<Node> comparator = getGeoNodeComparator(xlat, xlon);
        final SortedSet<Node> set = new TreeSet<>(comparator);
        SortedSet<Node> newlist = new TreeSet<>(comparator);
        newlist.addAll(list);
        return fillGeoSet(count, set, newlist);
    }

    public SortedSet<Node> getClosestNodesLocal(Node targetNode) {
        LatLon latLon = targetNode.tryGetLatLon();
        Comparator<Node> comparator = getGeoNodeComparator(latLon.getLat(), latLon.getLon());
        final SortedSet<Node> set = new TreeSet<>(comparator);
        SortedSet<Node> newlist = new TreeSet<>(comparator);
        newlist.addAll(smallBucket.getGeobucket());
        return fillGeoSet(config.getKR(), set, newlist);
    }

    public SortedSet<Node> getOldestNodes(final Number160 targetId, double lat, double lon, final int count) {
        return getOldestNodes(nodeId, targetId, lat, lon, count, smallBucket.getGeobucket());
    }

    public static SortedSet<Node> getOldestNodes(final Number160 locId, final Number160 targetId, double lat, double lon, final int count, SortedSet<Node> list) {
        Comparator<Node> comparator = getGeoNodeComparator(lat, lon);
        final SortedSet<Node> set = new TreeSet<>(comparator);
        SortedSet<Node> newlist = new TreeSet<>(comparator);
        newlist.addAll(list);
        SortedSet<Node> nodes = fillGeoSet(count, set, newlist);
        return nodes;
    }

    private static SortedSet<Node> fillGeoSet(final int count,  SortedSet<Node> set, SortedSet<Node> geobucket) {
        Iterator<Node> iterator = geobucket.iterator();
        int i = 0;
        while (i < count && iterator.hasNext()) {
            Node next = iterator.next();
            set.add(next);
            i++;
        }
        return set;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("This node ");
        sb.append(nodeId).append(" (" + idStr + ")").append("\n").append("geobucket: ");
        synchronized (smallBucket.getGeobucket()) {
            for (Node node : smallBucket.getGeobucket()) {
                sb.append("node:").append(node.getId()).append(",");
            }
        }
//        synchronized (geobucket) {
//            for (Node node : geobucket) {
//                sb.append("node:").append(node.getId()).append(",");
//            }
//        }
        return sb.toString();
    }


    public static Comparator<Node> getGeoNodeComparator(final double lat, final double lon) {
        return new Comparator<Node>() {
            private final Logger log = LoggerFactory.getLogger(this.getClass());

            @Override
            public int compare(Node node1, Node node2) {
                if (node1.getId() != null && node2.getId() != null) {
                    if (node1.getId().compareTo(node2.getId()) == 0)
                        return 0; // if the keys are the same, this IS the same node so return 0
                }
                LatLon latLon1 = node1.tryGetLatLon();
                LatLon latLon2 = node2.tryGetLatLon();
                if (latLon1 == null || latLon2 == null) {
                    log.error("Unable to calculate distance between nodes, x or y was not supplied in request!");
                    return node1.getId().compareTo(node2.getId()); // fall back to ID XOR distance
                }
//                    double node1_dist = DistanceCalculator.distance(lat, lon, node1_lat, node1_lon);
//                    double node2_dist = DistanceCalculator.distance(lat, lon, node2_lat, node2_lon);
                double node1_dist = DistanceCalculator.pythagoreanDistance(lat, lon, latLon1.getLat(), latLon1.getLon());
                double node2_dist = DistanceCalculator.pythagoreanDistance(lat, lon, latLon2.getLat(), latLon2.getLon());
                if (node1_dist < node2_dist) return -1;
                if (node1_dist > node2_dist) return 1;
                // if distance is the same, use ID XOR to differantiate nodes
                // multiple nodes CAN have the same distance and should be added to the SET, but TOTAL ORDER is critical, the uniqueness of the ID XOR ensures this
                return node1.getId().compareTo(node2.getId());
            }
        };
    }



    public static Comparator<Node> getOldestNodeComparator() {
	    return new Comparator<Node>() {
            @Override
            public int compare(Node n1, Node n2) {
                if (n1 != null && n2 != null && n1.getPeerStatistics() != null && n2.getPeerStatistics() != null) {
                    if (n1.getPeerStatistics().getTimestamp() < n2.getPeerStatistics().getTimestamp()) return -1;
                    if (n1.getPeerStatistics().getTimestamp() > n2.getPeerStatistics().getTimestamp()) return 1;
                }
                return 0;
            }
        };
    }


    public Object getValue(Number160 key) {
        return storage.getOrDefault(key, null);
    }

    public void setValue(Number160 key, Object obj) {
	    storage.put(key, obj);
    }

    public int valuesInBuckets() {
	    return smallBucket.getGeobucket().size();
//	    return geobucket.size();
    }

    public Map<String, List<List<String>>> getFormattedKBuckets() {
	    Map<String, List<List<String>>> map = new HashMap<>();
	    List<List<String>> list = new ArrayList<>();
	    map.put(nodeId.toString(), list);
	    List<String> nodes = new ArrayList<>();
	    List<String> idStrs = new ArrayList<>();
	    list.add(nodes);
	    list.add(idStrs);


        for (Node node : smallBucket.getGeobucket()) {
            nodes.add(node.getId().toString());
            idStrs.add(node.getIdStr());
        }
//        for (Node node : geobucket) {
//            nodes.add(node.getId().toString());
//            idStrs.add(node.getIdStr());
//        }

        return map;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public SmallWorldBucket getSmallWorldBucket() {
        return smallBucket;
    }
}

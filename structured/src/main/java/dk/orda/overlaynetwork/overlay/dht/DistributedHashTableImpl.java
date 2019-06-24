package dk.orda.overlaynetwork.overlay.dht;

import dk.orda.overlaynetwork.statistics.StatConfiguration;
import dk.orda.overlaynetwork.statistics.StatLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

public class DistributedHashTableImpl implements DistributedHashTable {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final Number160 nodeId;
    private final String idStr;
    private List<Map<Number160, Node>> kBuckets;
    private List<Map<Number160, Node>> savedKBuckets;
    private final DistributedHashTableConfiguration config;
    private StatLogger statLogger;
    private Random rand;

    private HashMap<Number160, Object> storage;

	public DistributedHashTableImpl(final Number160 nodeId, final String idStr, DistributedHashTableConfiguration config, StatLogger statLogger) {
	    this.nodeId = nodeId;
	    this.idStr = idStr;
        this.config = config;
	    this.kBuckets = initKBuckets();

	    this.statLogger = statLogger;
	    this.storage = new HashMap<>();

        this.rand = new Random();
    }

    private List<Map<Number160, Node>> initKBuckets() {
	    List<Map<Number160, Node>> list = new ArrayList<>();
	    for (int i = 0; i < Number160.BITS; i++) {
            list.add(new HashMap<>());
        }
        return Collections.unmodifiableList(list);
    }

    public void saveState() {
        savedKBuckets = initKBuckets();
        for (int i = 0; i < savedKBuckets.size(); i++) {
            savedKBuckets.get(i).putAll(kBuckets.get(i));
        }
    }

    public void reloadState() {
        kBuckets = initKBuckets();
        for (int i = 0; i < kBuckets.size(); i++) {
            kBuckets.get(i).putAll(savedKBuckets.get(i));
        }
    }

    public Number160 getNodeId() {
        return nodeId;
    }

    public String getNodeIdStr() { return idStr; }

    public List<Map<Number160, Node>> getBuckets() {
        return kBuckets;
    }

    public DistributedHashTableConfiguration getConfiguration() {
        return config;
    }

    private int getKBucket(final Number160 id) {
        return getKBucket(getNodeId(), id);
    }

    public static int getKBucket(final Number160 first, final Number160 second) {
        return distance(first, second).bitLength() - 1;
    }

    Comparator<Node> newestComparator = null;
    public boolean updateNode(Node node) {
	    // don't add self to buckets
	    if (nodeId.equals(node.getId())) return false;

        final int kBucket = getKBucket(node.getId());
        final Map<Number160, Node> map = kBuckets.get(kBucket);

        // update or add
        if (map.containsKey(node.getId())) {
//            PeerStatistic old = map.get(remotePeer.getPeerId());
//            if (old != null) {
//                old.peerAddress(remotePeer);
//                old.addRTT(null);
//                old.successfullyChecked();
//                old.increaseNumberOfResponses();
//            }
            Node current = map.get(node.getId());
            current.setPeer(node.getPeer());
            current.getPeerStatistics().setTimestamp(System.currentTimeMillis());
        }
        else {
//            final PeerStatistic peerStatistic = new PeerStatistic(remotePeer);
//            peerStatistic.successfullyChecked();
//            peerStatistic.addRTT(null);

            // check bucket size
            // kademlia sepc says that nodes that don't response are to be removed, however all nodes responds in testing,
            // so remove newest node with a 50% chance
            if (map.size() > config.getBucketSize()) {
                if (rand.nextInt(100) >= 50) {
                    if (newestComparator == null) {
                        newestComparator = getNewestNodeComparator();
                    }
                    final SortedSet<Node> set = new TreeSet<Node>(newestComparator);
                    set.addAll(map.values());
                    map.remove(set.first().getId());
                    set.clear();

                    map.put(node.getId(), node);
                    node.getPeerStatistics().setTimestamp(System.currentTimeMillis());
                }
//                log.info("bucket size reached, size: " + valuesInBuckets());
            } else {
                map.put(node.getId(), node);
                node.getPeerStatistics().setTimestamp(System.currentTimeMillis());
            }
        }

        return true;
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
        return true;
    }


    public SortedSet<Node> getClosestNodes() {
        return getClosestNodes(nodeId);
    }

    public SortedSet<Node> getClosestNodes(final Number160 id) {
    	return getClosestNodes(nodeId, id, config.getKR(), kBuckets);
    }

    public static SortedSet<Node> getClosestNodes(final Number160 locId, final Number160 targetId, final int count, List<Map<Number160, Node>> list) {
        Comparator<Node> comparator = getXORNodeComparator(targetId);
        final SortedSet<Node> set = new TreeSet<Node>(comparator);
        final int kBucket = getKBucket(locId, targetId);
        return fillSet(set, list, kBucket, count);
    }

    public SortedSet<Node> getClosestNodesLocal(Node targetNode) {
        Comparator<Node> comparator = getXORNodeComparator(targetNode.getId());
        final SortedSet<Node> set = new TreeSet<Node>(comparator);
        final int kBucket = getKBucket(nodeId, targetNode.getId());
        return fillSet(set, kBuckets, kBucket, config.getKR());
    }

    public SortedSet<Node> getOldestNodes(final Number160 targetId, final int count) {
        return getOldestNodes(nodeId, targetId, count, kBuckets);
    }

    public static SortedSet<Node> getOldestNodes(final Number160 locId, final Number160 targetId, final int count, List<Map<Number160, Node>> list) {
        Comparator<Node> comparator = getOldestNodeComparator();
        final SortedSet<Node> set = new TreeSet<Node>(comparator);
        final int kBucket = getKBucket(locId, targetId);
        return fillSet(set, list, kBucket, count);
    }

    private static SortedSet<Node> fillSet(final SortedSet<Node> set, List<Map<Number160, Node>> list, final int kBucket, final int count) {
        // bucket = -1, happens when asking for nodes closest to self
        if (kBucket == -1) {
            for (int i = 0; i < list.size(); i++) {
                final Map<Number160, Node> map = list.get(i);
                for (Node node : map.values()) {
                    set.add(node);
                    if (set.size() >= count) {
                        return set;
                    }
                }
            }
            return set;
        }

        // fill set using target kbucket first
        for (Node node : list.get(kBucket).values()) {
            set.add(node);
            if (set.size() >= count) return set;
        }

        // and then grab from neighbour buckets until k is reached
        if (set.size() < count) {
            int i = kBucket - 1;
            int j = kBucket + 1;
            int size = list.size();
            while ((i > 0) != (j < size) || i > 0) {
                if (i >= 0) {
                    for (Node node : list.get(i).values()) {
                        set.add(node);
                        if (set.size() >= count) return set;
                    }
                    i--;
                }
                if (j < size) {
                    for (Node node : list.get(j).values()) {
                        set.add(node);
                        if (set.size() >= count) return set;
                    }
                    j++;
                }
            }
        }
        return set;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("This node ");
        sb.append(nodeId).append("(" + idStr + ")").append("\n");
        Map<Number160, Node> list = null;
        for (int i = 0; i < Number160.BITS; i++) {
            list = kBuckets.get(i);
            synchronized (list) {
                if (list.size() > 0) {
                    sb.append("\nbucket:").append(i).append("->\n");
                    for (final Node node : list.values()) {
                        sb.append("node:").append(node.getId()).append(",");

                    }
                }
            }
        }
        return sb.toString();
    }


    public static Comparator<Number160> getXORNumberComparator(final Number160 location) {
        return new Comparator<Number160>() {
            public int compare(final Number160 id1, final Number160 id2) {
                return compareDistance(location, id1, id2);
            }
        };
    }

    public static Comparator<Node> getXORNodeComparator(final Number160 location) {
        return new Comparator<Node>() {
            @Override
            public int compare(Node node1, Node node2) {
                if (node1.getId() != null && node2.getId() != null) {
                    return compareDistance(location, node1, node2);
                }
                return 0;
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
                return 1; // if the timestamps are the same, make sure both are added to the SET, since it is a set, this comparator would result in the node nto being added, but that is not the idea... careful with sets...
            }
        };
    }

    public static Comparator<Node> getNewestNodeComparator() {
        return new Comparator<Node>() {
            @Override
            public int compare(Node n1, Node n2) {
                if (n1 != null && n2 != null && n1.getPeerStatistics() != null && n2.getPeerStatistics() != null) {
                    if (n1.getPeerStatistics().getTimestamp() < n2.getPeerStatistics().getTimestamp()) return 1;
                    if (n1.getPeerStatistics().getTimestamp() > n2.getPeerStatistics().getTimestamp()) return -1;
                }
                return 1; // if the timestamps are the same, make sure both are added to the SET, since it is a set, this comparator would result in the node nto being added, but that is not the idea... careful with sets...
            }
        };
    }

    public static int compareDistance(final Number160 loc, final Node node1, final Node node2) {
        return distance(loc, node1.getId()).compareTo(distance(loc, node2.getId()));
    }

    public static int compareDistance(final Number160 loc, final Number160 id1, final Number160 id2) {
        return distance(loc, id1).compareTo(distance(loc, id2));
    }

    public static Number160 distance(final Number160 id1, final Number160 id2) {
        return id1.xor(null, id2);
    }

    public static int compareKBucket(final Number160 loc, final Number160 id1, final Number160 id2) {
        Integer d1 = getKBucket(loc, id1);
        Integer d2 = getKBucket(loc, id2);
        return d1.compareTo(d2);
    }


    public Object getValue(Number160 key) {
        return storage.getOrDefault(key, null);
    }

    public void setValue(Number160 key, Object obj) {
	    storage.put(key, obj);
    }

    public int valuesInBuckets() {
	    int count = 0;
        for (Map<Number160, Node> kBucket : kBuckets) {
            count += kBucket.size();
        }
        return count;
    }

    public Map<String, List<List<String>>> getFormattedKBuckets() {
	    Map<String, List<List<String>>> map = new HashMap<>();
	    List<List<String>> list = new ArrayList<>();
	    map.put(nodeId.toString(), list);
	    List<String> nodes = new ArrayList<>();
	    List<String> idStrs = new ArrayList<>();
	    list.add(nodes);
	    list.add(idStrs);

        for (Map<Number160, Node> kBucket : kBuckets) {
            for (Map.Entry<Number160, Node> entry : kBucket.entrySet()) {
                nodes.add(entry.getKey().toString());
                idStrs.add(entry.getValue().getIdStr());
            }
        }

        return map;
    }
}

package dk.orda.overlaynetwork.overlaygeo;

import dk.orda.overlaynetwork.overlay.dht.Node;
import dk.orda.overlaynetwork.overlay.dht.Number160;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@SuppressWarnings("Duplicates")
public class SmallWorldBucket {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final Number160 nodeId;
    private double lat;
    private double lon;
    private LinkedHashSet<Node> closeNodes;
    private LinkedHashSet<Node> randomNodes;
    private LinkedHashSet<Node> hubNodes;
    private LinkedHashSet<Node> passiveNodes;
    private Random random;

    private int sizeClose = 1000;
    private int sizeRandom = 1000;
    private int sizeHub = 1000;
    private int sizePassive = 5000;

    // RATE is setup for 10 nodes a second using milliseconds
    private double r_const = 0.2;
    private double targetRate = 0.001; // f.eks MaxListeLængde / dag
    private double rate       = 0.001;
    private long lastAddition = System.currentTimeMillis();

    private double averageDistance = 99999999.0;
    private double phi = 2.0; // use 1 or 2

    private LinkedHashSet<Node> savedCloseNodes;
    private LinkedHashSet<Node> savedRandomNodes;
    private LinkedHashSet<Node> savedHubNodes;

    // NOTE: this class is used from multiple threads, but is not thread-safe
    // generally handled by try/catch->ignore

    public SmallWorldBucket(final Number160 nodeId, final double lat, final double lon) {
        this.nodeId = nodeId;
        this.lat = lat;
        this.lon = lon;
        closeNodes = new LinkedHashSet<>();
        randomNodes = new LinkedHashSet<>();
        hubNodes = new LinkedHashSet<>();
        passiveNodes = new LinkedHashSet<>();
        random = new Random();

        savedCloseNodes = new LinkedHashSet<>();
        savedRandomNodes = new LinkedHashSet<>();
        savedHubNodes = new LinkedHashSet<>();
    }

    public SmallWorldBucket(final Number160 nodeId, final double lat, final double lon, int sizeClose, int sizeRandom, int sizeHub, int sizePassive) {
        this(nodeId, lat, lon);
        this.sizeClose = sizeClose;
        this.sizeRandom = sizeRandom;
        this.sizeHub = sizeHub;
        this.sizePassive = sizePassive;
    }

//    public SortedSet<Node> getGeobucket() {
//        return getGeobucket(100, 20, 20);
//    }

//    public SortedSet<Node> getGeobucket(int nmbrClose, int nmbrRandom, int nmbrHub) {
    public SortedSet<Node> getGeobucket() {
        SortedSet<Node> set = new TreeSet<>(GeoDistributedHashTableImpl.getGeoNodeComparator(lat, lon));
        for (Node closeNode : closeNodes) {
            if (!passiveNodes.contains(closeNode)) {
                set.add(closeNode);
            }
        }
        for (Node randomNode : randomNodes) {
            if (!passiveNodes.contains(randomNode)) {
                set.add(randomNode);
            }
        }
        for (Node hubNode : hubNodes) {
            if (!passiveNodes.contains(hubNode)) {
                set.add(hubNode);
            }
        }
        return set;
    }

    public boolean addCloseNode(Node node) {
        if (closeNodes.size() < sizeClose) {
            closeNodes.add(node);
            averageDistance = calculateAverageDistance();
            return true;
        }

        boolean success = true;

        // Pin(d) = e^-(d/davg)^phi
        LatLon latLon = node.tryGetLatLon();
        if (latLon != null) {
            double d = DistanceCalculator.pythagoreanDistance(lat, lon, latLon.getLat(), latLon.getLon());
            double p = Math.exp(-Math.pow(d / averageDistance, phi));

            if (p < 0.0 || Double.isNaN(p)) {
                p = 0.0;
            }

            double dice = random.nextDouble();
            success = dice <= p;
        }

        if (success) {
            closeNodes.add(node);
            if (closeNodes.size() > sizeClose) {
                removeNodeFromList(closeNodes);
            }
            averageDistance = calculateAverageDistance();
//            log.info("Node " + node.getIdStr() + " added");
        } else {
//            log.info("Node " + node.getIdStr() + " WAS NOT added");
        }

        return success;
    }

    public boolean addRandomNode(Node node) {
        if (randomNodes.size() < sizeRandom) {
            randomNodes.add(node);
            lastAddition = System.currentTimeMillis();
            return true;
        }
        boolean success = true;

        double last = System.currentTimeMillis() - lastAddition;
        if (last <= 0.0) last = 1000.0; // 1 sec, if invalid time
        double part1 = r_const * (1.0 / last);
        double part2 = (1.0 - r_const) * rate;
        rate = part1 + part2;

        double p = Math.pow(rate/targetRate, -1);
        if (p < 0.0 || Double.isNaN(p)) { // clean up p
            p = 0.0;
        } else if (p > 1.0) {
            p = 1.0;
        }

        double dice = random.nextDouble();
        success = dice <= p;

        if (success) {
            randomNodes.add(node);
            lastAddition = System.currentTimeMillis();
            if (randomNodes.size() > sizeRandom) {
                removeNodeFromList(randomNodes);
            }
//            log.info("RANDOM Node " + node.getIdStr() + " added");
        } else {
//            log.info("RANDOM Node " + node.getIdStr() + " WAS NOT added");
        }

        return success;
    }

    public boolean addHubNode(Node node) {
        hubNodes.add(node);
        if (hubNodes.size() > sizeHub) {
            removeNodeFromList(hubNodes);
        }
        return true;
    }

    private double calculateAverageDistance() {
        double sum = 0.0;
        double count = 0.0;

        for (Node node : closeNodes) {
            LatLon latLon = node.tryGetLatLon();
            if (latLon == null) {
                log.error("UNABLE TO CALC DISTANCE, no distance in node!!!");
                continue;
            }
            double distance = DistanceCalculator.pythagoreanDistance(lat, lon, latLon.getLat(), latLon.getLon());
            sum += distance;
            count++;
        }
        double davg = sum / count;
        return davg;
    }

    public void markPassive(Node node) {
        passiveNodes.add(node);

        // if a  node has been on the passive list for a long time (i.e.) the passive list is full.. remove the oldest one from the passive list and from all other lists
        if (passiveNodes.size() > sizePassive) {
            Node next = passiveNodes.iterator().next();
            passiveNodes.remove(next);
            closeNodes.remove(next);
            randomNodes.remove(next);
            hubNodes.remove(next);
        }
    }

    public void markActive(Node node) {
        passiveNodes.remove(node);
    }

    private void removeNodeFromList(LinkedHashSet<Node> list) {
        Node node = null;
        for (Node listNode : list) {
            if (passiveNodes.contains(listNode)) {
                node = listNode;
                break;
            }
        }
        if (node != null) {
            list.remove(node);
        } else {
            if (!list.isEmpty()) {
                try {
                    list.remove(list.iterator().next());
                } catch (Exception e) {
                    // This is multithreaded, and nothing in this class is thread-safe, so the list may already be empty: just catch and "ignore".
                    log.warn("Failed to remove a node from routing table", e);
                }
            }
        }
    }

    public boolean updateNode(Node node, boolean remove) {
        return addCloseNode(node);
    }

    public void setSizeClose(int sizeClose) {
        this.sizeClose = sizeClose;
    }

    public void setSizeRandom(int sizeRandom) {
        this.sizeRandom = sizeRandom;
    }

    public void setSizeHub(int sizeHub) {
        this.sizeHub = sizeHub;
    }

    public void setSizePassive(int sizePassive) {
        this.sizePassive = sizePassive;
    }

    public void saveState() {
        savedCloseNodes.clear();
        savedCloseNodes.addAll(closeNodes);
        savedRandomNodes.clear();
        savedRandomNodes.addAll(randomNodes);
        savedHubNodes.clear();
        savedHubNodes.addAll(hubNodes);
    }

    public void reloadState() {
        closeNodes.clear();
        randomNodes.clear();
        hubNodes.clear();
        closeNodes.addAll(savedCloseNodes);
        randomNodes.addAll(savedRandomNodes);
        hubNodes.addAll(savedHubNodes);
    }

//    public boolean updateNodeOLD(Node node, boolean remove) {
//        // don't add self to buckets
//        if (nodeId.equals(node.getId())) return false;
//
//
//
//        boolean exists = false;
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
//                log.error("GEO/SMALL WORLD DHT DID NOT LEARN ABOUT " + node.getIdStr() + " IN UPDATE NODE()");
//                geobucket.add(node);
//            }
//        }
//
//        if (false) {
//            removeNodes(geobucket);
//        }
//
//        return true;
//    }

//    public void removeNodes(SortedSet<Node> bucket) {
//        if (bucket.size() < 20) return;
//        Node first = bucket.first();
//        Node last = bucket.last();
//        LatLon firstLatLon = first.tryGetLatLon();
//        LatLon lastLatLon = last.tryGetLatLon();
//        if (firstLatLon == null || lastLatLon == null) {
//            log.error("Unable to do small world node removal as nodes don't have coordinates");
//            return;
//        }
//        double nearest = DistanceCalculator.pythagoreanDistance(lat, lon, firstLatLon.getLat(), firstLatLon.getLon());
//        double farthest = DistanceCalculator.pythagoreanDistance(lat, lon, lastLatLon.getLat(), lastLatLon.getLon());
//        double top = farthest - nearest;
//        List<Node> remove = new ArrayList<>();
//        for (Node node : bucket) {
//            LatLon latLon = node.tryGetLatLon();
//            if (latLon == null) {
//                log.error("Unable to get latlon from node, cannot do linaer random removal from geobucket");
//                continue;
//            }
//            double v = DistanceCalculator.pythagoreanDistance(lat, lon, latLon.getLat(), latLon.getLon());
//            double p = (v / top) * 100;
//            double index = Math.random() * top;
//            if (index < p) {
//                remove.add(node);
////                log.info("REMOVE HAHA node " + node.getIdStr());
//            }
//        }
//        for (Node node : remove) {
//            bucket.remove(node);
//        }
//    }
}

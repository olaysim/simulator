package dk.orda.overlaynetwork.overlay.dht;

import dk.orda.overlaynetwork.overlay.peer.Peer;
import dk.orda.overlaynetwork.overlay.peer.PeerStatistics;
import dk.orda.overlaynetwork.overlaygeo.DistanceCalculator;
import dk.orda.overlaynetwork.overlaygeo.LatLon;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Node implements Comparable<Node> {

    private final Number160 id;
    private String idStr;
    private Peer peer;
    private PeerStatistics peerStats;
    private Map<String, String> dict;
    private Map<String, byte[]> dictBytes;
    private DistributedHashTableType networkType;

    public Node clone(){
        Node n = new Node(this.id, this.idStr);
        n.setNetworkType(this.networkType);
        n.setPeer(this.peer); // todo: clone
        n.setPeerStatistics(this.peerStats); // todo: clone
        n.getDict().putAll(this.dict);
        n.getDictBytes().putAll(this.dictBytes);
        return n;
    }

    public Node(final Number160 id, final String idStr) {
        this(id);
        this.idStr = idStr;
        this.networkType = DistributedHashTableType.DEFAULT;
    }
    public Node(final Number160 id) {
        this.id = id;
        this.peerStats = new PeerStatistics();
        dict = new HashMap<>();
        dictBytes = new HashMap<>();
        this.networkType = DistributedHashTableType.DEFAULT;
    }

    public Node(Node node) {
        this.id = node.getId();
        this.idStr = node.getIdStr();
        this.peer = new Peer(node.getPeer());
        this.peerStats = new PeerStatistics();
        if (node.dict != null) this.dict = new HashMap<>(node.dict);
        if (node.dictBytes != null) this.dictBytes = new HashMap<>(node.dictBytes);
        this.networkType = node.getNetworkType();
    }

    public Number160 getId() {
        return id;
    }

    public Peer getPeer() {
        return peer;
    }

    public void setPeer(Peer peer) {
        this.peer = peer;
    }

    public PeerStatistics getPeerStatistics() {
        return peerStats;
    }

    public void setPeerStatistics(PeerStatistics peerStats) {
        this.peerStats = peerStats;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node)) return false;
        Node node = (Node) o;
        return Objects.equals(id, node.id) &&
            Objects.equals(idStr, node.idStr) &&
            Objects.equals(tryGetLatLon(), node.tryGetLatLon());
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, idStr, tryGetLatLon());
    }

    @Override
    public String toString() {
        if (idStr != null && peer != null)
            return id.toString() + " / " + idStr + " (" + peer.toString() + ")";
        if (idStr != null)
            return id.toString() + " / " + idStr;
        return id.toString();
    }

    @Override
    public int compareTo(Node other) {
//        if (networkType == DistributedHashTableType.GEO) {
//            LatLon latLon = tryGetLatLon();
//            LatLon otherLatLon = other.tryGetLatLon();
//            double distance = DistanceCalculator.pythagoreanDistance(latLon.getLat(), latLon.getLon(), otherLatLon.getLat(), otherLatLon.getLon());
//
//        } else {
            return id.compareTo(other.getId());
//        }
    }

    public String putDict(String key, String value) {
        return this.dict.put(key, value);
    }

    public byte[] putDict(String key, byte[] value) {
        return this.dictBytes.put(key, value);
    }

    public String getDict(String key) {
        return dict.get(key);
    }

    public byte[] getDictBytes(String key) {
        return dictBytes.get(key);
    }

    public Map<String, String> getDict() {
        return dict;
    }

    public Map<String, byte[]> getDictBytes() {
        return dictBytes;
    }

    public String getIdStr() {
        return idStr;
    }

    public LatLon tryGetLatLon() {
        if (dict == null || !dict.containsKey("x") || !dict.containsKey("y")) return null;
        String lat = dict.get("x");
        String lon = dict.get("y");
        if (lat == null || lat.isEmpty() || lon == null || lon.isEmpty()) return null;
        try {
            return new LatLon(Double.parseDouble(lat), Double.parseDouble(lon));
        } catch (Exception ignore) {
            return null;
        }
    }

    public DistributedHashTableType getNetworkType() {
        return networkType;
    }

    public void setNetworkType(DistributedHashTableType networkType) {
        this.networkType = networkType;
    }


}

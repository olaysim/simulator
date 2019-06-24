package dk.orda.overlaynetwork.overlay;

import dk.orda.overlaynetwork.overlay.dht.DistributedHashTable;
import dk.orda.overlaynetwork.overlay.dht.Node;
import dk.orda.overlaynetwork.overlay.dht.Number160;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

public interface RequestOverlay extends OverlayNetwork{

    Node getSelf();
    boolean updateNode(Node node);
    boolean updateRandom(Node node);
    boolean updateHub(Node node);

    SortedSet<Node> getNodes(Number160 id, Map<String, String> ext);
    SortedSet<Node> getNodes(Node targetNode);

    byte[] getValue(Number160 key);
    void setValue(Number160 key, byte[] value);
    boolean hasValue(Number160 key);

    void addValueToList(Number160 key, byte[] value);
    boolean removeValueFromList(Number160 key, byte[] value);
    boolean listsHasKey(Number160 key);
    boolean listHasValue(Number160 key, byte[] value);
    List<byte[]> getList(Number160 key);

    void putValueInMap(Number160 key, String index, byte[] value);
    boolean removeIndexFromMap(Number160 key, String index);
    boolean mapsHasKey(Number160 key);
    boolean mapHasIndex(Number160 key, String index);
    Map<String, byte[]> getMap(Number160 key);
    byte[] getMapValue(Number160 key, String index);

    DistributedHashTable getDHT();
}

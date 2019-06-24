package dk.orda.overlaynetwork.storage;

import dk.orda.overlaynetwork.overlay.dht.Number160;

import java.util.List;
import java.util.Map;

public interface StorageLayer {
    void setValue(Number160 key, byte[] value);
    boolean hasKey(Number160 key);
    byte[] getValue(Number160 key);

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
}

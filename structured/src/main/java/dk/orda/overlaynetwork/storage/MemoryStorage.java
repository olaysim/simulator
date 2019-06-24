package dk.orda.overlaynetwork.storage;

import dk.orda.overlaynetwork.overlay.dht.Number160;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class MemoryStorage implements StorageLayer {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final Map<Number160, byte[]> database;
    private final Map<Number160, Map<String, byte[]>> mapDatabase;
    private final Map<Number160, List<byte[]>> listDatabase;

    public MemoryStorage() {
        database = new Hashtable<>();
        mapDatabase = new Hashtable<>();
        listDatabase = new Hashtable<>();
    }

    // STANDARD KEY / VALUE STORE
    @Override
    public void setValue(Number160 key, byte[] value) {
        database.put(key, value);
    }

    @Override
    public boolean hasKey(Number160 key) {
        return database.containsKey(key);
    }

    @Override
    public byte[] getValue(Number160 key) {
        return database.get(key);
    }


    // KEY / LIST<string> STORE
    @Override
    public void addValueToList(Number160 key, byte[] value) {
        synchronized (listDatabase) {
            if (!listDatabase.containsKey(key) || listDatabase.get(key) == null) {
                listDatabase.put(key, new ArrayList<>());
            }
            if (!listDatabase.get(key).contains(value)) {
                listDatabase.get(key).add(value);
            }
        }
    }

    @Override
    public boolean removeValueFromList(Number160 key, byte[] value) {
        synchronized (listDatabase) {
            if (!listDatabase.containsKey(key) || listDatabase.get(key) == null) {
                return false;
            }
            return listDatabase.get(key).remove(value);
        }
    }

    @Override
    public boolean listsHasKey(Number160 key) {
        return listDatabase.containsKey(key);
    }

    @Override
    public boolean listHasValue(Number160 key, byte[] value) {
        synchronized (listDatabase) {
            if (!listDatabase.containsKey(key) || listDatabase.get(key) == null) {
                return false;
            }
            return listDatabase.get(key).contains(value);
        }
    }

    @Override
    public List<byte[]> getList(Number160 key) {
        synchronized (listDatabase) {
            if (!listDatabase.containsKey(key) || listDatabase.get(key) == null) {
                return new ArrayList<>();
            }
            return listDatabase.get(key);
        }
    }

    // KEY / MAP(INDEX/VALUE) STORE
    @Override
    public void putValueInMap(Number160 key, String index, byte[] value) {
        synchronized (mapDatabase) {
            if (!mapDatabase.containsKey(key) || mapDatabase.get(key) == null) {
                mapDatabase.put(key, new Hashtable<>());
            }
            mapDatabase.get(key).put(index, value);
        }
    }

    @Override
    public boolean removeIndexFromMap(Number160 key, String index) {
        synchronized (mapDatabase) {
            if (!mapDatabase.containsKey(key) || mapDatabase.get(key) == null) {
                return false;
            }
            return mapDatabase.get(key).remove(index) != null;
        }
    }

    @Override
    public boolean mapsHasKey(Number160 key) {
        return mapDatabase.containsKey(key);
    }

    @Override
    public boolean mapHasIndex(Number160 key, String index) {
        if (!mapDatabase.containsKey(key) || mapDatabase.get(key) == null) {
            return false;
        }
        return mapDatabase.get(key).containsKey(index);
    }

    @Override
    public Map<String, byte[]> getMap(Number160 key) {
        synchronized (mapDatabase) {
            if (!mapDatabase.containsKey(key) || mapDatabase.get(key) == null) {
                return new Hashtable<>();
            }
            return mapDatabase.get(key);
        }
    }

    @Override
    public byte[] getMapValue(Number160 key, String index) {
        synchronized (mapDatabase) {
            if (!mapDatabase.containsKey(key) || mapDatabase.get(key) == null) {
                return new byte[0];
            }
            return mapDatabase.get(key).get(index);
        }
    }
}

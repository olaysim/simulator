package dk.orda.overlaynetwork.overlay.dht;

import dk.orda.overlaynetwork.overlaygeo.GeoDistributedHashTableImpl;
import dk.orda.overlaynetwork.statistics.StatConfiguration;
import dk.orda.overlaynetwork.statistics.StatLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("Duplicates")
public class DistributedHashTableFactory {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public static DistributedHashTable createDistributedHashTable(Number160 id, String idStr, StatLogger statLogger) {
        DistributedHashTableConfiguration config = new DistributedHashTableConfiguration();
        return new DistributedHashTableImpl(id, idStr, config, statLogger);
    }

    public static DistributedHashTable createDistributedHashTable(Number160 id, String idStr, DistributedHashTableConfiguration config, StatLogger statLogger) {
        return new DistributedHashTableImpl(id, idStr, config, statLogger);
    }



    public static DistributedHashTable createGeoDistributedHashTable(Number160 id, double lat, double lon, String idStr, StatLogger statLogger) {
        DistributedHashTableConfiguration config = new DistributedHashTableConfiguration();
        return new GeoDistributedHashTableImpl(id, lat, lon, idStr, config, statLogger);
    }

    public static DistributedHashTable createGeoDistributedHashTable(Number160 id, double lat, double lon, String idStr, DistributedHashTableConfiguration config, StatLogger statLogger) {
        return new GeoDistributedHashTableImpl(id, lat, lon, idStr, config, statLogger);
    }
}

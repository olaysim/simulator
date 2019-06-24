package dk.orda.overlaynetwork.overlay.peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeerStatistics {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private long timestamp;
    private long pingTime;


    public PeerStatistics() {
        this.timestamp = System.currentTimeMillis();
    }


    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void resetTimestamp() {
        this.timestamp = System.currentTimeMillis();
    }

    public long getPingTime() {
        return pingTime;
    }

    public void setPingTime(long pingTime) {
        this.pingTime = pingTime;
    }
}

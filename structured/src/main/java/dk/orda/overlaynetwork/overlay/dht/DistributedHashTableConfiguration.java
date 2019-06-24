package dk.orda.overlaynetwork.overlay.dht;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DistributedHashTableConfiguration {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // Variables defined in Kademlia specification http://xlattice.sourceforge.net/components/protocol/kademlia/specs.html
    private int alpha;
    private final int B;
    private int k;
    private int kr; // k but used in number of nodes returned, because k is used in iteration count, which needs to be very large in the feeder tests
    private int bucketSize; // k but used in bucket size tests, because k is used in iteration count, which needs to be very large in the feeder tests
    private final int tExpire;
    private final int tRefresh;
    private final int tReplicate;
    private final int tRepublish;
    private DistributedHashTableType type;

    public DistributedHashTableConfiguration() {
        this.alpha = 3;
        this.B = Number160.BITS;
        this.k = 200; // IMPACTS timing measurements when chains is larger than 20 (when value is 20)
        this.kr = 20;
        this.bucketSize = 20; // default k
        this.tExpire = 86400;
        this.tRefresh = 3600;
        this.tReplicate = 3600;
        this.tRepublish = 86410;
        this.type = DistributedHashTableType.DEFAULT;
    }
    public DistributedHashTableConfiguration(final int alpha, final int bits, final int contacts, final int tExpire, final int tRefresh, final int tReplicate, final int tRepublish) {
        this.alpha = alpha;
        this.B = bits;
        this.k = contacts;
        this.kr = 20;
        this.bucketSize = 20; // default, set with setter when testing
        this.tExpire = tExpire;
        this.tRefresh = tRefresh;
        this.tReplicate = tReplicate;
        this.tRepublish = tRepublish;
        this.type = DistributedHashTableType.DEFAULT;
    }

    public int getAlpha() {
        return alpha;
    }

    public int getBits() {
        return B;
    }

    public int getK() {
        return k;
    }

    public int getKR() {
        return kr;
    }

    public int getTExpire() {
        return tExpire;
    }

    public int getTRefresh() {
        return tRefresh;
    }

    public int getTReplicate() {
        return tReplicate;
    }

    public int getTRepublish() {
        return tRepublish;
    }

    public void setK(int value) {
        this.k = value;
    }

    public void setA(int value) {
        this.alpha = value;
    }

    public DistributedHashTableType getType() {
        return type;
    }

    public void setType(DistributedHashTableType type) {
        this.type = type;
    }

    public int getBucketSize() {
        return bucketSize;
    }

    public void setBucketSize(int bucketSize) {
        this.bucketSize = bucketSize;
    }
}

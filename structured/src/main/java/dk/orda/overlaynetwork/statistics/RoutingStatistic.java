package dk.orda.overlaynetwork.statistics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class RoutingStatistic {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private long totalMethodTime; // NANO
    private long nodesConnected;
    private long nodesAnswered;
    private long nodesSkipped;
    private int iterationCount;


    public long getTotalMethodTime() {
        return totalMethodTime;
    }

    public void setTotalMethodTime(long totalMethodTime) {
        this.totalMethodTime = totalMethodTime;
    }

    public long getNodesConnected() {
        return nodesConnected;
    }

    public void setNodesConnected(long nodesConnected) {
        this.nodesConnected = nodesConnected;
    }

    public long getNodesAnswered() {
        return nodesAnswered;
    }

    public void setNodesAnswered(long nodesAnswered) {
        this.nodesAnswered = nodesAnswered;
    }

    public long getNodesSkipped() {
        return nodesSkipped;
    }

    public void setNodesSkipped(long nodesSkipped) {
        this.nodesSkipped = nodesSkipped;
    }

    public int getIterationCount() {
        return iterationCount;
    }

    public void setIterationCount(int iterationCount) {
        this.iterationCount = iterationCount;
    }

    public double getSuccessRate() {
        if (nodesConnected == 0) return 0.0;
        long x = nodesAnswered + nodesSkipped;
        return (double)x / (double)nodesConnected;
    }

    public void increaseAnsweredCount() {
        nodesAnswered++;
    }

    public void increaseConnectedCount() {
        nodesConnected++;
    }

    public void increaseSkippedCount(long count) {
        nodesSkipped += count;
    }

    public void increaseIterationCount() {
        iterationCount++;
    }

    @Override
    public String toString() {
        return "Total call time: " + totalMethodTime + " (" + (double)totalMethodTime/1000000000.0 + "s), success rate: " + getSuccessRate() + " (co: " + nodesConnected + ", an: " + nodesAnswered + ", sk: " + nodesSkipped +")";
    }
}

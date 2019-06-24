package dk.orda.overlaynetwork.statistics;

public class StatConfiguration {
    private String testId;
    private String correlationId;
    private long timestamp;

    public StatConfiguration(String testId, String correlationId) {
        this.testId = testId;
        this.correlationId = correlationId;
        this.timestamp = System.currentTimeMillis();
    }

    public StatConfiguration() {}

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}

package dk.orda.overlaynetwork.statistics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Value<T> {
    private String name;
    private long timestamp;
    private String correlation;
    private String testid;
    private String group;
    private List<T> values;
    private String description;

    public Value(String name, long timestamp, StatConfiguration statConf, String group, List<T> values, String description) {
        this.name = name;
        this.timestamp = timestamp;
        this.testid = statConf.getTestId() != null ? statConf.getTestId() : "";
        this.correlation = statConf.getCorrelationId() != null ? statConf.getCorrelationId() : "";
        this.group = group;
        this.values = values;
        this.description = description;
    }

    public Value(String name, long timestamp, StatConfiguration statConf, String group, List<T> values) {
        this.name = name;
        this.timestamp = timestamp;
        this.testid = statConf.getTestId() != null ? statConf.getTestId() : "";
        this.correlation = statConf.getCorrelationId() != null ? statConf.getCorrelationId() : "";
        this.group = group;
        this.values = values;
        this.description = "";
    }

    public Value(String name, long timestamp, StatConfiguration statConf, List<T> values) {
        this.name = name;
        this.timestamp = timestamp;
        this.testid = statConf.getTestId() != null ? statConf.getTestId() : "";
        this.correlation = statConf.getCorrelationId() != null ? statConf.getCorrelationId() : "";
        this.group = "other";
        this.values = values;
        this.description = "";
    }



    public Value(String name, long timestamp, StatConfiguration statConf, String group, T values, String description) {
        this.name = name;
        this.timestamp = timestamp;
        this.testid = statConf.getTestId() != null ? statConf.getTestId() : "";
        this.correlation = statConf.getCorrelationId() != null ? statConf.getCorrelationId() : "";
        this.group = group;
        this.values = Arrays.asList(values,  (T)Collections.emptyList());
        this.description = description;
    }

    public Value(String name, long timestamp, StatConfiguration statConf, String group, T values) {
        this.name = name;
        this.timestamp = timestamp;
        this.testid = statConf.getTestId() != null ? statConf.getTestId() : "";
        this.correlation = statConf.getCorrelationId() != null ? statConf.getCorrelationId() : "";
        this.group = group;
        this.values = Arrays.asList(values, (T)Collections.emptyList());
        this.description = "";
    }

    public Value(String name, long timestamp, StatConfiguration statConf, T values) {
        this.name = name;
        this.timestamp = timestamp;
        this.testid = statConf.getTestId() != null ? statConf.getTestId() : "";
        this.correlation = statConf.getCorrelationId() != null ? statConf.getCorrelationId() : "";
        this.group = "other";
        this.values = Arrays.asList(values, (T)Collections.emptyList());
        this.description = "";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getCorrelation() {
        return correlation;
    }

    public void setCorrelation(String correlation) {
        this.correlation = correlation;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<T> getValue() {
        return values;
    }

    public void setValue(List<T> values) {
        this.values = values;
    }

    public List<String> getValuesAsStringList() {
        List<String> list = new ArrayList<>();
        for (T value : values) {
            list.add(String.valueOf(value));
        }
        return list;
    }

    public String getTestid() {
        return testid;
    }

    public void setTestid(String testid) {
        this.testid = testid;
    }
}

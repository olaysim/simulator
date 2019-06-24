package dk.orda.overlaynetwork.statistics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DhtStates {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private List<Map<String, List<List<String>>>> dhtStates;
    private StatLogger statLogger;
    private final Object lock = new Object();

    public DhtStates(StatLogger statLogger) {
        dhtStates = new ArrayList<>();
        this.statLogger = statLogger;
    }

    public void setDhtStates(List<Map<String, List<List<String>>>> dhtStates) {
        this.dhtStates = dhtStates;
    }

    public void add(Map<String, List<List<String>>> state) {
        synchronized (lock) {
            dhtStates.add(state);
        }
    }

    public void addToStatLogger(StatConfiguration statConf) {
        addToStatLogger(statConf, "dhtstates");
    }
    public void addToStatLogger(StatConfiguration statConf, String group) {
        synchronized (lock) {
            for (Map<String, List<List<String>>> state : dhtStates) {
                List<Value> vlist = new ArrayList<>();
                for (Map.Entry<String, List<List<String>>> entry : state.entrySet()) {
                    if (entry.getValue().size() < 1 || entry.getValue().size() > 2) {
                        // this code only supports a value list size of two... otherwise the log output will be impossible to parse...
                        log.error("Value list must have a size of 2");
                    }
                    List<List<String>> newList = entry.getValue();
                    if (entry.getValue().size() == 1) {
                        List<List<String>> tmp = new ArrayList<>(2);
                        tmp.add(entry.getValue().get(0));
                        tmp.add(Collections.emptyList());
                        newList = tmp;
                    }
                    vlist.add(new Value(entry.getKey(), statConf.getTimestamp(), statConf, Arrays.asList(String.join(",", newList.get(0)), String.join(",", newList.get(1)))));
                }
                statLogger.addValues(group, vlist);
            }
            dhtStates.clear();
        }
    }

}

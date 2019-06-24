package dk.orda.overlaynetwork.overlay.dht;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedSet;
import java.util.TreeSet;

public class ValueOrNodes {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private SortedSet<Node> list;
    private byte[] value;

    public ValueOrNodes() {
        list = new TreeSet<>();
        value = null;
    }

    public ValueOrNodes(SortedSet<Node> nodes) {
        this.list = nodes;
        value = null;
    }

    public ValueOrNodes(byte[] value) {
        this.list = null;
        this.value = value;
    }

    public SortedSet<Node> getList() {
        return list;
    }

    public void setList(SortedSet<Node> list) {
        this.list = list;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    public boolean isValue() {
        return value != null;
    }
}

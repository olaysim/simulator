package dk.orda.overlaynetwork.net.broadcast;

import dk.orda.overlaynetwork.overlay.dht.Number160;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeEvent {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private Number160 nodeId;
    private String ipAddress;
    private int port;

    public static final byte SEPERATOR = (byte) '|';

    public NodeEvent() {}

    public NodeEvent(String ipAddress, int port, Number160 nodeId) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.nodeId = nodeId;
    }

    public NodeEvent(String ipAddress, int port, byte[] nodeId) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.nodeId = new Number160(null, nodeId);
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Number160 getNodeId() {
        return nodeId;
    }

    public byte[] getNodeIdBytes() {
        return nodeId.toByteArray();
    }

    public void setNodeId(Number160 nodeId) {
        this.nodeId = nodeId;
    }

    public byte[] getPortBytes() {
        byte[] arr = new byte[4];
        arr[0] = (byte) ((port & 0xFF000000) >> 24);
        arr[1] = (byte) ((port & 0x00FF0000) >> 16);
        arr[2] = (byte) ((port & 0x0000FF00) >> 8);
        arr[3] = (byte) ((port & 0x000000FF));
        return arr;
    }
}

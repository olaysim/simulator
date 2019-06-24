package dk.orda.overlaynetwork.net.rpc;

import com.google.protobuf.MessageLite;
import dk.orda.overlaynetwork.net.Address;
import dk.orda.overlaynetwork.net.NetworkServer;
import dk.orda.overlaynetwork.net.rpc.client.MapOrNodesFuture;
import dk.orda.overlaynetwork.net.rpc.client.PingFuture;
import dk.orda.overlaynetwork.net.rpc.client.ValueOrNodesFuture;
import dk.orda.overlaynetwork.net.rpc.protobuf.RpcMessages;
import dk.orda.overlaynetwork.net.tcp.TcpClient;
import dk.orda.overlaynetwork.overlay.OverlayNetwork;
import dk.orda.overlaynetwork.overlay.RequestOverlay;
import dk.orda.overlaynetwork.overlay.dht.Node;
import dk.orda.overlaynetwork.overlay.dht.Number160;
import dk.orda.overlaynetwork.overlay.dht.ValueOrNodes;
import dk.orda.overlaynetwork.overlay.peer.Peer;
import dk.orda.overlaynetwork.statistics.StatConfiguration;
import dk.orda.overlaynetwork.statistics.StatLogger;
import dk.orda.overlaynetwork.statistics.Value;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SuperNode implements Runnable {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private TcpClient client;
    private NetworkServer srv;
    private final Map<String, Connection> conns;
    private ScheduledExecutorService executor;
    private StatLogger statLogger;
    private Map<Number160, OverlayNetwork> overlays;
    private Address address;
    private SuperNodeServerHandler serverHandler;
    private MessageQueue virtualMessageQueue;

    public SuperNode(Address address, NetworkServer srv, StatLogger statLogger) {
        this.address = address;
        this.client = new TcpClient();
        this.srv = srv;
        this.conns = new Hashtable<>();
        this.overlays = new HashMap<>();
        virtualMessageQueue = new MessageQueue();
        this.statLogger = statLogger;
//        executor = Executors.newSingleThreadScheduledExecutor();
//        executor.scheduleWithFixedDelay(this, 30,30, TimeUnit.SECONDS);

        this.serverHandler = new SuperNodeServerHandler(this, overlays, client);
    }

    public void start() {
        srv.start(this);
    }

    public void stop() {
        srv.stop();
        for (Map.Entry<String, Connection> entry : conns.entrySet()) {
            try {
                client.close(entry.getValue().channel);
            } catch (Exception ignore) {}
        }
        client.shutdown();
    }

    public void reset() {
        srv.reset(getOverlayList());
    }

    private Connection getConnection(Node node) {
        long starttime = System.nanoTime();
        String key = node.getPeer().getAddress().getAddress() + ":" + node.getPeer().getAddress().getPort().getPort();
        synchronized (conns) { // NOTE: slow?
            if (conns.containsKey(key)) {
                Connection con = conns.get(key);
                Channel ch = con.channel;
                if (ch.isActive()) {
                    long endtime = System.nanoTime();
                    statLogger.addValue(new Value("getconnection", statLogger.getConfig().getTimestamp(), statLogger.getConfig(), "connection", (endtime-starttime)));
                    return con;
                } else {
                    client.close(ch);
                    conns.remove(key);
                }
            }

            try {
                Channel ch = client.connect(node.getPeer().getAddress().getInetSocketAddress());
                Connection con = new Connection(ch);
                conns.put(key, con);
                long endtime = System.nanoTime();
                statLogger.addValue(new Value("getconnection", statLogger.getConfig().getTimestamp(), statLogger.getConfig(), "connection", (endtime-starttime)));
                return con;
            } catch (Exception e) {
                log.error("Unable to connect to node: " + node.getId().toString() + " - " + node.getPeer());
                long endtime = System.nanoTime();
                statLogger.addValue(new Value("getconnection", statLogger.getConfig().getTimestamp(), statLogger.getConfig(), "connection", (endtime-starttime)));
                return null;
            }
        }
    }

    public SortedSet<Node> sendMessageLocal(Node node, Node target, Node self) throws Exception {
        return serverHandler.handleGenericRequestLocal(node, target, self, MessageType.FIND_NODE);
    }

    public <T extends MessageLite, V extends Future> V sendMessage(Node node, T msg, MessageType type, UUID uuid) {
        OverlayNetwork network = overlays.get(node.getId());
        if (network != null) {
            ValueOrNodesFuture f = new ValueOrNodesFuture(virtualMessageQueue, uuid);
            RpcMessages.ValueOrNodes result = null;
            try {
                result = serverHandler.handleGenericRequest(node, msg, type);
            } catch (Exception e) {
                log.error("unable to get result from virtual overlay network", e);
            }
            if (result != null) { // if null, then request will timeout when checking for answer
                virtualMessageQueue.offer(uuid, result);
            }
            return (V)f;
        } else {
            Connection con = getConnection(node);
            Channel ch = con.channel;
            if (ch == null) return null;
            node.getPeerStatistics().setPingTime(con.getPingTime());
            return client.sendMessage(ch, msg, type, uuid);
        }
    }

    public <T extends MessageLite, V extends Future> V sendMessageForMap(Node node, T msg, MessageType type, UUID uuid) {
        OverlayNetwork network = overlays.get(node.getId());
        if (network != null) {
            MapOrNodesFuture f = new MapOrNodesFuture(virtualMessageQueue, uuid);
            RpcMessages.MapOrNodes result = null;
            try {
                result = serverHandler.handleGenericRequest(node, msg, type);
            } catch (Exception e) {
                log.error("unable to get result from virtual overlay network", e);
            }
            if (result != null) { // if null, then request will timeout when checking for answer
                virtualMessageQueue.offer(uuid, result);
            }
            return (V)f;
        } else {
            Connection con = getConnection(node);
            Channel ch = con.channel;
            if (ch == null) return null;
            node.getPeerStatistics().setPingTime(con.getPingTime());
            return client.sendMessageForMap(ch, msg, type, uuid);
        }
    }

    @Override
    public void run() {
        List<String> remove = new ArrayList<>();

        // run background maintenance
        for (Map.Entry<String, Connection> entry : conns.entrySet()) {
            try {
                long timeStart = System.currentTimeMillis();
                RpcMessages.Ping ping = RpcMessages.Ping.newBuilder()
                    .setTimestamp(System.currentTimeMillis())
//                    .setId(overlay.getSelf().getId().toString())
                    .setTarget("unknown")
                    .build();
                PingFuture f = client.sendPing(entry.getValue().getChannel(), ping, MessageType.PING, UUID.randomUUID());
                if (f != null) {
                    entry.getValue().setLast(System.currentTimeMillis());
                    f.markDone();
                    long time = System.currentTimeMillis() - timeStart;
                    entry.getValue().setPingTime(time);
                    statLogger.addValue(new Value(entry.getKey(), System.currentTimeMillis(), statLogger.getConfig(), "pingtime", time));
                } else {
                    remove.add(entry.getKey());
                }
            } catch (Exception e) {
                try {
                    client.close(entry.getValue().getChannel());
                } finally {
                    remove.add(entry.getKey());
                }
            }
        }

        // remove failed connections
        synchronized (conns) {
            for (String key : remove) {
                try {
                    conns.remove(key);
                } catch (Exception e) {
                    log.error("Unable to remove connection from supernode", e);
                }
            }
        }
    }

    private static class Connection {
        private Channel channel;
        private long last;
        private long pingTime;

        public Connection(Channel channel) {
            this.channel = channel;
            this.last = System.currentTimeMillis();
        }

        public Channel getChannel() {
            return channel;
        }

        public void setChannel(Channel channel) {
            this.channel = channel;
        }

        public long getLast() {
            return last;
        }

        public void setLast(long last) {
            this.last = last;
        }

        public long getPingTime() {
            return pingTime;
        }

        public void setPingTime(long pingTime) {
            this.pingTime = pingTime;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Connection)) return false;
            Connection that = (Connection) o;
            return last == that.last &&
                Objects.equals(channel, that.channel);
        }

        @Override
        public int hashCode() {
            return Objects.hash(channel, last);
        }
    }

    public OverlayNetwork getOverlayNetwork(Number160 id) {
        return this.overlays.get(id);
    }
    public OverlayNetwork getOverlayNetwork(String id) {
        Number160 num = Number160.createHash(statLogger, id);
        return this.overlays.get(num);
    }

    public void registerOverlay(Number160 id, OverlayNetwork overlay) {
        this.overlays.put(id, overlay);
    }
    public void registerOverlay(String id, OverlayNetwork overlay) {
        Number160 num = Number160.createHash(statLogger, id);
        this.overlays.put(num, overlay);
    }

    public List<RequestOverlay> getOverlayList() {
        List<RequestOverlay> ro = new ArrayList<>();
        for (Map.Entry<Number160, OverlayNetwork> entry : overlays.entrySet()) {
            ro.add(entry.getValue().getRequestOverlay());
        }
        return ro;
    }

    public boolean updateNode(Number160 id, Node node) {
        OverlayNetwork overlay = overlays.get(id);
        if (overlay == null) return false;
        overlay.getRequestOverlay().updateNode(node);
        return true;
    }

    public void updateAllOverlaysWithNode(Node node) {
        for (Map.Entry<Number160, OverlayNetwork> entry : overlays.entrySet()) {
            entry.getValue().getRequestOverlay().updateNode(node);
        }
    }

    public Address getAddress() {
        return address;
    }

    public SuperNodeServerHandler getServerHandler() {
        return this.serverHandler;
    }

    // only use until connection bug is fixed
    @Deprecated
    public void warmupConnection(String address) {
        Node n = new Node(Number160.createHash(statLogger, address));
        Peer p = new Peer();
        n.setPeer(p);
        Address a = new Address(address, 65487, true);
        p.setAddress(a);

        getConnection(n);
    }

    public StatLogger getStatLogger() {
        return statLogger;
    }
}

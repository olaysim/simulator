package dk.orda.overlaynetwork.overlay.dht;

import com.google.protobuf.ByteString;
import dk.orda.overlaynetwork.net.Address;
import dk.orda.overlaynetwork.net.rpc.MessageType;
import dk.orda.overlaynetwork.net.rpc.SuperNode;
import dk.orda.overlaynetwork.net.rpc.client.MapOrNodesFuture;
import dk.orda.overlaynetwork.net.rpc.client.ValueOrNodesFuture;
import dk.orda.overlaynetwork.net.rpc.protobuf.RpcMessages;
import dk.orda.overlaynetwork.overlay.RequestOverlay;
import dk.orda.overlaynetwork.overlay.peer.Peer;
import dk.orda.overlaynetwork.statistics.DhtStates;
import dk.orda.overlaynetwork.statistics.RoutingStatistic;
import dk.orda.overlaynetwork.statistics.StatLogger;
import dk.orda.overlaynetwork.statistics.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("Duplicates")
public class RoutingProtocol {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private RequestOverlay overlay;
    private SuperNode sn;
    private StatLogger statLogger;

    public RoutingProtocol(SuperNode sn) {
        this.sn = sn;
        this.statLogger = sn.getStatLogger();
    }

    public void setNetworkOverlay(RequestOverlay overlay) {
        this.overlay = overlay;
    }


    public Node iterativeFindNodeLocal(Node targetNode, DistributedHashTableImpl dht) {
        RoutingStatistic stats = new RoutingStatistic();
        long timeStart = System.nanoTime();

        Set<Node> shortlist = new TreeSet<>(DistributedHashTableImpl.getXORNodeComparator(dht.getNodeId()));
        Set<Node> probed = new TreeSet<>();

        int alpha = dht.getConfiguration().getAlpha();
        int k = dht.getConfiguration().getK();
        for (Node n : dht.getOldestNodes(targetNode.getId(), 3)) {
            if (n.getId().equals(targetNode.getId())) {
//                System.out.println("found (already knew it: " + n);
                long time = System.nanoTime() - timeStart;
                stats.setTotalMethodTime(time);
                addRoutingStats(stats, statLogger.getConfig().getTimestamp());
                return n;
            }
            shortlist.add(n);
        }

        Set<Node> toBeAdded = new HashSet<>();
        while (!allProbed(shortlist, probed) && probed.size() < k) {
            stats.increaseIterationCount();
            SortedSet<Node> result = null;
            int count = 0;
            List<String> nodesConnectedTo = new ArrayList<>(alpha);
            synchronized (shortlist) {
                for (Node node : shortlist) {
                    if (!probed.contains(node) && count < alpha) {
                        nodesConnectedTo.add(node.getId().toString());
                        stats.increaseConnectedCount();
                        try {
                            result = sn.sendMessageLocal(node, targetNode, overlay.getSelf());
                            probed.add(node);
                            count++;

                            stats.increaseAnsweredCount();
                            if (result != null) {
                                for (Node n : result) {
                                    // do not add self to shortlist nor dht
                                    if (!node.getId().equals(overlay.getSelf().getId())) {
                                        overlay.updateNode(node);
                                    }

                                    if (node.getId().equals(targetNode.getId())) {
//                                        System.out.println("found: " + n);
                                        long time = System.nanoTime() - timeStart;
                                        stats.setTotalMethodTime(time);
                                        addRoutingStats(stats, statLogger.getConfig().getTimestamp(), true);
                                        return n;
                                    }
                                }
                                toBeAdded.addAll(result);
                            } else {
                                log.error("DID NOT GET A RESULT BACK FROM QUERY");
                            }
                        } catch (Exception e) {
                            log.error("EXCEPTION IN QUERY");
                        }
                    }
                }
            }

            synchronized (shortlist) {
                shortlist.addAll(toBeAdded);
                while (shortlist.size() >= k) {
                    Node last = ((NavigableSet<Node>) shortlist).last();
                    shortlist.remove(last);
                }
                toBeAdded.clear();
            }
        }
        long time = System.nanoTime() - timeStart;
        stats.setTotalMethodTime(time);
        addRoutingStats(stats, statLogger.getConfig().getTimestamp(), false);
        return null;
    }






    public Node iterativeFindNode(Number160 targetId, DistributedHashTableImpl dht) {
//        System.out.println("iterative find goal: " + id);
        DhtStates dhtStates = new DhtStates(statLogger);
        DhtStates dhtConnections = new DhtStates(statLogger);
        DhtStates dhtReplies = new DhtStates(statLogger);
        RoutingStatistic stats = new RoutingStatistic();
        long timeStart = System.nanoTime();

        Set<Node> shortlist = new TreeSet<>(DistributedHashTableImpl.getXORNodeComparator(dht.getNodeId()));
        Set<Node> probed = new TreeSet<>();
        dhtStates.add(dht.getFormattedKBuckets());

        int alpha = dht.getConfiguration().getAlpha();
        int k = dht.getConfiguration().getK();
        for (Node n : dht.getOldestNodes(targetId, 3)) {
//            if (n.getId().equals(id)) continue; // don't query the node, if we already know it... (yeah, this method is also used for other purposes than finding the node)
            if (n.getId().equals(targetId)) { // I think it should be returned if it's already known?
                System.out.println("found (already knew it: " + n);
                long time = System.nanoTime() - timeStart;
                stats.setTotalMethodTime(time);
                addRoutingStats(stats, statLogger.getConfig().getTimestamp());
                dhtStates.addToStatLogger(statLogger.getConfig());
                return n;
            }
            shortlist.add(n);
        }
        if (shortlist.size() == 0) {
            int i = 0;
        }

        while (!allProbed(shortlist, probed) && probed.size() < k) {

            dhtStates.add(dht.getFormattedKBuckets());
            stats.increaseIterationCount();

//            if (probed.size() % 1000 == 0) {
//                System.out.println("- " + probed.size());
//            }

            List<ValueOrNodesFuture> futures = new ArrayList<>();
            int count = 0;
            Map<String, String> corrNode = new HashMap<>();
            synchronized (shortlist) {
                List<String> nodesConnectedTo = new ArrayList<>(alpha);
                for (Node node : shortlist) {
                    if (!probed.contains(node) && count < alpha) {
                        nodesConnectedTo.add(node.getId().toString());
                        UUID uuid = UUID.randomUUID();
                        corrNode.put(uuid.toString(), node.getId().toString());
                        RpcMessages.Header header = RpcMessages.Header.newBuilder()
                            .setCorrelationId(uuid.toString())
                            .setTargetId(targetId.toString())
                            .setSenderId(overlay.getSelf().getId().toString())
                            .setSender(getRpcPeer(overlay.getSelf()))
                            .setQueryId(node.getId().toString())
                            .setNetworkType(0)
                            .setNetworkId("0")
                            .build();
                        RpcMessages.FindNode findNode = RpcMessages.FindNode.newBuilder()
                            .setHeader(header)
                            .build();
                        stats.increaseConnectedCount();
                        ValueOrNodesFuture f = sn.sendMessage(node, findNode, MessageType.FIND_NODE, uuid);
                        if (f != null) {
                            futures.add(f);
                        }
                        probed.add(node);
                        count++;
                    }
                }

                Map<String, List<List<String>>> dhconns = new HashMap<>();
                dhconns.put(dht.getNodeId().toString(), Collections.singletonList(nodesConnectedTo));
                dhtConnections.add(dhconns);
            }

            int xused = 0;
            Map<String, List<List<String>>> repliesMap = new HashMap<>();
            for (ValueOrNodesFuture f : futures) {
                try {
                    SortedSet<Node> nodes = new TreeSet<>();
                    RpcMessages.ValueOrNodes r = f.get(4000, TimeUnit.MILLISECONDS);
                    String requestingNode = corrNode.get(r.getHeader().getCorrelationId());
                    stats.increaseAnsweredCount();
                    xused++;
                    for (RpcMessages.Peer p : r.getPeersList()) {
                        Node node = new Node(new Number160(statLogger, p.getId()), p.getIdStr());
                        Peer peer = new Peer();
                        peer.setAddress(new Address(p.getAddress(), p.getPort()));
                        node.setPeer(peer);
                        // do not add self to shortlist nor dht
                        if (!node.getId().equals(overlay.getSelf().getId())) {
                            nodes.add(node);
                            overlay.updateNode(node);
                        }
                    }
                    List<String> repliesList = new ArrayList<>();
                    for (Node n : nodes) {
                        repliesList.add(n.getId().toString());
                    }
                    repliesMap.put(requestingNode, Arrays.asList(repliesList, Collections.emptyList()));

                    for (Node n : nodes) {
                        if (n.getId().equals(targetId)) {
                            System.out.println("found: " + n);
                            long time = System.nanoTime() - timeStart;
                            stats.setTotalMethodTime(time);
                            stats.setNodesSkipped(futures.size() - xused);
                            addRoutingStats(stats, statLogger.getConfig().getTimestamp(), true);
                            dhtStates.addToStatLogger(statLogger.getConfig());
                            dhtConnections.addToStatLogger(statLogger.getConfig(), "dhtconnections");
                            dhtReplies.add(repliesMap);
                            dhtReplies.addToStatLogger(statLogger.getConfig(), "dhtreplies");
                            return n;
                        }
                    }
                    synchronized (shortlist) {
                        shortlist.addAll(nodes);
                        while (shortlist.size() >= k) {
                            Node last = ((NavigableSet<Node>)shortlist).last();
                            shortlist.remove(last);
                        }
                    }
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    e.printStackTrace();
                }
                finally {
                    f.markDone();
                }
                dhtReplies.add(repliesMap);
            }
        }
        long time = System.nanoTime() - timeStart;
        stats.setTotalMethodTime(time);
        addRoutingStats(stats, statLogger.getConfig().getTimestamp(), false);
        dhtStates.addToStatLogger(statLogger.getConfig());
        dhtConnections.addToStatLogger(statLogger.getConfig(), "dhtconnections");
        dhtReplies.addToStatLogger(statLogger.getConfig(), "dhtreplies");
        return null;
    }

    private boolean allProbed(final Set<Node> shortlist, final Set<Node> probed) {
        if (shortlist.size() != probed.size()) return false;
        if (shortlist.size() <= 0) return true;
        synchronized (shortlist) {
            for (Node n : shortlist) {
                if (!probed.contains(n)) return false;
            }
        }
        return true;
    }

    private RpcMessages.Peer getRpcPeer(Node node) {
        RpcMessages.Peer.Builder builder = RpcMessages.Peer.newBuilder()
            .setId(node.getId().toString());
        if (node.getPeer() != null) {
            builder
                .setAddress(node.getPeer().getAddress().getAddress())
                .setPort(node.getPeer().getAddress().getPort().getPort());
        }
        return builder.build();
    }

    public byte[] iterativeFindValue(Number160 key, DistributedHashTableImpl dht) {
//        System.out.println("iterative find value goal: " + key);
        DhtStates dhtStates = new DhtStates(statLogger);
        RoutingStatistic stats = new RoutingStatistic();
        long timeStart = System.nanoTime();

        Set<Node> shortlist = new TreeSet<>(DistributedHashTableImpl.getXORNodeComparator(dht.getNodeId()));
        Set<Node> probed = new TreeSet<>();
        dhtStates.add(dht.getFormattedKBuckets());

        int alpha = dht.getConfiguration().getAlpha();
        int k = dht.getConfiguration().getK();
//        for (Node n : dht.getOldestNodes(key, 4)) {
//            if (n.getId().equals(key)) continue; // don't query the node, if we already know it... (yeah, this method is also used for other purposes than finding the node)
//            shortlist.add(n);
//        }
        shortlist.addAll(dht.getOldestNodes(key, 3));

        while (!allProbed(shortlist, probed) && probed.size() < k) {

            dhtStates.add(dht.getFormattedKBuckets());
            stats.increaseIterationCount();

            List<ValueOrNodesFuture> futures = new ArrayList<>();
            int count = 0;
            synchronized (shortlist) {
                for (Node node : shortlist) {
                    if (!probed.contains(node) && count <= alpha) {
                        UUID uuid = UUID.randomUUID();
                        RpcMessages.Header header = RpcMessages.Header.newBuilder()
                            .setCorrelationId(uuid.toString())
                            .setSenderId(overlay.getSelf().getId().toString())
                            .setSender(getRpcPeer(overlay.getSelf()))
                            .setQueryId(node.getId().toString())
                            .setNetworkType(0)
                            .setNetworkId("0")
                            .build();
                        RpcMessages.FindValue findValue = RpcMessages.FindValue.newBuilder()
                            .setHeader(header)
                            .setKey(key.toString())
                            .build();
                        stats.increaseConnectedCount();
                        ValueOrNodesFuture f = sn.sendMessage(node, findValue, MessageType.FIND_VALUE, uuid);
                        if (f != null) {
                            futures.add(f);
                        }
                        probed.add(node);
                        count++;
                    }
                }
            }

            int xused = 0;
            for (ValueOrNodesFuture f : futures) {
                try {
                    SortedSet<Node> nodes = new TreeSet<>();
                    RpcMessages.ValueOrNodes r = f.get(4000, TimeUnit.MILLISECONDS);
                    stats.increaseAnsweredCount();
                    xused++;
                    ByteString bs = r.getValue();
                    if (bs != null && !bs.isEmpty()) { // TODO: use isEmpty??? are you sure?, the value might "just" be empty? or is that not allowed?
                        long time = System.nanoTime() - timeStart;
                        stats.setTotalMethodTime(time);
                        stats.setNodesSkipped(futures.size() - xused);
                        addRoutingStats(stats, statLogger.getConfig().getTimestamp());
                        dhtStates.addToStatLogger(statLogger.getConfig());
                        return bs.toByteArray();
                    }
                    else {
                        synchronized (shortlist) {
                            for (RpcMessages.Peer p : r.getPeersList()) {
                                Node node = new Node(new Number160(statLogger, p.getId()), p.getIdStr());
                                Peer peer = new Peer();
                                peer.setAddress(new Address(p.getAddress(), p.getPort()));
                                node.setPeer(peer);
                                // do not add self to shortlist nor dht
                                if (!node.getId().equals(overlay.getSelf().getId())) {
                                    nodes.add(node);
                                    overlay.updateNode(node);
                                    shortlist.add(node);
                                }

                                while (shortlist.size() >= k) {
                                    Node last = ((NavigableSet<Node>) shortlist).last();
                                    shortlist.remove(last);
                                }
                            }
                        }
                    }
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    e.printStackTrace();
                }
                finally {
                    f.markDone();
                }
            }
        }
        long time = System.nanoTime() - timeStart;
        stats.setTotalMethodTime(time);
        addRoutingStats(stats, statLogger.getConfig().getTimestamp());
        dhtStates.addToStatLogger(statLogger.getConfig());
        return null;
    }

    public void iterativeStore(Number160 key, byte[] value, DistributedHashTableImpl dht, int nodestorecount) {
//        System.out.println("iterative store goal: " + key);
        DhtStates dhtStates = new DhtStates(statLogger);
        RoutingStatistic stats = new RoutingStatistic();
        long timeStart = System.nanoTime();

        // start with a modified iterative find
        Set<Node> shortlist = new TreeSet<>(DistributedHashTableImpl.getXORNodeComparator(dht.getNodeId()));
        Set<Node> probed = new TreeSet<>();
        dhtStates.add(dht.getFormattedKBuckets());

        int alpha = dht.getConfiguration().getAlpha();
        int k = dht.getConfiguration().getK();
        shortlist.addAll(dht.getOldestNodes(key, 3)); // TODO: <-- difference from findnode, but should be possible to reuse code

        while (!allProbed(shortlist, probed) && probed.size() < k) {

            dhtStates.add(dht.getFormattedKBuckets());
            stats.increaseIterationCount();

            List<ValueOrNodesFuture> futures = new ArrayList<>();
            int count = 0;
            synchronized (shortlist) {
                for (Node node : shortlist) {
                    if (!probed.contains(node) && count <= alpha) {
                        UUID uuid = UUID.randomUUID();
                        RpcMessages.Header header = RpcMessages.Header.newBuilder()
                            .setCorrelationId(uuid.toString())
                            .setTargetId(key.toString())
                            .setSenderId(overlay.getSelf().getId().toString())
                            .setSender(getRpcPeer(overlay.getSelf()))
                            .setQueryId(node.getId().toString())
                            .setNetworkType(0)
                            .setNetworkId("0")
                            .build();
                        RpcMessages.FindNode findNode = RpcMessages.FindNode.newBuilder()
                            .setHeader(header)
                            .build();
                        stats.increaseConnectedCount();
                        ValueOrNodesFuture f = sn.sendMessage(node, findNode, MessageType.FIND_NODE, uuid);
                        if (f != null) {
                            futures.add(f);
                        }
                        probed.add(node);
                        count++;
                    }
                }
            }

            for (ValueOrNodesFuture f : futures) {
                try {
                    SortedSet<Node> nodes = new TreeSet<>();
                    RpcMessages.ValueOrNodes r = f.get(4000, TimeUnit.MILLISECONDS);
                    stats.increaseAnsweredCount();
                    for (RpcMessages.Peer p : r.getPeersList()) {
                        Node node = new Node(new Number160(statLogger, p.getId()), p.getIdStr());
                        Peer peer = new Peer();
                        peer.setAddress(new Address(p.getAddress(), p.getPort()));
                        node.setPeer(peer);
                        // do not add self to shortlist nor dht
                        if (!node.getId().equals(overlay.getSelf().getId())) {
                            nodes.add(node);
                            overlay.updateNode(node);
                        }
                    }

                    synchronized (shortlist) {
                        shortlist.addAll(nodes);
                        while (shortlist.size() >= k) {
                            Node last = ((NavigableSet<Node>)shortlist).last();
                            shortlist.remove(last);
                        }
                    }
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    e.printStackTrace();
                }
                finally {
                    f.markDone();
                }
            }
        }

        // second part is an iterative store
        int nodestorecountCounter = 0; // count can't be larger than k
        for (Node n : shortlist) {
            if (nodestorecount <= 0 || (nodestorecount > 0 && nodestorecountCounter < nodestorecount)) {
                stats.increaseIterationCount();
                UUID uuid = UUID.randomUUID();
                RpcMessages.Header header = RpcMessages.Header.newBuilder()
                    .setCorrelationId(uuid.toString())
                    .setSenderId(overlay.getSelf().getId().toString())
                    .setSender(getRpcPeer(overlay.getSelf()))
                    .setQueryId(n.getId().toString())
                    .setNetworkType(0)
                    .setNetworkId("0")
                    .build();
                RpcMessages.StoreValue storeValue = RpcMessages.StoreValue.newBuilder()
                    .setHeader(header)
                    .setKey(key.toString())
                    .setValue(ByteString.copyFrom(value))
                    .build();
                stats.increaseConnectedCount();
                ValueOrNodesFuture f = sn.sendMessage(n, storeValue, MessageType.STORE_VALUE, uuid);

                if (f != null) {
                    try {
                        RpcMessages.ValueOrNodes r = f.get(4000, TimeUnit.MILLISECONDS);
                        stats.increaseAnsweredCount();
                        ByteString bs = r.getValue();
                        if (bs != null && !bs.isEmpty()) {
                            String result = bs.toStringUtf8();
                            boolean success = result.equals("1");
                            if (success) {
                                nodestorecountCounter++;
                            }
//                        log.debug("Store key: " + key.toString() + ", success: " + result.equals("1"));
                        }
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        log.debug("Failed to get reply from: " + n.getId().toString() + " - " + n.getPeer().getAddress().getAddress() + ":" + n.getPeer().getAddress().getPort());
                    } finally {
                        f.markDone();
                    }
                }
            }
        }

        long time = System.nanoTime() - timeStart;
        stats.setTotalMethodTime(time);
        addRoutingStats(stats, statLogger.getConfig().getTimestamp());
        dhtStates.addToStatLogger(statLogger.getConfig());
    }





    public Map<String,byte[]> iterativeFindMap(Number160 key, DistributedHashTableImpl dht) {
//        System.out.println("iterative find map goal: " + key);
        DhtStates dhtStates = new DhtStates(statLogger);
        RoutingStatistic stats = new RoutingStatistic();
        long timeStart = System.nanoTime();

        Set<Node> shortlist = new TreeSet<>(DistributedHashTableImpl.getXORNodeComparator(dht.getNodeId()));
        Set<Node> probed = new TreeSet<>();
        dhtStates.add(dht.getFormattedKBuckets());

        int alpha = dht.getConfiguration().getAlpha();
        int k = dht.getConfiguration().getK();

        shortlist.addAll(dht.getOldestNodes(key, 3));

        while (!allProbed(shortlist, probed) && probed.size() < k) {

            dhtStates.add(dht.getFormattedKBuckets());
            stats.increaseIterationCount();

            List<MapOrNodesFuture> futures = new ArrayList<>();
            int count = 0;
            synchronized (shortlist) {
                for (Node node : shortlist) {
                    if (!probed.contains(node) && count <= alpha) {
                        UUID uuid = UUID.randomUUID();
                        RpcMessages.Header header = RpcMessages.Header.newBuilder()
                            .setCorrelationId(uuid.toString())
                            .setSenderId(overlay.getSelf().getId().toString())
                            .setSender(getRpcPeer(overlay.getSelf()))
                            .setQueryId(node.getId().toString())
                            .setNetworkType(0)
                            .setNetworkId("0")
                            .build();
                        RpcMessages.FindMap findValue = RpcMessages.FindMap.newBuilder()
                            .setHeader(header)
                            .setKey(key.toString())
                            .build();
                        stats.increaseConnectedCount();
                        MapOrNodesFuture f = sn.sendMessageForMap(node, findValue, MessageType.FIND_MAP, uuid);
                        if (f != null) {
                            futures.add(f);
                        }
                        probed.add(node);
                        count++;
                    }
                }
            }

            int xused = 0;
            for (MapOrNodesFuture f : futures) {
                try {
                    SortedSet<Node> nodes = new TreeSet<>();
                    RpcMessages.MapOrNodes r = f.get(4000, TimeUnit.MILLISECONDS);
                    stats.increaseAnsweredCount();
                    xused++;
                    if (r.getType() == 1) {
                        Map<String, ByteString> map = r.getValue();
                        Map<String, byte[]> resultMap = new Hashtable<>(map.size());
                        for (Map.Entry<String, ByteString> entry : map.entrySet()) {
                            resultMap.put(entry.getKey(), entry.getValue().toByteArray());
                        }
                        long time = System.nanoTime() - timeStart;
                        stats.setTotalMethodTime(time);
                        stats.setNodesSkipped(futures.size() - xused);
                        addRoutingStats(stats, statLogger.getConfig().getTimestamp());
                        dhtStates.addToStatLogger(statLogger.getConfig());
                        return resultMap;
                    }
                    else {
                        synchronized (shortlist) {
                            for (RpcMessages.Peer p : r.getPeersList()) {
                                Node node = new Node(new Number160(statLogger, p.getId()), p.getIdStr());
                                Peer peer = new Peer();
                                peer.setAddress(new Address(p.getAddress(), p.getPort()));
                                node.setPeer(peer);
                                // do not add self to shortlist nor dht
                                if (!node.getId().equals(overlay.getSelf().getId())) {
                                    nodes.add(node);
                                    overlay.updateNode(node);
                                    shortlist.add(node);
                                }

                                while (shortlist.size() >= k) {
                                    Node last = ((NavigableSet<Node>) shortlist).last();
                                    shortlist.remove(last);
                                }
                            }
                        }
                    }
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    e.printStackTrace();
                }
                finally {
                    f.markDone();
                }
            }
        }
        long time = System.nanoTime() - timeStart;
        stats.setTotalMethodTime(time);
        addRoutingStats(stats, statLogger.getConfig().getTimestamp());
        dhtStates.addToStatLogger(statLogger.getConfig());
        return null;
    }



    public void iterativeStoreMapValue(Number160 key, String index, byte[] value, DistributedHashTableImpl dht) {
//        System.out.println("iterative store map value goal: " + key);
        DhtStates dhtStates = new DhtStates(statLogger);
        RoutingStatistic stats = new RoutingStatistic();
        long timeStart = System.nanoTime();

        // start with a modified iterative find
        Set<Node> shortlist = new TreeSet<>(DistributedHashTableImpl.getXORNodeComparator(dht.getNodeId()));
        Set<Node> probed = new TreeSet<>();
        dhtStates.add(dht.getFormattedKBuckets());

        int alpha = dht.getConfiguration().getAlpha();
        int k = dht.getConfiguration().getK();
        shortlist.addAll(dht.getOldestNodes(key, 3)); // TODO: <-- difference from findnode, but should be possible to reuse code

        while (!allProbed(shortlist, probed) && probed.size() < k) {

            dhtStates.add(dht.getFormattedKBuckets());
            stats.increaseIterationCount();

            List<ValueOrNodesFuture> futures = new ArrayList<>();
            int count = 0;
            synchronized (shortlist) {
                for (Node node : shortlist) {
                    if (!probed.contains(node) && count <= alpha) {
                        UUID uuid = UUID.randomUUID();
                        RpcMessages.Header header = RpcMessages.Header.newBuilder()
                            .setCorrelationId(uuid.toString())
                            .setTargetId(key.toString())
                            .setSenderId(overlay.getSelf().getId().toString())
                            .setSender(getRpcPeer(overlay.getSelf()))
                            .setQueryId(node.getId().toString())
                            .setNetworkType(0)
                            .setNetworkId("0")
                            .build();
                        RpcMessages.FindNode findNode = RpcMessages.FindNode.newBuilder()
                            .setHeader(header)
                            .build();
                        stats.increaseConnectedCount();
                        ValueOrNodesFuture f = sn.sendMessage(node, findNode, MessageType.FIND_NODE, uuid);
                        if (f != null) {
                            futures.add(f);
                        }
                        probed.add(node);
                        count++;
                    }
                }
            }

            for (ValueOrNodesFuture f : futures) {
                try {
                    SortedSet<Node> nodes = new TreeSet<>();
                    RpcMessages.ValueOrNodes r = f.get(4000, TimeUnit.MILLISECONDS);
                    stats.increaseAnsweredCount();
                    for (RpcMessages.Peer p : r.getPeersList()) {
                        Node node = new Node(new Number160(statLogger, p.getId()), p.getIdStr());
                        Peer peer = new Peer();
                        peer.setAddress(new Address(p.getAddress(), p.getPort()));
                        node.setPeer(peer);
                        // do not add self to shortlist nor dht
                        if (!node.getId().equals(overlay.getSelf().getId())) {
                            nodes.add(node);
                            overlay.updateNode(node);
                        }
                    }

                    synchronized (shortlist) {
                        shortlist.addAll(nodes);
                        while (shortlist.size() >= k) {
                            Node last = ((NavigableSet<Node>)shortlist).last();
                            shortlist.remove(last);
                        }
                    }
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    e.printStackTrace();
                } catch (Exception exx) {
                    exx.printStackTrace();
                }
                finally {
                    f.markDone();
                }
            }
        }

        // second part is an iterative store
        for (Node n : shortlist) {
            stats.increaseIterationCount();
            try {
                UUID uuid = UUID.randomUUID();
                RpcMessages.Header header = RpcMessages.Header.newBuilder()
                    .setCorrelationId(uuid.toString())
                    .setSenderId(overlay.getSelf().getId().toString())
                    .setSender(getRpcPeer(overlay.getSelf()))
                    .setQueryId(n.getId().toString())
                    .setNetworkType(0)
                    .setNetworkId("0")
                    .build();
                RpcMessages.AddValue addValue = RpcMessages.AddValue.newBuilder()
                    .setHeader(header)
                    .setKey(key.toString())
                    .setIndex(index)
                    .setValue(ByteString.copyFrom(value))
                    .build();
                stats.increaseConnectedCount();
                ValueOrNodesFuture f = sn.sendMessage(n, addValue, MessageType.ADD_VALUE, uuid);

                if (f != null) {
                    RpcMessages.ValueOrNodes r = f.get(4000, TimeUnit.MILLISECONDS);
                    stats.increaseAnsweredCount();
                    ByteString bs = r.getValue();
                    if (bs != null && !bs.isEmpty()) {
                        String result = bs.toStringUtf8();
                        //success = result.equals("1");
//                        log.debug("Store key: " + key.toString() + ", success: " + result.equals("1"));
                    }
                    f.markDone();
                }
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.debug("Failed to get reply from: " + n.getId().toString() + " - " + n.getPeer().getAddress().getAddress() + ":" + n.getPeer().getAddress().getPort());
            }
        }

        long time = System.nanoTime() - timeStart;
        stats.setTotalMethodTime(time);
        addRoutingStats(stats, statLogger.getConfig().getTimestamp());
        dhtStates.addToStatLogger(statLogger.getConfig());
    }


    private void addRoutingStats(RoutingStatistic stats, long timestmp) {
        addRoutingStats(stats, timestmp, true);
    }
    private void addRoutingStats(RoutingStatistic stats, long timestmp, boolean success) {
        List<Value> list = Arrays.asList(
            new Value<>("successrate", timestmp, statLogger.getConfig(), null, Collections.singletonList(stats.getSuccessRate())),
            new Value<>("nodesconnected", timestmp, statLogger.getConfig(), null, Collections.singletonList(stats.getNodesConnected())),
            new Value<>("nodesanswered", timestmp, statLogger.getConfig(), null, Collections.singletonList(stats.getNodesAnswered())),
            new Value<>("nodesskipped", timestmp, statLogger.getConfig(), null, Collections.singletonList(stats.getNodesSkipped())),
            new Value<>("toalmethodtime", timestmp, statLogger.getConfig(), null, Collections.singletonList(stats.getTotalMethodTime())),
            new Value<>("iterationcount", timestmp, statLogger.getConfig(), null, Collections.singletonList(stats.getIterationCount())),
            new Value<>("success", timestmp, statLogger.getConfig(), null, Collections.singletonList(success ? 1.0 : 0.0))
        );
        this.statLogger.addValues("routing", list);
//        log.info("Routing stats: " + stats);
    }

}

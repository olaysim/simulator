package dk.orda.overlaynetwork.net.rpc;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import dk.orda.overlaynetwork.net.rpc.protobuf.RpcMessages;
import dk.orda.overlaynetwork.net.tcp.TcpClient;
import dk.orda.overlaynetwork.overlay.OverlayNetwork;
import dk.orda.overlaynetwork.overlay.RequestOverlay;
import dk.orda.overlaynetwork.overlay.dht.Node;
import dk.orda.overlaynetwork.overlay.dht.Number160;
import dk.orda.overlaynetwork.overlaygeo.LatLon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.Future;

@SuppressWarnings("Duplicates")
public class SuperNodeServerHandler {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private SuperNode sn;
    private Map<Number160, OverlayNetwork> overlays;
    private TcpClient client;

    public SuperNodeServerHandler(SuperNode sn, Map<Number160, OverlayNetwork> overlays, TcpClient client) {
        this.sn = sn;
        this.overlays = overlays;
        this.client = client;
    }

    @SuppressWarnings("unchecked")
    public <T extends MessageLite, V extends MessageLite> V handleGenericRequest(Node queryNode, T msg, MessageType type) throws Exception {
        switch (type) {
            case FIND_NODE:
                return (V) handleFindNode(queryNode, (RpcMessages.FindNode) msg);
            case FIND_VALUE:
                return (V) handleFindValue(queryNode, (RpcMessages.FindValue) msg);
            case STORE_VALUE:
                break;
            case ADD_VALUE:
                break;
            case FIND_MAP:
                break;
        }
        return null;
    }

    public SortedSet<Node> handleGenericRequestLocal(Node queryNode, Node targetNode, Node self, MessageType type) throws Exception {
        switch (type) {
            case FIND_NODE:
                return handleFindNodeLocal(queryNode, targetNode, self);
        }
        return null;
    }

    public SortedSet<Node> handleFindNodeLocal(Node queryNode, Node targetNode, Node self) throws Exception {
//        Number160 targetId = targetNode.getId();
        Number160 queryId  = queryNode.getId();

        OverlayNetwork on = sn.getOverlayNetwork(queryId);
        if (on == null) {
            log.error("Unable to get overlaynetwork from supernode");
            throw new IllegalArgumentException("Unable to find requested overlay network on this node");
        }
        RequestOverlay reqo = on.getRequestOverlay();
        SortedSet<Node> nodes = reqo.getNodes(targetNode);

        // add random, by adding self to querynode
        reqo.updateRandom(self);

        return nodes;
    }

    public RpcMessages.ValueOrNodes handleFindNode(Node queryNode, RpcMessages.FindNode msg) throws Exception {
        Number160 targetId = new Number160(sn.getStatLogger(), msg.getHeader().getTargetId());
        Number160 queryId  = (queryNode != null) ? queryNode.getId() : new Number160(sn.getStatLogger(), msg.getHeader().getQueryId());

        OverlayNetwork on = sn.getOverlayNetwork(queryId);
        if (on == null) {
            log.error("Unable to get overlaynetwork from supernode");
            throw new IllegalArgumentException("Unable to find requested overlay network on this node");
        }
        RequestOverlay reqo = on.getRequestOverlay();

        SortedSet<Node> nodes = reqo.getNodes(targetId, msg.getExt().getDictMap());

        List<RpcMessages.Peer> peers = new ArrayList<>();
        for (Node node : nodes) {
            LatLon latLon = node.tryGetLatLon();
            RpcMessages.Ext.Builder ext = RpcMessages.Ext.newBuilder();
            if (latLon != null) {
                ext
                    .putDict("x", String.valueOf(latLon.getLat()))
                    .putDict("y", String.valueOf(latLon.getLon()));
            }
            RpcMessages.Peer peer = RpcMessages.Peer.newBuilder()
                .setId(node.getId().toString())
                .setIdStr(node.getIdStr())
                .setAddress(node.getPeer().getAddress().getAddress())
                .setPort(node.getPeer().getAddress().getPort().getPort())
                .setExt(ext.build())
                .build();
            peers.add(peer);
        }
        RpcMessages.ValueOrNodes reply = RpcMessages.ValueOrNodes.newBuilder()
            .setHeader(RpcMessages.Header.newBuilder().setCorrelationId(msg.getHeader().getCorrelationId()).build())
            .addAllPeers(peers)
            .build();

        // also add the node that contacted this node to this nodes routing table
//        OverlayNetwork sOn = sn.getOverlayNetwork(msg.getHeader().getSenderId());
        OverlayNetwork sOn = sn.getOverlayNetwork(new Number160(null, msg.getHeader().getSenderId()));
        if (sOn != null) {
            Node self = sOn.getRequestOverlay().getSelf();
            reqo.updateRandom(self.clone());
        }

        return reply;
    }

    public RpcMessages.ValueOrNodes handleFindValue(Node queryNode, RpcMessages.FindValue msg) throws Exception {
//        Number160 targetId = new Number160(sn.getStatLogger(), msg.getHeader().getTargetId());
        Number160 queryId = (queryNode != null) ? queryNode.getId() : new Number160(sn.getStatLogger(), msg.getHeader().getQueryId());
        Number160 key = new Number160(sn.getStatLogger(), msg.getKey());


        RequestOverlay reqo = sn.getOverlayNetwork(queryId).getRequestOverlay();
        if (reqo == null) {
            log.error("Unable to get overlaynetwork from supernode");
            throw new IllegalArgumentException("Unable to find requested overlay network on this node");
        }

        RpcMessages.ValueOrNodes reply;

//        int size = 0;
        if (reqo.hasValue(key)) {
            byte[] value = reqo.getValue(key); // TODO: multithreaded race condition here
//            size = value.length;
            reply = RpcMessages.ValueOrNodes.newBuilder()
                .setHeader(RpcMessages.Header.newBuilder().setCorrelationId(msg.getHeader().getCorrelationId()).build())
                .setValue(ByteString.copyFrom(value))
                .build();
        }
        else {
            SortedSet<Node> nodes = reqo.getNodes(key, msg.getExt().getDictMap());
            List<RpcMessages.Peer> peers = new ArrayList<>();

            for (Node node : nodes) {
                LatLon latLon = node.tryGetLatLon();
                RpcMessages.Ext.Builder ext = RpcMessages.Ext.newBuilder();
                if (latLon != null) {
                    ext
                        .putDict("x", String.valueOf(latLon.getLat()))
                        .putDict("y", String.valueOf(latLon.getLon()));
                }
                RpcMessages.Peer peer = RpcMessages.Peer.newBuilder()
                    .setId(node.getId().toString())
                    .setIdStr(node.getIdStr())
                    .setAddress(node.getPeer().getAddress().getAddress())
                    .setPort(node.getPeer().getAddress().getPort().getPort())
                    .setExt(ext.build())
                    .build();
                peers.add(peer);
            }
            reply = RpcMessages.ValueOrNodes.newBuilder()
                .setHeader(RpcMessages.Header.newBuilder().setCorrelationId(msg.getHeader().getCorrelationId()).build())
                .addAllPeers(peers)
                .build();
        }

        return reply;
    }
}

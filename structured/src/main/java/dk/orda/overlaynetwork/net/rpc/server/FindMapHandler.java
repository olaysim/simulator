package dk.orda.overlaynetwork.net.rpc.server;

import com.google.protobuf.ByteString;
import dk.orda.overlaynetwork.net.rpc.MessageType;
import dk.orda.overlaynetwork.net.rpc.SuperNode;
import dk.orda.overlaynetwork.net.rpc.protobuf.ProtoMessageWrapper;
import dk.orda.overlaynetwork.net.rpc.protobuf.RpcMessages;
import dk.orda.overlaynetwork.overlay.RequestOverlay;
import dk.orda.overlaynetwork.overlay.dht.DistributedHashTableImpl;
import dk.orda.overlaynetwork.overlay.dht.Node;
import dk.orda.overlaynetwork.overlay.dht.Number160;
import dk.orda.overlaynetwork.statistics.DhtStates;
import dk.orda.overlaynetwork.statistics.StatLogger;
import dk.orda.overlaynetwork.statistics.Value;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@ChannelHandler.Sharable
public class FindMapHandler extends SimpleChannelInboundHandler<RpcMessages.FindMap> {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final SuperNode sn;
    private StatLogger statLogger;

    public FindMapHandler(SuperNode reference) {
        this.sn = reference;
        this.statLogger = reference.getStatLogger();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessages.FindMap msg) throws Exception {
        long timestamp = System.currentTimeMillis();
        long timeStart = System.nanoTime();
        DhtStates dhtStates = new DhtStates(statLogger);

        Number160 senderId = new Number160(statLogger, msg.getHeader().getSenderId());
        Number160 queryId  = new Number160(statLogger, msg.getHeader().getQueryId());
        Number160 key = new Number160(statLogger, msg.getKey());

        RpcMessages.MapOrNodes reply;

        RequestOverlay reqo = (RequestOverlay) sn.getOverlayNetwork(queryId);
        if (reqo == null) {
            log.error("Unable to get overlaynetwork from supernode");
            throw new IllegalArgumentException("Unable to find requested overlay network on this node");
        }

        if (reqo.mapsHasKey(key)) {
            Map<String, byte[]> value = reqo.getMap(key);
            Map<String, ByteString> protoValue = new Hashtable<>();
            for (Map.Entry<String, byte[]> entry : value.entrySet()) {
                protoValue.put(entry.getKey(), ByteString.copyFrom(entry.getValue()));
            }
            reply = RpcMessages.MapOrNodes.newBuilder()
                .setHeader(RpcMessages.Header.newBuilder().setCorrelationId(msg.getHeader().getCorrelationId()).build())
                .putAllValue(protoValue)
                .setType(1)
                .build();
        }
        else {
            SortedSet<Node> nodes = reqo.getNodes(key, msg.getExt().getDictMap());
            List<RpcMessages.Peer> peers = new ArrayList<>();

            for (Node node : nodes) {
                RpcMessages.Peer peer = RpcMessages.Peer.newBuilder()
                    .setId(node.getId().toString())
                    .setAddress(node.getPeer().getAddress().getAddress())
                    .setPort(node.getPeer().getAddress().getPort().getPort())
                    .build();
                peers.add(peer);
            }
            reply = RpcMessages.MapOrNodes.newBuilder()
                .setHeader(RpcMessages.Header.newBuilder().setCorrelationId(msg.getHeader().getCorrelationId()).build())
                .addAllPeers(peers)
                .setType(2)
                .build();
        }

        ProtoMessageWrapper wrapper = new ProtoMessageWrapper(MessageType.REPLY_MAP_OR_NODES, reply);
        long total = System.nanoTime() - timeStart;
        int mapsize = 0;
        if (reply.getType() == 1) {
            mapsize = reply.getValueMap().size();
        }
        addToStats(timestamp, total, mapsize);
        dhtStates.add(((DistributedHashTableImpl)reqo.getDHT()).getFormattedKBuckets());
        dhtStates.addToStatLogger(statLogger.getConfig(), "serverhandler_dhtstates");
        ctx.writeAndFlush(wrapper);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Error in receiving Find Map request", cause);
        ctx.close();
    }

    @SuppressWarnings("Duplicates")
    private void addToStats(long timestamp, long total, int mapsize) {
        List<Value> list = Arrays.asList(
            new Value<>("mapsize", timestamp, statLogger.getConfig(), null, mapsize),
            new Value<>("toalmethodtime", timestamp, statLogger.getConfig(), null, total)
        );
        this.statLogger.addValues("findmaphandler", list);
    }
}
